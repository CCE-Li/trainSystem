<template>
  <div class="buy-ticket-view">
    <el-card class="panel" shadow="never">
      <template #header>
        <div class="panel-header">
          <div>
            <h3>在线购票</h3>
            <p>按出发站、到达站和出发日期筛选可购买区间，并直接下单。</p>
          </div>
          <el-tag :type="isLoggedIn ? 'success' : 'warning'" effect="dark">
            {{ isLoggedIn ? '已登录' : '需登录' }}
          </el-tag>
        </div>
      </template>

      <div class="filter-bar">
        <el-select
          v-model="filters.departureStation"
          placeholder="选择出发站"
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

        <div class="switch-wrap">
          <el-button circle @click="swapStations">
            <el-icon><Switch /></el-icon>
          </el-button>
        </div>

        <el-select
          v-model="filters.arrivalStation"
          placeholder="选择到达站"
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

        <el-button type="primary" :loading="loading" @click="loadData">刷新列表</el-button>
      </div>

      <div class="quick-dates">
        <el-button
          v-for="item in quickDates"
          :key="item.label"
          :type="filters.departureDate === item.value ? 'primary' : ''"
          size="small"
          @click="filters.departureDate = item.value"
        >
          {{ item.label }}
        </el-button>
      </div>

      <el-alert
        class="result-tip"
        type="info"
        :closable="false"
        :title="`当前共筛选出 ${filteredTickets.length} 条可购买区间，第 ${pagination.page} / ${totalPages} 页`"
      />

      <!-- 行程卡片列表 -->
      <div class="journey-list" v-loading="loading">
        <div
          v-for="ticket in paginatedTickets"
          :key="ticket.purchaseKey"
          class="journey-card"
          :class="{ 'is-low-seat': ticket.seatNum <= 5 && ticket.seatNum > 0, 'is-no-seat': ticket.seatNum <= 0 }"
        >
          <!-- 车次标识 -->
          <div class="journey-train-id">
            <span class="train-badge">{{ ticket.trainId }}</span>
          </div>

          <!-- 行程主体 -->
          <div class="journey-main">
            <!-- 出发信息 -->
            <div class="journey-point journey-departure">
              <div class="point-time">{{ formatTimeOnly(ticket.departureDate) }}</div>
              <div class="point-station">{{ ticket.departureStation }}</div>
            </div>

            <!-- 行程中间 -->
            <div class="journey-middle">
              <div class="middle-duration">{{ ticket.durationLabel }}</div>
              <div class="middle-line">
                <span class="line-dot"></span>
                <span class="line-bar"></span>
                <span class="line-arrow">→</span>
              </div>
            </div>

            <!-- 到达信息 -->
            <div class="journey-point journey-arrival">
              <div class="point-time">{{ formatTimeOnly(ticket.arrivalDate) }}</div>
              <div class="point-station">{{ ticket.arrivalStation }}</div>
            </div>
          </div>

          <!-- 价格与余票 -->
          <div class="journey-info">
            <div class="info-price">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ ticket.price }}</span>
            </div>
            <div class="info-seat">
              <el-tag
                :type="getSeatStatus(ticket.seatNum).type"
                effect="dark"
                size="small"
              >
                {{ ticket.seatNum }}张 {{ getSeatStatus(ticket.seatNum).label }}
              </el-tag>
            </div>
          </div>

          <!-- 购票按钮 -->
          <div class="journey-action">
            <el-button
              type="primary"
              :disabled="ticket.seatNum <= 0 || buyingTrainId === ticket.purchaseKey"
              :loading="buyingTrainId === ticket.purchaseKey"
              @click="openBuyDialog(ticket)"
            >
              {{ ticket.seatNum <= 0 ? '售罄' : '购票' }}
            </el-button>
          </div>
        </div>

        <el-empty
          v-if="!loading && paginatedTickets.length === 0"
          :description="hasActiveFilters ? '当前筛选条件下没有可购买的车票' : '暂无可购买的车票，请点击刷新'"
        />
      </div>

      <div v-if="!loading && paginatedTickets.length > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredTickets.length"
          :page-sizes="[5, 10, 20, 50]"
        />
      </div>
    </el-card>

    <!-- 购票确认弹窗 -->
    <el-dialog v-model="buyDialogVisible" title="确认购票" width="480px" :close-on-click-modal="false">
      <div v-if="selectedTicket" class="buy-confirm">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="车次">{{ selectedTicket.trainId }}</el-descriptions-item>
          <el-descriptions-item label="单价">¥{{ selectedTicket.price }}</el-descriptions-item>
          <el-descriptions-item label="出发站">{{ selectedTicket.departureStation }}</el-descriptions-item>
          <el-descriptions-item label="到达站">{{ selectedTicket.arrivalStation }}</el-descriptions-item>
          <el-descriptions-item label="出发时间">{{ selectedTicket.departureTimeLabel }}</el-descriptions-item>
          <el-descriptions-item label="到达时间">{{ selectedTicket.arrivalTimeLabel }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ selectedTicket.durationLabel }}</el-descriptions-item>
          <el-descriptions-item label="余票">
            <el-tag :type="getSeatStatus(selectedTicket.seatNum).type" size="small">
              {{ selectedTicket.seatNum }}张
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="buy-quantity">
          <span class="buy-quantity-label">购票数量</span>
          <el-input-number v-model="buyQuantity" :min="1" :max="selectedTicket.seatNum" />
        </div>

        <div class="buy-total">
          合计：<strong>¥{{ selectedTicket.price * buyQuantity }}</strong>
        </div>
      </div>

      <template #footer>
        <el-button @click="buyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="buyingTrainId !== ''" @click="confirmBuy">确认购票</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import http from '../utils/http'
