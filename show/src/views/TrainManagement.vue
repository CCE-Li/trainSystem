<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div>
          <h3>车次管理</h3>
          <p>管理员可新增车次并按车次 ID 查询详情。</p>
        </div>
      </div>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="新增车次" name="add">
        <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-position="top" class="wide-form">
          <el-form-item label="车次 ID" prop="trainId">
            <el-input v-model="addForm.trainId" placeholder="请输入车次 ID" />
          </el-form-item>
          <el-form-item label="座位数" prop="seatNum">
            <el-input-number v-model="addForm.seatNum" :min="1" style="width: 100%" />
          </el-form-item>
          <el-form-item label="首发时间" prop="startTime">
            <el-time-picker
              v-model="addForm.startTimeDate"
              format="HH:mm"
              placeholder="选择首发时间"
              style="width: 100%"
            />
          </el-form-item>

          <el-divider content-position="left">站点与区间信息</el-divider>

          <div class="station-list">
            <div v-for="(station, index) in addForm.stations" :key="index" class="station-row">
              <div class="station-index">{{ index + 1 }}</div>
              <el-input
                v-model="addForm.stations[index]"
                placeholder="站名"
                class="station-input"
              />
              <div v-if="index > 0" class="segment-info">
                <el-input-number
                  v-model="addForm.durations[index - 1]"
                  :min="1"
                  placeholder="时长(分)"
                  controls-position="right"
                  class="segment-input"
                />
                <el-input-number
                  v-model="addForm.prices[index - 1]"
                  :min="0"
                  placeholder="票价(元)"
                  controls-position="right"
                  class="segment-input"
                />
              </div>
              <el-button
                v-if="addForm.stations.length > 2"
                type="danger"
                circle
                size="small"
                @click="removeStation(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <el-button type="primary" plain @click="addStation" class="add-station-btn">
            <el-icon><Plus /></el-icon> 添加站点
          </el-button>

          <el-form-item>
            <el-button type="primary" @click="handleAdd" :loading="loading">新增车次</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="查询车次" name="query">
        <el-form :model="queryForm" label-position="top" class="narrow-form" @submit.prevent>
          <el-form-item label="车次 ID">
            <el-input
              v-model="queryForm.trainId"
              placeholder="请输入车次 ID"
              @keydown.enter.prevent="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleQuery" :loading="loading">查询</el-button>
          </el-form-item>
        </el-form>

        <el-card v-if="trainInfo" class="result-card" shadow="never">
          <h4>车次信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="车次 ID">{{ trainInfo.trainId }}</el-descriptions-item>
            <el-descriptions-item label="座位数">{{ trainInfo.seatNum }}</el-descriptions-item>
            <el-descriptions-item label="首发时间">{{ formatStartTime(trainInfo.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="站点数">{{ trainInfo.stations ? trainInfo.stations.length : 0 }}</el-descriptions-item>
          </el-descriptions>

          <div class="result-timeline">
            <div
              v-for="(station, index) in trainInfo.stations"
              :key="index"
              class="timeline-row"
            >
              <div class="timeline-dot" :class="{ 'dot-start': index === 0, 'dot-end': index === trainInfo.stations.length - 1 }"></div>
              <div class="timeline-content">
                <div class="timeline-station">{{ station }}</div>
                <div v-if="index < trainInfo.stations.length - 1" class="timeline-segment">
                  <el-tag size="small" type="info">耗时 {{ formatDuration(trainInfo.durations[index]) }}</el-tag>
                  <el-tag size="small" type="success">¥{{ trainInfo.prices[index] }}</el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import http from '../utils/http'
import { formatStartTime, formatDuration } from '../utils'
import { ElMessage } from 'element-plus'

const activeTab = ref('add')
const loading = ref(false)
const addFormRef = ref()

const addForm = reactive({
  trainId: '',
  seatNum: 1000,
  startTimeDate: null,
  stations: ['', ''],
  durations: [0],
  prices: [0]
})

const addRules = {
  trainId: [{ required: true, message: '请输入车次 ID', trigger: 'blur' }],
  seatNum: [{ required: true, message: '请输入座位数', trigger: 'blur' }],
  startTimeDate: [{ required: true, message: '请选择首发时间', trigger: 'change' }]
}

const addStation = () => {
  addForm.stations.push('')
  addForm.durations.push(0)
  addForm.prices.push(0)
}

const removeStation = (index) => {
  addForm.stations.splice(index, 1)
  if (index > 0) {
    addForm.durations.splice(index - 1, 1)
    addForm.prices.splice(index - 1, 1)
  }
}

const queryForm = reactive({
  trainId: ''
})

const trainInfo = ref(null)

const handleAdd = async () => {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const stations = addForm.stations.map(s => s.trim()).filter(Boolean)
  const durations = addForm.durations.filter(d => d > 0)
  const prices = addForm.prices.filter(p => p >= 0)

  if (stations.length < 2) {
    ElMessage.warning('至少需要 2 个站点')
    return
  }

  if (durations.length !== stations.length - 1) {
    ElMessage.warning('每个区间都需要填写时长')
    return
  }

  if (prices.length !== stations.length - 1) {
    ElMessage.warning('每个区间都需要填写票价')
    return
  }

  const startTime = addForm.startTimeDate
  const pad = (n) => `${n}`.padStart(2, '0')
  const timeStr = `${pad(startTime.getHours())}:${pad(startTime.getMinutes())}`

  loading.value = true
  try {
    const response = await http.post('/api/train/add', {
      trainId: addForm.trainId,
      seatNum: addForm.seatNum,
      startTime: timeStr,
      stations,
      durations,
      prices
    })

    if (response.data.code === 200) {
      ElMessage.success('新增成功')
      addForm.trainId = ''
      addForm.seatNum = 1000
      addForm.startTimeDate = null
      addForm.stations = ['', '']
      addForm.durations = [0]
      addForm.prices = [0]
    } else {
      ElMessage.error(response.data.message || '新增失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '新增失败')
  } finally {
    loading.value = false
  }
}

const handleQuery = async () => {
  if (!queryForm.trainId) {
    ElMessage.warning('请输入车次 ID')
    return
  }

  loading.value = true
  try {
    const response = await http.get(`/api/train/query/${queryForm.trainId}`)

    if (response.data.code === 200) {
      trainInfo.value = response.data.data
    } else {
      ElMessage.error(response.data.message || '查询失败')
      trainInfo.value = null
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '查询失败')
    trainInfo.value = null
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.panel {
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
}

.panel-header h3,
.result-card h4 {
  margin: 0;
}

.panel-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.wide-form {
  max-width: 820px;
}

.narrow-form {
  max-width: 420px;
}

.station-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.station-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.station-index {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #dbeafe;
  color: #1d4ed8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.station-input {
  width: 180px;
}

.segment-info {
  display: flex;
  gap: 8px;
}

.segment-input {
  width: 130px;
}

.add-station-btn {
  margin-top: 12px;
}

.result-card {
  margin-top: 20px;
  border-radius: 20px;
  background: #f8fafc;
}

.result-timeline {
  margin-top: 16px;
  padding-left: 8px;
}

.timeline-row {
  display: flex;
  gap: 16px;
  position: relative;
}

.timeline-row:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 18px;
  bottom: -8px;
  width: 2px;
  background: #e2e8f0;
}

.timeline-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
  margin-top: 4px;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #cbd5e1;
}

.timeline-dot.dot-start {
  background: #22c55e;
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.3);
}

.timeline-dot.dot-end {
  background: #ef4444;
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.3);
}

.timeline-content {
  flex: 1;
  padding-bottom: 16px;
}

.timeline-station {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.timeline-segment {
  margin-top: 6px;
  display: flex;
  gap: 8px;
}
</style>
