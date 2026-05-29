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
        <el-form :model="addForm" label-position="top" class="wide-form">
          <el-form-item label="车次 ID">
            <el-input v-model="addForm.trainId" placeholder="请输入车次 ID" />
          </el-form-item>
          <el-form-item label="座位数">
            <el-input-number v-model="addForm.seatNum" :min="1" />
          </el-form-item>
          <el-form-item label="首发时间">
            <el-input v-model="addForm.startTime" placeholder="格式：HH:mm，例如 08:00" />
          </el-form-item>
          <el-form-item label="站点列表">
            <el-input
              v-model="addForm.stationsInput"
              type="textarea"
              :rows="3"
              placeholder="使用 / 分隔，例如 北京/天津/济南/青岛"
            />
          </el-form-item>
          <el-form-item label="区段时长(分钟)">
            <el-input v-model="addForm.durationsInput" placeholder="使用 / 分隔，例如 35/95/160" />
          </el-form-item>
          <el-form-item label="区段票价">
            <el-input v-model="addForm.pricesInput" placeholder="使用 / 分隔，例如 59/97/118" />
          </el-form-item>
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
          <p><strong>车次 ID：</strong>{{ trainInfo.trainId }}</p>
          <p><strong>座位数：</strong>{{ trainInfo.seatNum }}</p>
          <p><strong>首发时间：</strong>{{ formatStartTime(trainInfo.startTime) }}</p>
          <p><strong>站点：</strong>{{ trainInfo.stations.join(' -> ') }}</p>
          <p><strong>区段时长：</strong>{{ trainInfo.durations.join(' / ') }} 分钟</p>
          <p><strong>区段票价：</strong>{{ trainInfo.prices.join(' / ') }} 元</p>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useStore } from '../store'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const store = useStore()

const activeTab = ref('add')
const loading = ref(false)

const addForm = reactive({
  trainId: '',
  seatNum: 1000,
  startTime: '',
  stationsInput: '',
  durationsInput: '',
  pricesInput: ''
})

const queryForm = reactive({
  trainId: ''
})

const trainInfo = ref(null)

const formatStartTime = (value) => {
  if (!value) {
    return '-'
  }

  const [timePart] = String(value).split(/[_ ]/)
  return timePart || '-'
}

const handleAdd = async () => {
  if (!addForm.trainId || !addForm.startTime || !addForm.stationsInput ||
      !addForm.durationsInput || !addForm.pricesInput) {
    ElMessage.warning('请填写完整信息')
    return
  }

  const stations = addForm.stationsInput.split('/').map(s => s.trim()).filter(Boolean)
  const durations = addForm.durationsInput.split('/').map(s => parseInt(s.trim(), 10)).filter(s => !Number.isNaN(s))
  const prices = addForm.pricesInput.split('/').map(s => parseInt(s.trim(), 10)).filter(s => !Number.isNaN(s))

  if (stations.length < 2) {
    ElMessage.warning('至少需要 2 个站点')
    return
  }

  if (durations.length !== stations.length - 1) {
    ElMessage.warning('区段时长数量应比站点数少 1')
    return
  }

  if (prices.length !== stations.length - 1) {
    ElMessage.warning('区段票价数量应比站点数少 1')
    return
  }

  loading.value = true
  try {
    const response = await axios.post('/api/train/add', {
      trainId: addForm.trainId,
      seatNum: addForm.seatNum,
      startTime: addForm.startTime,
      stations,
      durations,
      prices
    }, {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })

    if (response.data.code === 200) {
      ElMessage.success('新增成功')
      addForm.trainId = ''
      addForm.seatNum = 1000
      addForm.startTime = ''
      addForm.stationsInput = ''
      addForm.durationsInput = ''
      addForm.pricesInput = ''
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
    const response = await axios.get(`/api/train/query/${queryForm.trainId}`, {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })

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

.result-card {
  margin-top: 20px;
  border-radius: 20px;
  background: #f8fafc;
}
</style>