import { parseBackendTime, formatDateOnly, formatDateTime, formatDuration, getSeatStatus } from '../utils'
import { ElMessage } from 'element-plus'
import { Switch } from '@element-plus/icons-vue'
import { useStore } from '../store'

const store = useStore()
const route = useRoute()

// 格式化时间为 HH:mm
const formatTimeOnly = (date) => {
  if (!date) return '--'
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes}`
}

const loading = ref(false)
const buyingTrainId = ref('')
const segmentTickets = ref([])
const trainSchedulers = ref([])
const isLoggedIn = computed(() => Boolean(store.sessionId && store.userInfo))

const buyDialogVisible = ref(false)
const selectedTicket = ref(null)
const buyQuantity = ref(1)

const filters = reactive({
  departureStation: '',
  arrivalStation: '',
  departureDate: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10
})

// 日期快捷选项
const quickDates = computed(() => {
  const today = new Date()
  const dates = []
  for (let i = 0; i < 3; i++) {
    const d = new Date(today)
    d.setDate(d.getDate() + i)
    const value = formatDateOnly(d)
    const labels = ['今天', '明天', '后天']
    dates.push({ label: labels[i], value })
  }
  return dates
})

const hasActiveFilters = computed(() =>
  Boolean(filters.departureStation || filters.arrivalStation || filters.departureDate)
)

const applyRouteFilters = () => {
  const departureStation = typeof route.query.departureStation === 'string'
    ? route.query.departureStation
    : ''
  const arrivalStation = typeof route.query.arrivalStation === 'string'
    ? route.query.arrivalStation
    : ''
  const departureDate = typeof route.query.departureDate === 'string'
    ? route.query.departureDate
    : ''

  filters.departureStation = departureStation
  filters.arrivalStation = arrivalStation
  filters.departureDate = departureDate
}

const schedulesByTrainId = computed(() => {
  const map = new Map()
  trainSchedulers.value.forEach((scheduler) => {
    map.set(scheduler.trainId, scheduler)
  })
  return map
})

const segmentRows = computed(() => {
  return segmentTickets.value
    .map((ticket) => {
      const departureDate = parseBackendTime(ticket.departureTime)
      return {
        ...ticket,
        departureDate,
        departureDateOnly: formatDateOnly(departureDate)
      }
    })
    .filter((ticket) => ticket.departureDate)
})

const buildRunKey = (trainId, baseDate) => {
  return `${trainId}-${baseDate.toISOString()}`
}

const calculateRunBaseDate = (ticket, scheduler, segmentIndex) => {
  const departureDate = parseBackendTime(ticket.departureTime)
  if (!departureDate) return null

  let offsetMinutes = 0
  for (let i = 0; i < segmentIndex; i += 1) {
    offsetMinutes += Number(scheduler.durations?.[i] || 0)
  }
  return new Date(departureDate.getTime() - offsetMinutes * 60000)
}

const journeyRows = computed(() => {
  const groupedRuns = new Map()

  segmentRows.value.forEach((ticket) => {
    const scheduler = schedulesByTrainId.value.get(ticket.trainId)
    if (!scheduler || !Array.isArray(scheduler.stations) || scheduler.stations.length < 2) return

    const segmentIndex = scheduler.stations.findIndex((station, index) => {
      if (index + 1 >= scheduler.stations.length) return false
      return station === ticket.departureStation && scheduler.stations[index + 1] === ticket.arrivalStation
    })
    if (segmentIndex < 0) return

    const baseDate = calculateRunBaseDate(ticket, scheduler, segmentIndex)
    if (!baseDate) return

    const runKey = buildRunKey(ticket.trainId, baseDate)
    if (!groupedRuns.has(runKey)) {
      groupedRuns.set(runKey, {
        trainId: ticket.trainId,
        scheduler,
        baseDate,
        segments: new Map()
      })
    }

    groupedRuns.get(runKey).segments.set(segmentIndex, ticket)
  })

  const journeys = []
  groupedRuns.forEach(({ trainId, scheduler, baseDate, segments }) => {
    const stations = scheduler.stations || []
    const durations = scheduler.durations || []
    const prices = scheduler.prices || []

    for (let start = 0; start < stations.length - 1; start += 1) {
      for (let end = start + 1; end < stations.length; end += 1) {
        let valid = true
        let remaining = Number.POSITIVE_INFINITY
        let totalDuration = 0
        let totalPrice = 0

        for (let index = start; index < end; index += 1) {
          const segment = segments.get(index)
          if (!segment) { valid = false; break }
          remaining = Math.min(remaining, Number(segment.seatNum))
          totalDuration += Number(durations[index] || 0)
          totalPrice += Number(prices[index] || 0)
        }

        if (!valid || !Number.isFinite(remaining)) continue

        const departureSegment = segments.get(start)
        const departureDate = parseBackendTime(departureSegment.departureTime)
        const arrivalDate = departureDate ? new Date(departureDate.getTime() + totalDuration * 60000) : null

        journeys.push({
          trainId,
          departureStation: stations[start],
          arrivalStation: stations[end],
          departureTime: departureSegment.departureTime,
          seatNum: remaining,
          price: totalPrice,
          duration: totalDuration,
          departureDate,
          arrivalDate,
          departureDateOnly: formatDateOnly(departureDate),
          departureTimeLabel: formatDateTime(departureDate),
          arrivalTimeLabel: formatDateTime(arrivalDate),
          durationLabel: formatDuration(totalDuration),
          purchaseKey: `${trainId}-${baseDate.toISOString()}-${stations[start]}-${stations[end]}`
        })
      }
    }
  })

  return journeys.sort((left, right) => {
    const leftTime = left.departureDate?.getTime?.() || 0
    const rightTime = right.departureDate?.getTime?.() || 0
    return leftTime - rightTime
  })
})

const stationOptions = computed(() => {
  const set = new Set()
  journeyRows.value.forEach((ticket) => {
    if (ticket.departureStation) set.add(ticket.departureStation)
    if (ticket.arrivalStation) set.add(ticket.arrivalStation)
  })
  return Array.from(set)
})

const departureStationOptions = computed(() => stationOptions.value)

const arrivalStationOptions = computed(() => {
  if (!filters.departureStation) return stationOptions.value

  const set = new Set()
  journeyRows.value.forEach((ticket) => {
    if (ticket.departureStation === filters.departureStation && ticket.arrivalStation) {
      set.add(ticket.arrivalStation)
    }
  })
  return Array.from(set)
})

const filteredTickets = computed(() => {
  return journeyRows.value.filter((ticket) => {
    if (filters.departureStation && ticket.departureStation !== filters.departureStation) return false
    if (filters.arrivalStation && ticket.arrivalStation !== filters.arrivalStation) return false
    if (filters.departureDate && ticket.departureDateOnly !== filters.departureDate) return false
    return ticket.seatNum > 0
  })
})

const totalPages = computed(() => {
  const total = Math.ceil(filteredTickets.value.length / pagination.pageSize)
  return total > 0 ? total : 1
})

const paginatedTickets = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredTickets.value.slice(start, end)
})

watch(
  () => [filters.departureStation, filters.arrivalStation, filters.departureDate],
  () => { pagination.page = 1 }
)

watch(
  () => filteredTickets.value.length,
  (length) => {
    if (length === 0) { pagination.page = 1; return }
    if (pagination.page > totalPages.value) pagination.page = totalPages.value
  }
)

watch(
  () => pagination.pageSize,
  () => { pagination.page = 1 }
)

watch(
  () => route.query,
  () => { applyRouteFilters() },
  { deep: true }
)

const loadData = async () => {
  loading.value = true
  try {
    const [ticketsResponse, trainsResponse] = await Promise.all([
      http.get('/api/ticket/list'),
      http.get('/api/train/list')
    ])

    if (ticketsResponse.data.code !== 200) {
      ElMessage.error(ticketsResponse.data.message || '加载票务列表失败')
      return
    }

    if (trainsResponse.data.code !== 200) {
      ElMessage.error(trainsResponse.data.message || '加载车次列表失败')
      return
    }

    segmentTickets.value = ticketsResponse.data.data || []
    trainSchedulers.value = trainsResponse.data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载票务数据失败')
  } finally {
    loading.value = false
  }
}

const swapStations = () => {
  const departureStation = filters.departureStation
  filters.departureStation = filters.arrivalStation
  filters.arrivalStation = departureStation
}

const openBuyDialog = (ticket) => {
  selectedTicket.value = ticket
  buyQuantity.value = 1
  buyDialogVisible.value = true
}

const confirmBuy = async () => {
  if (!selectedTicket.value) return
  const ticket = selectedTicket.value
  const quantity = buyQuantity.value

  if (quantity > ticket.seatNum) {
    ElMessage.warning(`当前最多可购买 ${ticket.seatNum} 张`)
    return
  }

  buyingTrainId.value = ticket.purchaseKey
  try {
    const response = await http.post('/api/ticket/buy', {
      trainId: ticket.trainId,
      departureStation: ticket.departureStation,
      arrivalStation: ticket.arrivalStation,
      departureTime: ticket.departureTime,
      quantity
    })

    if (response.data.code === 200) {
      ElMessage.success('购票成功')
      buyDialogVisible.value = false
      await loadData()
      return
    }

    ElMessage.error(response.data.message || '购票失败')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '购票失败')
  } finally {
    buyingTrainId.value = ''
  }
}

onMounted(() => {
  applyRouteFilters()
  loadData()
})
</script>

<style scoped>
.buy-ticket-view {
  width: 100%;
}

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

.filter-bar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto minmax(220px, 1fr) minmax(220px, 1fr) auto;
  gap: 16px;
  align-items: end;
}

.filter-item {
  width: 100%;
}

.switch-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding-bottom: 4px;
}

.quick-dates {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.result-tip {
  margin-top: 18px;
}

/* ===== 行程卡片列表 ===== */
.journey-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 20px;
  min-height: 200px;
}

.journey-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.journey-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.12);
}

.journey-card.is-low-seat {
  border-color: #fb923c;
  background: linear-gradient(to right, #fff7ed, #fff);
}

.journey-card.is-no-seat {
  border-color: #e2e8f0;
  background: #f8fafc;
  opacity: 0.7;
}

/* 车次标识 */
.journey-train-id {
  flex-shrink: 0;
  width: 80px;
}

.train-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  background: linear-gradient(135deg, #1e40af, #3b82f6);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  border-radius: 8px;
  letter-spacing: 0.5px;
}

/* 行程主体 */
.journey-main {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.journey-point {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 70px;
}

.point-time {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.point-station {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.journey-departure .point-time {
  color: #059669;
}

.journey-arrival .point-time {
  color: #dc2626;
}

/* 行程中间 */
.journey-middle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 100px;
}

.middle-duration {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.middle-line {
  display: flex;
  align-items: center;
  gap: 2px;
}

.line-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
}

.line-bar {
  width: 60px;
  height: 2px;
  background: linear-gradient(to right, #22c55e, #ef4444);
}

.line-arrow {
  font-size: 14px;
  color: #ef4444;
  font-weight: bold;
}

/* 价格与余票 */
.journey-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 90px;
}

.info-price {
  display: flex;
  align-items: baseline;
}

.price-symbol {
  font-size: 14px;
  color: #dc2626;
  font-weight: 600;
}

.price-value {
  font-size: 24px;
  color: #dc2626;
  font-weight: 700;
  line-height: 1;
}

.info-seat {
  display: flex;
  justify-content: center;
}

/* 购票按钮 */
.journey-action {
  flex-shrink: 0;
}

.journey-action .el-button {
  min-width: 80px;
  height: 40px;
  font-size: 15px;
  font-weight: 600;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 购票确认弹窗 */
.buy-confirm {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.buy-quantity {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 8px;
}

.buy-quantity-label {
  font-weight: 600;
  color: #334155;
}

.buy-total {
  text-align: right;
  font-size: 16px;
  color: #475569;
}

.buy-total strong {
  font-size: 22px;
  color: #dc2626;
}

/* 响应式 */
@media (max-width: 1200px) {
  .filter-bar {
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  }

  .switch-wrap {
    justify-content: flex-start;
    padding-bottom: 0;
  }

  .journey-card {
    flex-wrap: wrap;
    gap: 12px;
  }

  .journey-train-id {
    width: auto;
  }

  .journey-main {
    flex: 1 1 100%;
    order: 3;
  }

  .journey-info {
    flex-direction: row;
    align-items: center;
    gap: 12px;
  }
}

@media (max-width: 768px) {
  .journey-middle {
    min-width: 80px;
  }

  .line-bar {
    width: 40px;
  }

  .point-time {
    font-size: 18px;
  }

  .price-value {
    font-size: 20px;
  }
}
</style>
