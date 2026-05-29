<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div>
          <h3>我的订单</h3>
          <p>查看已购车票，并对可退订单执行退票。</p>
        </div>
        <el-button type="primary" @click="loadOrders" :loading="loading">刷新</el-button>
      </div>
    </template>

    <el-table :data="orders" class="table" stripe>
      <el-table-column prop="trainId" label="车次 ID" width="120" />
      <el-table-column prop="departureStation" label="出发站" width="120" />
      <el-table-column prop="arrivalStation" label="到达站" width="120" />
      <el-table-column prop="departureTime" label="出发时间" width="150" />
      <el-table-column prop="arrivalTime" label="到达时间" width="150" />
      <el-table-column prop="duration" label="时长(分钟)" width="110" />
      <el-table-column prop="price" label="票价" width="100" />
      <el-table-column prop="ticketNumber" label="已购数量" width="100" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.refundableCount > 0"
            type="danger"
            size="small"
            @click="handleRefund(row)"
          >
            退票
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="orders.length === 0 && !loading" description="暂无订单" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useStore } from '../store'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const store = useStore()

const orders = ref([])
const loading = ref(false)

const loadOrders = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/ticket/orders', {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })

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

const handleRefund = async (order) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `当前最多可退 ${order.refundableCount} 张，请输入退票数量`,
      '退票确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: '1',
        inputPattern: /^[1-9]\d*$/,
        inputErrorMessage: '请输入大于 0 的整数'
      }
    )

    const quantity = Number(value)
    if (quantity > order.refundableCount) {
      ElMessage.warning(`当前最多可退 ${order.refundableCount} 张`)
      return
    }

    const response = await axios.post('/api/ticket/refund', {
      trainId: order.trainId,
      departureTime: order.departureTime,
      departureStation: order.departureStation,
      arrivalStation: order.arrivalStation,
      quantity
    }, {
      headers: {
        Authorization: `Bearer ${store.sessionId}`
      }
    })

    if (response.data.code === 200) {
      ElMessage.success('退票成功')
      loadOrders()
    } else {
      ElMessage.error(response.data.message || '退票失败')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.response?.data?.message || '退票失败')
    }
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
}

.panel-header h3 {
  margin: 0;
  font-size: 24px;
}

.panel-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.table {
  margin-top: 8px;
}
</style>
