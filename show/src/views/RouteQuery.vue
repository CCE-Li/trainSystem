<template>
  <el-card class="panel" shadow="never">
    <template #header>
      <div class="panel-header">
        <div>
          <h3>路线查询</h3>
          <p>支持连通性、全部路径和最优路径查询。</p>
        </div>
      </div>
    </template>
    <el-form :model="queryForm" label-position="top" class="query-grid">
      <el-form-item label="起点站">
        <el-select v-model="queryForm.departureStation" placeholder="请选择起点站" filterable>
          <el-option
            v-for="station in stations"
            :key="station"
            :label="station"
            :value="station"
          />
        </el-select>
      </el-form-item>
      <div class="swap-wrap">
        <el-button circle @click="swapStations" aria-label="交换站点">
          <el-icon><Switch /></el-icon>
        </el-button>
      </div>
      <el-form-item label="终点站">
        <el-select v-model="queryForm.arrivalStation" placeholder="请选择终点站" filterable>
          <el-option
            v-for="station in stations"
            :key="station"
            :label="station"
            :value="station"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="查询类型">
        <el-radio-group v-model="queryForm.type">
          <el-radio label="accessibility">连通性查询</el-radio>
          <el-radio label="all">全部路径</el-radio>
          <el-radio label="best">最优路径</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="queryForm.type === 'best'" label="优先策略">
        <el-radio-group v-model="queryForm.preference">
          <el-radio label="time">时间优先</el-radio>
          <el-radio label="price">价格优先</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <el-button type="primary" @click="handleQuery" :loading="loading">查询</el-button>

    <!-- 连通性结果 -->
    <div v-if="queryForm.type === 'accessibility' && accessibilityResult !== null" class="result-container">
      <div class="accessibility-result" :class="accessibilityResult ? 'accessible' : 'inaccessible'">
        <el-icon size="48">
          <CircleCheckFilled v-if="accessibilityResult" />
          <CircleCloseFilled v-else />
        </el-icon>
        <h4>{{ accessibilityResult ? '两站之间可以连通' : '两站之间无法连通' }}</h4>
        <p>{{ accessibilityResult ? '存在可达路线，您可以继续查询具体路径。' : '当前网络中不存在连接这两站的路线。' }}</p>
      </div>
    </div>

    <!-- 路径列表结果 -->
    <div v-if="(queryForm.type === 'all' || queryForm.type === 'best') && routeResults.length > 0" class="result-container">
      <h4 class="result-title">{{ queryForm.type === 'best' ? '最优路径' : '全部路径' }}</h4>
      <div class="route-list">
        <div v-for="(route, index) in routeResults" :key="index" class="route-card">
          <div class="route-index">路线 {{ index + 1 }}</div>
          <div class="route-timeline">
            <div
              v-for="(station, sIndex) in route.stations || route"
              :key="sIndex"
              class="timeline-node"
              :class="{ 'is-start': sIndex === 0, 'is-end': sIndex === (route.stations || route).length - 1 }"
            >
              <div class="node-dot"></div>
              <div class="node-label">{{ station }}</div>
            </div>
          </div>
          <div v-if="route.totalPrice || route.totalTime" class="route-meta">
            <el-tag v-if="route.totalTime" type="info" size="small">耗时 {{ formatDuration(route.totalTime) }}</el-tag>
            <el-tag v-if="route.totalPrice" type="success" size="small">票价 ¥{{ route.totalPrice }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 文本结果回退 -->
    <div v-if="rawResultText" class="result-container">
      <el-card shadow="never" class="result-card">
        <template #header>
          <h4>查询结果</h4>
        </template>
        <pre class="result-text">{{ rawResultText }}</pre>
      </el-card>
    </div>

    <el-empty v-if="noResult && !loading" description="未找到路线" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import http from '../utils/http'
import { formatDuration } from '../utils'
import { ElMessage } from 'element-plus'
import { Switch } from '@element-plus/icons-vue'

const queryForm = reactive({
  departureStation: '',
  arrivalStation: '',
  type: 'accessibility',
  preference: 'time'
})

const stations = ref([])
const loading = ref(false)
const accessibilityResult = ref(null)
const routeResults = ref([])
const rawResultText = ref(null)
const noResult = ref(false)

const swapStations = () => {
  const temp = queryForm.departureStation
  queryForm.departureStation = queryForm.arrivalStation
  queryForm.arrivalStation = temp
}

const loadStations = async () => {
  try {
    const response = await http.get('/api/route/stations')
    if (response.data.code === 200) {
      stations.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('加载站点列表失败')
  }
}

const parseRouteData = (data) => {
  if (!data) return []
  if (Array.isArray(data)) {
    return data.map(item => {
      if (Array.isArray(item)) return { stations: item }
      if (item.stations) return item
      if (item.route) return { stations: Array.isArray(item.route) ? item.route : [item.route] }
      return { stations: [item] }
    })
  }
  if (data.stations) return [data]
  if (data.route) return [{ stations: Array.isArray(data.route) ? data.route : [data.route] }]
  if (typeof data === 'string') return []
  return [{ stations: [data] }]
}

const handleQuery = async () => {
  if (!queryForm.departureStation || !queryForm.arrivalStation) {
    ElMessage.warning('请选择起点和终点')
    return
  }

  loading.value = true
  accessibilityResult.value = null
  routeResults.value = []
  rawResultText.value = null
  noResult.value = false

  try {
    if (queryForm.type === 'accessibility') {
      const response = await http.post('/api/route/accessibility', {
        departureStation: queryForm.departureStation,
        arrivalStation: queryForm.arrivalStation
      })
      if (response.data.code === 200) {
        accessibilityResult.value = response.data.data
      } else {
        ElMessage.error(response.data.message || '查询失败')
      }
    } else if (queryForm.type === 'all') {
      const response = await http.post('/api/route/findAll', {
        departureStation: queryForm.departureStation,
        arrivalStation: queryForm.arrivalStation
      })
      if (response.data.code === 200) {
        const data = response.data.data
        if (!data) {
          noResult.value = true
        } else if (typeof data === 'string') {
          rawResultText.value = data
        } else {
          routeResults.value = parseRouteData(data)
          if (routeResults.value.length === 0) rawResultText.value = typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data)
        }
      } else {
        ElMessage.error(response.data.message || '查询失败')
      }
    } else if (queryForm.type === 'best') {
      const response = await http.post('/api/route/best', {
        departureStation: queryForm.departureStation,
        arrivalStation: queryForm.arrivalStation,
        preference: queryForm.preference
      })
      if (response.data.code === 200) {
        const data = response.data.data
        if (!data) {
          noResult.value = true
        } else if (typeof data === 'string') {
          rawResultText.value = data
        } else {
          routeResults.value = parseRouteData(data)
          if (routeResults.value.length === 0) rawResultText.value = typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data)
        }
      } else {
        ElMessage.error(response.data.message || '查询失败')
      }
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStations()
})
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

.query-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 0 16px;
  align-items: end;
}

