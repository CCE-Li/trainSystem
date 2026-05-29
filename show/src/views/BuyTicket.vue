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

      <el-alert
        class="result-tip"
        type="info"
        :closable="false"
        :title="`当前共筛选出 ${filteredTickets.length} 条可购买区间，第 ${pagination.page} / ${totalPages} 页`"
      />

      <el-table :data="paginatedTickets" class="ticket-table" stripe v-loading="loading">
        <el-table-column prop="departureTimeLabel" label="出发时间" width="180" sortable />
        <el-table-column prop="arrivalTimeLabel" label="到达时间" width="180" sortable />
        <el-table-column prop="departureStation" label="出发站" width="140" />
        <el-table-column prop="arrivalStation" label="到达站" width="140" />
        <el-table-column prop="price" label="价格" width="100" sortable>
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="durationLabel" label="耗时" width="120" sortable />
        <el-table-column prop="seatNum" label="余票" width="100" sortable />
        <el-table-column prop="trainId" label="车次" width="120" sortable />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              type="success"
              size="small"
              :disabled="row.seatNum <= 0 || buyingTrainId === row.purchaseKey"
              :loading="buyingTrainId === row.purchaseKey"
              @click="handleBuy(row)"
            >
              购票
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && filteredTickets.length === 0"
        description="当前筛选条件下没有可购买的车票"
      />

      <div v-else class="pagination-wrap">
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Switch } from '@element-plus/icons-vue'
import { useStore } from '../store'

const store = useStore()

const loading = ref(false)
const buyingTrainId = ref('')
const segmentTickets = ref([])
const trainSchedulers = ref([])
const isLoggedIn = computed(() => Boolean(store.sessionId && store.userInfo))

const filters = reactive({
  departureStation: '',
  arrivalStation: '',
  departureDate: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10
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

const formatDate = (date) => {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
    return ''
  }

  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
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

const formatDuration = (minutes) => {
  if (minutes === null || minutes === undefined || Number.isNaN(Number(minutes))) {
    return '-'
  }

  const total = Number(minutes)
  const hours = Math.floor(total / 60)
  const remainMinutes = total % 60
  if (hours === 0) {
    return `${remainMinutes}分钟`
  }
  return `${hours}小时${remainMinutes}分钟`
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
  if (!departureDate) {
    return null
  }

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
    if (!scheduler || !Array.isArray(scheduler.stations) || scheduler.stations.length < 2) {
      return
    }

    const segmentIndex = scheduler.stations.findIndex((station, index) => {
      if (index + 1 >= scheduler.stations.length) {
        return false
      }
      return station === ticket.departureStation && scheduler.stations[index + 1] === ticket.arrivalStation
    })
    if (segmentIndex < 0) {
      return
    }

    const baseDate = calculateRunBaseDate(ticket, scheduler, segmentIndex)
    if (!baseDate) {
      return
    }

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
          if (!segment) {
            valid = false
            break
          }
          remaining = Math.min(remaining, Number(segment.seatNum))
          totalDuration += Number(durations[index] || 0)
          totalPrice += Number(prices[index] || 0)
        }

        if (!valid || !Number.isFinite(remaining)) {
          continue
        }

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
          departureTimeLabel: formatDate(departureDate),
          arrivalTimeLabel: formatDate(arrivalDate),
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
    if (ticket.departureStation) {
      set.add(ticket.departureStation)
    }
    if (ticket.arrivalStation) {
      set.add(ticket.arrivalStation)
    }
  })
  return Array.from(set)
})

const departureStationOptions = computed(() => stationOptions.value)

const arrivalStationOptions = computed(() => {
  if (!filters.departureStation) {
    return stationOptions.value
  }

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
    if (filters.departureStation && ticket.departureStation !== filters.departureStation) {
      return false
    }
    if (filters.arrivalStation && ticket.arrivalStation !== filters.arrivalStation) {
      return false
    }
    if (filters.departureDate && ticket.departureDateOnly !== filters.departureDate) {
      return false
    }
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
  () => {
    pagination.page = 1
  }
)

watch(
  () => filteredTickets.value.length,
  (length) => {
    if (length === 0) {
      pagination.page = 1
      return
    }

    if (pagination.page > totalPages.value) {
      pagination.page = totalPages.value
    }
  }
)

watch(
  () => pagination.pageSize,
  () => {
    pagination.page = 1
  }
)

const loadData = async () => {
  loading.value = true
  try {
    const [ticketsResponse, trainsResponse] = await Promise.all([
      axios.get('/api/ticket/list', {
        headers: {
          Authorization: `Bearer ${store.sessionId}`
        }
      }),
      axios.get('/api/train/list', {
        headers: {
          Authorization: `Bearer ${store.sessionId}`
        }
      })
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

const handleBuy = async (ticket) => {
  let quantity = 1
  try {
    const result = await ElMessageBox.prompt('请输入购票数量', '购票数量', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: '1',
      inputPattern: /^[1-9]\d*$/,
      inputErrorMessage: '请输入大于 0 的整数'
    })
    quantity = Number(result.value)
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error('购票数量输入无效')
    return
  }

  if (quantity > ticket.seatNum) {
    ElMessage.warning(`当前最多可购买 ${ticket.seatNum} 张`)
    return
  }

  buyingTrainId.value = ticket.purchaseKey
  try {
    const response = await axios.post('/api/ticket/buy', {
      trainId: ticket.trainId,
      departureStation: ticket.departureStation,
      arrivalStation: ticket.arrivalStation,
      departureTime: ticket.departureTime,
      quantity
    }, {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })

    if (response.data.code === 200) {
      ElMessage.success('购票成功')
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

.result-tip {
  margin-top: 18px;
}

.ticket-table {
  margin-top: 18px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 1200px) {
  .filter-bar {
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  }

  .switch-wrap {
    justify-content: flex-start;
    padding-bottom: 0;
  }
}
</style>
