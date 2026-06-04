<template>
  <div class="train-list-view">
    <el-card class="panel" shadow="never">
      <template #header>
        <div class="header-content">
          <div>
            <h3>车次总览</h3>
            <p>展示当前系统中全部车次及其线路结构。</p>
          </div>
          <el-tag type="info" effect="dark">全量列表</el-tag>
        </div>
      </template>

      <div class="toolbar">
        <el-button type="primary" @click="loadTrains" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-input v-model="searchText" placeholder="搜索车次 ID 或站点" class="search" clearable>
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <el-table :data="paginatedTrains" class="table" stripe v-loading="loading">
        <el-table-column prop="trainId" label="车次 ID" width="120" sortable />
        <el-table-column prop="seatNum" label="座位数" width="100" sortable />
        <el-table-column label="首发时间" width="100">
          <template #default="{ row }">{{ formatStartTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="站点路线" min-width="280">
          <template #default="{ row }">
            <div class="route-timeline-compact">
              <span
                v-for="(station, index) in row.stations"
                :key="index"
                class="route-node"
              >
                <span class="route-station" :class="{ 'is-first': index === 0, 'is-last': index === row.stations.length - 1 }">
                  {{ station }}
                </span>
                <span v-if="index < row.stations.length - 1" class="route-arrow">
                  <span class="route-duration" v-if="row.durations && row.durations[index]">{{ formatDuration(row.durations[index]) }}</span>
                  <span class="route-price" v-if="row.prices && row.prices[index]">¥{{ row.prices[index] }}</span>
                  →
                </span>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetails(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="filteredTrains.length === 0 && !loading" description="暂无车次信息" />

      <div v-if="filteredTrains.length > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          background
          layout="total, sizes, prev, pager, next"
          :total="filteredTrains.length"
          :page-sizes="[5, 10, 20, 50]"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="车次详情" width="640px">
      <div v-if="selectedTrain" class="train-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="车次 ID">{{ selectedTrain.trainId }}</el-descriptions-item>
          <el-descriptions-item label="座位数">{{ selectedTrain.seatNum }}</el-descriptions-item>
          <el-descriptions-item label="首发时间">{{ formatStartTime(selectedTrain.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="站点数">{{ selectedTrain.stations ? selectedTrain.stations.length : 0 }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-timeline">
          <div
            v-for="(station, index) in selectedTrain.stations"
            :key="index"
            class="timeline-row"
          >
            <div class="timeline-dot" :class="{ 'dot-start': index === 0, 'dot-end': index === selectedTrain.stations.length - 1 }"></div>
            <div class="timeline-content">
              <div class="timeline-station">{{ station }}</div>
              <div v-if="index < selectedTrain.stations.length - 1" class="timeline-segment">
                <span class="segment-info">
                  <el-tag size="small" type="info">耗时 {{ formatDuration(selectedTrain.durations?.[index]) }}</el-tag>
                  <el-tag size="small" type="success">¥{{ selectedTrain.prices?.[index] || '-' }}</el-tag>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, watch } from 'vue'
import http from '../utils/http'
import { formatStartTime, formatDuration } from '../utils'
import { ElMessage } from 'element-plus'

const trains = ref([])
const loading = ref(false)
const searchText = ref('')
const detailDialogVisible = ref(false)
const selectedTrain = ref(null)
const pagination = reactive({ page: 1, pageSize: 10 })

const filteredTrains = computed(() => {
  if (!searchText.value) return trains.value
  const text = searchText.value.toLowerCase()
  return trains.value.filter(train => {
    const trainIdMatch = train.trainId && train.trainId.toLowerCase().includes(text)
    const stationsMatch = train.stations && train.stations.some(station => station.toLowerCase().includes(text))
    return trainIdMatch || stationsMatch
  })
})

const paginatedTrains = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  return filteredTrains.value.slice(start, start + pagination.pageSize)
})

watch(() => searchText.value, () => { pagination.page = 1 })
watch(() => pagination.pageSize, () => { pagination.page = 1 })

const loadTrains = async () => {
  loading.value = true
  try {
    const response = await http.get('/api/train/list')
    if (response.data.code === 200) {
      trains.value = response.data.data || []
    } else {
      ElMessage.error(response.data.message || '加载失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const viewDetails = (train) => {
  selectedTrain.value = train
  detailDialogVisible.value = true
}

onMounted(() => {
  loadTrains()
})
</script>

<style scoped>
.train-list-view {
  width: 100%;
}

.panel {
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-content h3 {
  margin: 0;
  font-size: 24px;
}

.header-content p {
  margin: 8px 0 0;
  color: #64748b;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.search {
  width: 320px;
}

.table {
  margin-top: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 路线紧凑展示 */
.route-timeline-compact {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  line-height: 1.4;
}

.route-node {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.route-station {
  display: inline-flex;
  align-items: center;
  font-weight: 600;
  color: #334155;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  line-height: 1.4;
}

.route-station.is-first {
  background: #dcfce7;
  color: #166534;
}

.route-station.is-last {
  background: #fee2e2;
  color: #991b1b;
}

.route-arrow {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.route-duration {
  color: #64748b;
  font-weight: 500;
}

.route-price {
  color: #16a34a;
  font-weight: 600;
}

/* 详情弹窗时间轴 */
.train-details {
  padding: 10px 0;
}

.detail-timeline {
  margin-top: 20px;
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
}

.segment-info {
  display: flex;
  gap: 8px;
}
</style>