.query-grid .el-form-item:nth-child(4),
.query-grid .el-form-item:nth-child(5) {
  grid-column: 1 / -1;
}

.swap-wrap {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

.result-container {
  margin-top: 24px;
}

.result-title {
  margin: 0 0 16px;
  font-size: 18px;
  color: #1e293b;
}

.accessibility-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 24px;
  border-radius: 20px;
  text-align: center;
}

.accessibility-result.accessible {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  color: #065f46;
}

.accessibility-result.inaccessible {
  background: linear-gradient(135deg, #fef2f2, #fecaca);
  color: #991b1b;
}

.accessibility-result h4 {
  margin: 0;
  font-size: 20px;
}

.accessibility-result p {
  margin: 4px 0 0;
  opacity: 0.8;
  font-size: 14px;
}

.route-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.route-card {
  padding: 20px 24px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  transition: box-shadow 0.2s;
}

.route-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
}

.route-index {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.route-timeline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
}

.timeline-node {
  display: flex;
  align-items: center;
  gap: 0;
}

.timeline-node .node-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}

.timeline-node.is-start .node-dot {
  background: #22c55e;
  width: 16px;
  height: 16px;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.2);
}

.timeline-node.is-end .node-dot {
  background: #ef4444;
  width: 16px;
  height: 16px;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.2);
}

.timeline-node .node-label {
  padding: 0 12px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
}

.timeline-node:not(:last-child)::after {
  content: '';
  display: inline-block;
  width: 28px;
  height: 2px;
  background: #cbd5e1;
  margin-right: 4px;
}

.route-meta {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.result-card {
  border-radius: 20px;
  background: #f8fafc;
}

.result-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', monospace;
  line-height: 1.6;
  margin: 0;
}
</style>
