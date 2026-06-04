<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div>
          <h3>我的订单</h3>
          <p>查看已购车票，并对可退订单执行退票。</p>
        </div>
        <div class="header-actions">
          <el-input
            v-model="searchText"
            placeholder="搜索车次/站点"
            clearable
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="loadOrders" :loading="loading">刷新</el-button>
        </div>
      </div>
    </template>

    <el-table :data="paginatedOrders" class="table" stripe v-loading="loading">
      <el-table-column prop="trainId" label="车次" width="100" />
      <el-table-column prop="departureStation" label="出发站" width="110" />
      <el-table-column prop="arrivalStation" label="到达站" width="110" />
      <el-table-column label="出发时间" width="160">
        <template #default="{ row }">{{ formatDepartureTime(row.departureTime) }}</template>
      </el-table-column>
      <el-table-column label="到达时间" width="160">
        <template #default="{ row }">{{ formatArrivalTime(row) }}</template>
      </el-table-column>
      <el-table-column label="耗时" width="120">
        <template #default="{ row }">{{ formatDuration(row.duration) }}</template>
      </el-table-column>
      <el-table-column prop="price" label="票价" width="80">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="ticketNumber" label="数量" width="70" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getOrderStatus(row).type" effect="dark" size="small">
            {{ getOrderStatus(row).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.refundableCount > 0"
            type="danger"
            size="small"
            @click="openRefundDialog(row)"
          >
            退票
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="filteredOrders.length === 0 && !loading" description="暂无订单" />

    <div v-if="filteredOrders.length > 0" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        background
        layout="total, sizes, prev, pager, next"
        :total="filteredOrders.length"
        :page-sizes="[5, 10, 20]"
      />
    </div>
  </el-card>

  <!-- 退票确认弹窗 -->
  <el-dialog v-model="refundDialogVisible" title="确认退票" width="440px" :close-on-click-modal="false">
    <div v-if="refundOrder" class="refund-confirm">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="车次">{{ refundOrder.trainId }}</el-descriptions-item>
        <el-descriptions-item label="票价">¥{{ refundOrder.price }}</el-descriptions-item>
        <el-descriptions-item label="出发站">{{ refundOrder.departureStation }}</el-descriptions-item>
        <el-descriptions-item label="到达站">{{ refundOrder.arrivalStation }}</el-descriptions-item>
        <el-descriptions-item label="出发时间">{{ formatDepartureTime(refundOrder.departureTime) }}</el-descriptions-item>
        <el-descriptions-item label="已购数量">{{ refundOrder.ticketNumber }}张</el-descriptions-item>
      </el-descriptions>

      <div class="refund-quantity">
        <span class="refund-quantity-label">退票数量</span>
        <el-input-number v-model="refundQuantity" :min="1" :max="refundOrder.refundableCount" />
        <span class="refund-quantity-hint">最多可退 {{ refundOrder.refundableCount }} 张</span>
      </div>

      <div class="refund-warning">
        <el-icon color="#f97316"><WarningFilled /></el-icon>
        <span>退票后将无法恢复，请确认操作。</span>
      </div>
    </div>

    <template #footer>
      <el-button @click="refundDialogVisible = false">取消</el-button>
      <el-button type="danger" :loading="refunding" @click="confirmRefund">确认退票</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import { useStore } from '../store'
import http from '../utils/http'
import { parseBackendTime, formatDateTime, formatDuration } from '../utils'
import { ElMessage } from 'element-plus'

const store = useStore()

const orders = ref([])
const loading = ref(false)
const searchText = ref('')
const pagination = reactive({ page: 1, pageSize: 10 })

const refundDialogVisible = ref(false)
const refundOrder = ref(null)
const refundQuantity = ref(1)
const refunding = ref(false)

const formatDepartureTime = (value) => {
  const date = parseBackendTime(value)
  return formatDateTime(date) || value || '-'
}

const formatArrivalTime = (row) => {
  const date = parseBackendTime(row.departureTime)
  if (!date) return '-'
  const arrival = new Date(date.getTime() + Number(row.duration || 0) * 60000)
  return formatDateTime(arrival)
}

const getOrderStatus = (row) => {
  if (row.refundableCount > 0) {
    return { type: 'success', label: '已出票' }
  }
  return { type: 'info', label: '已退票' }
}

const filteredOrders = computed(() => {
  if (!searchText.value) return orders.value
  const text = searchText.value.toLowerCase()
  return orders.value.filter(order =>
    (order.trainId && order.trainId.toLowerCase().includes(text)) ||
    (order.departureStation && order.departureStation.toLowerCase().includes(text)) ||
    (order.arrivalStation && order.arrivalStation.toLowerCase().includes(text))
  )
})

const paginatedOrders = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredOrders.value.slice(start, start + pagination.pageSize)
})

watch(() => searchText.value, () => { pagination.page = 1 })

watch(() => pagination.pageSize, () => { pagination.page = 1 })

const loadOrders = async () => {
  loading.value = true
  try {
    const response = await http.get('/api/ticket/orders')
    if (response.data.code === 200) {
      orders.value = response.data.data || []
    } else {
      ElMessage.error(response.data.message || '查询失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

const openRefundDialog = (order) => {
  refundOrder.value = order
  refundQuantity.value = 1
  refundDialogVisible.value = true
}

const confirmRefund = async () => {
  if (!refundOrder.value) return
  const order = refundOrder.value
  const quantity = refundQuantity.value

  if (quantity > order.refundableCount) {
    ElMessage.warning(`当前最多可退 ${order.refundableCount} 张`)
    return
  }

  refunding.value = true
  try {
    const response = await http.post('/api/ticket/refund', {
      trainId: order.trainId,
      departureTime: order.departureTime,
      departureStation: order.departureStation,
      arrivalStation: order.arrivalStation,
      quantity
    })

    if (response.data.code === 200) {
      ElMessage.success('退票成功')
      refundDialogVisible.value = false
      loadOrders()
    } else {
      ElMessage.error(response.data.message || '退票失败')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.response?.data?.message || '退票失败')
    }
  } finally {
    refunding.value = false
  }
}

onMounted(() => {
  loadOrders()
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
  flex-wrap: wrap;
}

.panel-header h3 {
  margin: 0;
  font-size: 24px;
}

.panel-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 220px;
}

.table {
  margin-top: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.refund-confirm {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.refund-quantity {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 4px;
}

.refund-quantity-label {
  font-weight: 600;
  color: #334155;
}

.refund-quantity-hint {
  font-size: 13px;
  color: #94a3b8;
}

.refund-warning {
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
