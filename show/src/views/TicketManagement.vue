<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div>
          <h3>票务管理</h3>
          <p>管理员可以按发车时间发售或停止发售指定车次。</p>
        </div>
      </div>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="发售车票" name="release">
        <el-form ref="releaseFormRef" :model="releaseForm" :rules="formRules" label-position="top" class="ticket-form">
          <el-form-item label="车次" prop="trainId">
            <el-select
              v-model="releaseForm.trainId"
              placeholder="请选择车次"
              filterable
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="train in trains"
                :key="train.trainId"
                :label="formatTrainLabel(train)"
                :value="train.trainId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="发车日期" prop="departureDate">
            <el-date-picker
              v-model="releaseForm.departureDate"
              type="date"
              placeholder="选择发车日期"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="发车时间" prop="departureTime">
            <el-time-picker
              v-model="releaseForm.departureTimeDate"
              format="HH:mm"
              placeholder="选择发车时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="confirmRelease" :loading="loading">发售</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="停止发售" name="expire">
        <el-form ref="expireFormRef" :model="expireForm" :rules="formRules" label-position="top" class="ticket-form">
          <el-form-item label="车次" prop="trainId">
            <el-select
              v-model="expireForm.trainId"
              placeholder="请选择车次"
              filterable
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="train in trains"
                :key="train.trainId"
                :label="formatTrainLabel(train)"
                :value="train.trainId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="发车日期" prop="departureDate">
            <el-date-picker
              v-model="expireForm.departureDate"
              type="date"
              placeholder="选择发车日期"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="发车时间" prop="departureTime">
            <el-time-picker
              v-model="expireForm.departureTimeDate"
              format="HH:mm"
              placeholder="选择发车时间"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" @click="confirmExpire" :loading="loading">停止发售</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="发售记录" name="records">
        <el-button type="primary" @click="loadRecords" :loading="loadingRecords" style="margin-bottom: 16px">
          刷新记录
        </el-button>

        <el-table :data="ticketRecords" stripe v-loading="loadingRecords">
          <el-table-column prop="trainId" label="车次" width="120" />
          <el-table-column prop="departureStation" label="出发站" width="120" />
          <el-table-column prop="arrivalStation" label="到达站" width="120" />
          <el-table-column label="出发时间" width="160">
            <template #default="{ row }">{{ formatDepartureTime(row.departureTime) }}</template>
          </el-table-column>
          <el-table-column prop="seatNum" label="余票" width="100" />
          <el-table-column prop="price" label="票价" width="100" />
        </el-table>

        <el-empty v-if="ticketRecords.length === 0 && !loadingRecords" description="暂无发售记录" />
      </el-tab-pane>
    </el-tabs>

    <!-- 发售确认弹窗 -->
    <el-dialog v-model="releaseConfirmVisible" title="确认发售车票" width="400px" :close-on-click-modal="false">
      <div class="confirm-content">
        <p>确认发售以下车次的车票？</p>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="车次">{{ releaseForm.trainId }}</el-descriptions-item>
          <el-descriptions-item label="发车时间">{{ releaseForm.departureDate }} {{ formatTimeValue(releaseForm.departureTimeDate) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="releaseConfirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleRelease">确认发售</el-button>
      </template>
    </el-dialog>

    <!-- 停止发售确认弹窗 -->
    <el-dialog v-model="expireConfirmVisible" title="确认停止发售" width="400px" :close-on-click-modal="false">
      <div class="confirm-content">
        <div class="confirm-warning">
          <el-icon color="#f97316"><WarningFilled /></el-icon>
          <span>停止发售后用户将无法继续购买该车次车票，请确认操作。</span>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="车次">{{ expireForm.trainId }}</el-descriptions-item>
          <el-descriptions-item label="发车时间">{{ expireForm.departureDate }} {{ formatTimeValue(expireForm.departureTimeDate) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="expireConfirmVisible = false">取消</el-button>
        <el-button type="danger" :loading="loading" @click="handleExpire">确认停止发售</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useStore } from '../store'
import http from '../utils/http'
import { formatStartTime, parseBackendTime, formatDateTime } from '../utils'
import { ElMessage } from 'element-plus'

const store = useStore()

const activeTab = ref('release')
const loading = ref(false)
const loadingRecords = ref(false)
const trains = ref([])
const ticketRecords = ref([])
const releaseFormRef = ref()
const expireFormRef = ref()

const releaseForm = reactive({
  trainId: '',
  departureDate: '',
  departureTimeDate: null
})

const expireForm = reactive({
  trainId: '',
  departureDate: '',
  departureTimeDate: null
})

const formRules = {
  trainId: [{ required: true, message: '请选择车次', trigger: 'change' }],
  departureDate: [{ required: true, message: '请选择发车日期', trigger: 'change' }],
  departureTime: [{ required: true, message: '请选择发车时间', trigger: 'change' }]
}

const releaseConfirmVisible = ref(false)
const expireConfirmVisible = ref(false)

const formatTrainLabel = (train) => {
  const stationText = train.stations?.length ? train.stations.join(' → ') : '暂无站点信息'
  const startTime = formatStartTime(train.startTime)
  return `${train.trainId} | ${startTime} | ${stationText}`
}

const formatTimeValue = (date) => {
  if (!date) return '-'
  const pad = (n) => `${n}`.padStart(2, '0')
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const formatDepartureTime = (value) => {
  const date = parseBackendTime(value)
  return formatDateTime(date) || value || '-'
}

const buildDepartureTime = (form) => {
  if (!form.departureTimeDate) return ''
  const pad = (n) => `${n}`.padStart(2, '0')
  const time = `${pad(form.departureTimeDate.getHours())}:${pad(form.departureTimeDate.getMinutes())}`
  return `${time} ${form.departureDate}`
}

const loadTrains = async () => {
  try {
    const response = await http.get('/api/train/list')
    if (response.data.code === 200) {
      trains.value = response.data.data || []
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载车次失败')
  }
}

const loadRecords = async () => {
  loadingRecords.value = true
  try {
    const response = await http.get('/api/ticket/list')
    if (response.data.code === 200) {
      ticketRecords.value = response.data.data || []
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载记录失败')
  } finally {
    loadingRecords.value = false
  }
}

const confirmRelease = async () => {
  const valid = await releaseFormRef.value?.validate().catch(() => false)
  if (!valid) return
  releaseConfirmVisible.value = true
}

const handleRelease = async () => {
  const departureTime = buildDepartureTime(releaseForm)
  if (!departureTime) {
    ElMessage.warning('请填写完整的发车日期和时间')
    return
  }

  loading.value = true
  try {
    const response = await http.post('/api/ticket/release', {
      trainId: releaseForm.trainId,
      departureTime
    })

    if (response.data.code === 200) {
      ElMessage.success('发售成功')
      releaseConfirmVisible.value = false
      releaseForm.trainId = ''
      releaseForm.departureDate = ''
      releaseForm.departureTimeDate = null
      loadRecords()
    } else {
      ElMessage.error(response.data.message || '发售失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '发售失败')
  } finally {
    loading.value = false
  }
}

const confirmExpire = async () => {
  const valid = await expireFormRef.value?.validate().catch(() => false)
  if (!valid) return
  expireConfirmVisible.value = true
}

const handleExpire = async () => {
  const departureTime = buildDepartureTime(expireForm)
  if (!departureTime) {
    ElMessage.warning('请填写完整的发车日期和时间')
    return
  }

  loading.value = true
  try {
    const response = await http.post('/api/ticket/expire', {
      trainId: expireForm.trainId,
      departureTime
    })

    if (response.data.code === 200) {
      ElMessage.success('停止发售成功')
      expireConfirmVisible.value = false
      expireForm.trainId = ''
      expireForm.departureDate = ''
      expireForm.departureTimeDate = null
      loadRecords()
    } else {
      ElMessage.error(response.data.message || '停止发售失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '停止发售失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTrains()
  loadRecords()
})
</script>

<style scoped>
.panel {
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
}

.panel-header h3 {
  margin: 0;
}

.panel-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.ticket-form {
  max-width: 420px;
}

.confirm-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.confirm-content p {
  color: #334155;
  font-weight: 600;
}

.confirm-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 14px;
}
</style>