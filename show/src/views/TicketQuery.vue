<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div>
          <h3>余票查询</h3>
          <p>管理员可按条件统计当前已发售车票数量。</p>
        </div>
        <el-tag type="success" effect="dark">管理员</el-tag>
      </div>
    </template>

    <div class="query-grid">
      <el-select
        v-model="filters.trainId"
        placeholder="请选择车次"
        filterable
        clearable
        class="filter-item"
      >
        <el-option
          v-for="train in trainOptions"
          :key="train.trainId"
          :label="formatTrainLabel(train)"
          :value="train.trainId"
        />
      </el-select>

      <el-select
        v-model="filters.departureStation"
        placeholder="请选择出发站"
        filterable
        clearable
        class="filter-item"
      >
        <el-option
          v-for="station in departureStationOptions"
          :key="station"
          :label="station"
          :value="station"
        />
      </el-select>

      <el-select
        v-model="filters.arrivalStation"
        placeholder="请选择终点站"
        filterable
        clearable
        class="filter-item"
      >
        <el-option
          v-for="station in arrivalStationOptions"
          :key="station"
          :label="station"
          :value="station"
        />
      </el-select>

      <el-date-picker
        v-model="filters.departureDate"
        type="date"
        placeholder="选择出发日期"
        value-format="YYYY-MM-DD"
        format="YYYY-MM-DD"
        class="filter-item"
      />

      <el-button type="primary" :loading="loading" @click="loadTickets">刷新统计</el-button>
    </div>

    <el-result
      v-if="!loading"
      icon="success"
      :title="`车票数量：${filteredTicketCount}`"
      sub-title="结果表示当前筛选条件下已发售车票记录数量。"
      class="result-card"
    />
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useStore } from '../store'

const store = useStore()

const loading = ref(false)
const tickets = ref([])
const trains = ref([])

const filters = reactive({
  trainId: '',
  departureStation: '',
  arrivalStation: '',
  departureDate: ''
})

const parseBackendTime = (value) => {
  if (!value) {
    return null
  }

  const normalized = value.replace('_', ' ')
  const [timePart, datePart] = normalized.split(' ')
  if (!timePart || !datePart) {
    return null
  }

  const [month, day] = datePart.split('-').map(Number)
  const [hour, minute] = timePart.split(':').map(Number)

  if ([month, day, hour, minute].some(Number.isNaN)) {
    return null
  }

  const year = new Date().getFullYear()
  return new Date(year, month - 1, day, hour, minute)
}

const formatDateOnly = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    return ''
  }

  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatTrainLabel = (train) => {
  const stationText = train.stations?.length ? train.stations.join(' -> ') : '暂无站点信息'
  const startTime = String(train.startTime || '').split(/[_ ]/)[0] || '-'
  return `${train.trainId} | ${startTime} | ${stationText}`
}

const ticketRows = computed(() => {
  return tickets.value.map(ticket => {
    const departureDate = parseBackendTime(ticket.departureTime)
    return {
      ...ticket,
      departureDateOnly: formatDateOnly(departureDate)
    }
  })
})

const trainOptions = computed(() => trains.value)

const departureStationOptions = computed(() => {
  const set = new Set()
  ticketRows.value.forEach(ticket => {
    if (!filters.trainId || ticket.trainId === filters.trainId) {
      set.add(ticket.departureStation)
    }
  })
  return Array.from(set)
})

const arrivalStationOptions = computed(() => {
  const set = new Set()
  ticketRows.value.forEach(ticket => {
    if (filters.trainId && ticket.trainId !== filters.trainId) {
      return
    }
    if (filters.departureStation && ticket.departureStation !== filters.departureStation) {
      return
    }
    set.add(ticket.arrivalStation)
  })
  return Array.from(set)
})

const filteredTicketCount = computed(() => {
  return ticketRows.value.filter(ticket => {
    if (filters.trainId && ticket.trainId !== filters.trainId) {
      return false
    }
    if (filters.departureStation && ticket.departureStation !== filters.departureStation) {
      return false
    }
    if (filters.arrivalStation && ticket.arrivalStation !== filters.arrivalStation) {
      return false
    }
    if (filters.departureDate && ticket.departureDateOnly !== filters.departureDate) {
      return false
    }
    return ticket.seatNum >= 0
  }).length
})

const loadTrains = async () => {
  try {
    const response = await axios.get('/api/train/list', {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })

    if (response.data.code === 200) {
      trains.value = response.data.data || []
      return
    }

    ElMessage.error(response.data.message || '加载车次失败')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载车次失败')
  }
}

const loadTickets = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/ticket/list', {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })

    if (response.data.code === 200) {
      tickets.value = response.data.data || []
      return
    }

    ElMessage.error(response.data.message || '加载票务数据失败')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载票务数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadTrains(), loadTickets()])
})
</script>

<style scoped>
.panel {
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.panel-header h3 {
  margin: 0;
  font-size: 24px;
}

.panel-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.query-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
  align-items: end;
}

.filter-item {
  width: 100%;
}

.result-card {
  margin-top: 28px;
}
</style>
