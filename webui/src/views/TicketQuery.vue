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

    <!-- 统计概览 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-value">{{ filteredTicketCount }}</div>
        <div class="stat-label">票务记录数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ totalSeats }}</div>
        <div class="stat-label">总余票数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ filteredTicketCount > 0 ? Math.round(totalSeats / filteredTicketCount) : 0 }}</div>
        <div class="stat-label">平均余票</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ soldOutCount }}</div>
        <div class="stat-label">售罄区间</div>
      </div>
    </div>

    <!-- 余票分布柱状图 -->
    <div v-if="seatDistribution.length > 0" class="chart-section">
      <h4>余票分布</h4>
      <div class="bar-chart">
        <div v-for="(item, index) in seatDistribution" :key="index" class="bar-item">
          <div class="bar-label">{{ item.label }}</div>
          <div class="bar-track">
            <div
              class="bar-fill"
              :style="{ width: item.percent + '%', background: item.color }"
            ></div>
          </div>
          <div class="bar-value">{{ item.count }}张</div>
        </div>
      </div>
    </div>

    <!-- 明细表格 -->
    <el-table :data="filteredTickets" class="detail-table" stripe v-loading="loading" max-height="420">
      <el-table-column prop="trainId" label="车次" width="120" />
      <el-table-column prop="departureStation" label="出发站" width="120" />
      <el-table-column prop="arrivalStation" label="到达站" width="120" />
      <el-table-column label="出发时间" width="160">
        <template #default="{ row }">{{ formatDepartureTime(row.departureTime) }}</template>
      </el-table-column>
      <el-table-column label="余票" width="140">
        <template #default="{ row }">
          <el-tag :type="getSeatStatus(row.seatNum).type" effect="dark" size="small">
            {{ row.seatNum }}张 {{ getSeatStatus(row.seatNum).label }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="filteredTickets.length === 0 && !loading" description="暂无票务数据" />
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import http from '../utils/http'
import { parseBackendTime, formatDateOnly, formatDateTime, formatStartTime, getSeatStatus } from '../utils'
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

const formatTrainLabel = (train) => {
  const stationText = train.stations?.length ? train.stations.join(' → ') : '暂无站点信息'
  const startTime = formatStartTime(train.startTime)
  return `${train.trainId} | ${startTime} | ${stationText}`
}

const formatDepartureTime = (value) => {
  const date = parseBackendTime(value)
  return formatDateTime(date) || value || '-'
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
    if (filters.trainId && ticket.trainId !== filters.trainId) return
    if (filters.departureStation && ticket.departureStation !== filters.departureStation) return
    set.add(ticket.arrivalStation)
  })
  return Array.from(set)
})

const filteredTickets = computed(() => {
  return ticketRows.value.filter(ticket => {
    if (filters.trainId && ticket.trainId !== filters.trainId) return false
    if (filters.departureStation && ticket.departureStation !== filters.departureStation) return false
    if (filters.arrivalStation && ticket.arrivalStation !== filters.arrivalStation) return false
    if (filters.departureDate && ticket.departureDateOnly !== filters.departureDate) return false
    return ticket.seatNum >= 0
  })
})

const filteredTicketCount = computed(() => filteredTickets.value.length)

const totalSeats = computed(() =>
  filteredTickets.value.reduce((sum, t) => sum + Number(t.seatNum || 0), 0)
)

const soldOutCount = computed(() =>
  filteredTickets.value.filter(t => Number(t.seatNum || 0) <= 0).length
)

const seatDistribution = computed(() => {
  const sufficient = filteredTickets.value.filter(t => Number(t.seatNum) > 20).length
  const limited = filteredTickets.value.filter(t => Number(t.seatNum) > 5 && Number(t.seatNum) <= 20).length
  const tight = filteredTickets.value.filter(t => Number(t.seatNum) > 0 && Number(t.seatNum) <= 5).length
  const soldOut = filteredTickets.value.filter(t => Number(t.seatNum) <= 0).length
  const max = Math.max(sufficient, limited, tight, soldOut, 1)

  return [
    { label: '充足 (>20)', count: sufficient, percent: (sufficient / max) * 100, color: '#22c55e' },
    { label: '较少 (6-20)', count: limited, percent: (limited / max) * 100, color: '#eab308' },
    { label: '紧张 (1-5)', count: tight, percent: (tight / max) * 100, color: '#f97316' },
    { label: '售罄 (0)', count: soldOut, percent: (soldOut / max) * 100, color: '#ef4444' }
  ]
})

const loadTrains = async () => {
  try {
    const response = await http.get('/api/train/list')
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
    const response = await http.get('/api/ticket/list')
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

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-top: 24px;
}

.stat-card {
  padding: 20px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1;
}

.stat-label {
  margin-top: 8px;
  font-size: 13px;
  color: #64748b;
}

.chart-section {
  margin-top: 24px;
}

.chart-section h4 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #334155;
}

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bar-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  width: 90px;
  font-size: 13px;
  color: #475569;
  text-align: right;
  flex-shrink: 0;
}

.bar-track {
  flex: 1;
  height: 20px;
  background: #f1f5f9;
  border-radius: 10px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 10px;
  transition: width 0.4s ease;
  min-width: 2px;
}

.bar-value {
  width: 60px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.detail-table {
  margin-top: 24px;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
