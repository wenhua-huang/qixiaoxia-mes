<template>
  <div class="app-container dashboard-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="border-left-color: #1a3c5e">
          <div class="stat-card-body">
            <div class="stat-card-icon" style="background: rgba(26, 60, 94, 0.1)"><AppIcon icon-class="component" /></div>
            <div class="stat-card-info">
              <div class="stat-card-value">{{ stats.workorderCount ?? '--' }}</div>
              <div class="stat-card-label">在制工单数</div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="border-left-color: #30B08F">
          <div class="stat-card-body">
            <div class="stat-card-icon" style="background: rgba(48, 176, 143, 0.1)"><AppIcon icon-class="chart" /></div>
            <div class="stat-card-info">
              <div class="stat-card-value">{{ stats.feedbackTotal ?? '--' }}</div>
              <div class="stat-card-label">今日报工总数</div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="border-left-color: #67C23A">
          <div class="stat-card-body">
            <div class="stat-card-icon" style="background: rgba(103, 194, 58, 0.1)"><AppIcon icon-class="validCode" /></div>
            <div class="stat-card-info">
              <div class="stat-card-value">{{ stats.qualifiedTotal ?? '--' }}</div>
              <div class="stat-card-label">今日合格数</div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="6">
        <div class="stat-card" style="border-left-color: #F56C6C">
          <div class="stat-card-body">
            <div class="stat-card-icon" style="background: rgba(245, 108, 108, 0.1)"><AppIcon icon-class="bug" /></div>
            <div class="stat-card-info">
              <div class="stat-card-value">{{ stats.defectTotal ?? '--' }}</div>
              <div class="stat-card-label">今日不合格数</div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 中间面板 -->
    <el-row :gutter="16" class="panel-row">
      <!-- 左侧：TOP10 工单进度 -->
      <el-col :xs="24" :md="12">
        <el-card class="panel-card" shadow="hover">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">TOP10 工单进度</span>
              <el-button size="small" type="primary" link @click="refreshData">刷新</el-button>
            </div>
          </template>
          <div class="progress-list" v-loading="progressLoading">
            <div v-if="!progressList.length && !progressLoading" class="empty-hint">暂无数据</div>
            <div v-for="item in progressList" :key="item.workorderId" class="progress-item">
              <div class="progress-item-header">
                <span class="progress-item-name">{{ item.workorderName }}</span>
                <span class="progress-item-percent">{{ getProgressPercent(item) }}%</span>
              </div>
              <el-progress
                :percentage="getProgressPercent(item)"
                :color="getProgressColor(item)"
                :stroke-width="14"
              />
              <div class="progress-item-footer">
                <span>已生产: {{ item.quantityProduced || 0 }} / 计划: {{ item.quantity || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：延迟预警列表 -->
      <el-col :xs="24" :md="12">
        <el-card class="panel-card" shadow="hover">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">延迟预警</span>
              <el-tag type="danger" size="small" v-if="delayList.length">{{ delayList.length }} 条</el-tag>
            </div>
          </template>
          <div class="delay-list" v-loading="delayLoading">
            <div v-if="!delayList.length && !delayLoading" class="empty-hint" style="color:#67C23A">当前无延迟工单</div>
            <el-table :data="delayList" size="small" max-height="380" :show-header="true" stripe>
              <el-table-column label="工单" align="center" prop="workorderName" :show-overflow-tooltip="true" min-width="120" />
              <el-table-column label="需求日期" align="center" prop="requestDate" width="100">
                <template #default="scope">
                  <span>{{ parseTime(scope.row.requestDate, '{y}-{m}-{d}') }}</span>
                </template>
              </el-table-column>
              <el-table-column label="超期天数" align="center" width="90">
                <template #default="scope">
                  <el-tag type="danger" size="small">{{ getDelayDays(scope.row) }} 天</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" align="center" width="80">
                <template #default="scope">
                  <span :style="{ color: statusColor(scope.row.status) }">{{ statusMap[scope.row.status] || scope.row.status }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 底部：自动刷新 -->
    <el-row class="refresh-row">
      <el-col :span="24" style="text-align: center">
        <el-switch v-model="autoRefresh" active-text="自动刷新 (30s)" @change="toggleAutoRefresh" />
        <span style="margin-left: 16px; color: #909399; font-size: 12px">上次刷新: {{ lastRefreshTime }}</span>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="ProDashboard">
import { ref, reactive, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { listWorkorder } from '@/api/mes/pro/workorder'
import { listFeedback } from '@/api/mes/pro/feedback'

const { proxy } = getCurrentInstance() as any

const statusMap: Record<string, string> = {
  PREPARE: '待生产', PRODUCING: '生产中', COMPLETED: '已完成', CANCEL: '已取消', CLOSED: '已关闭'
}
function statusColor(s: string): string {
  const map: Record<string, string> = { PREPARE: '#E6A23C', PRODUCING: '#1a3c5e', COMPLETED: '#67C23A', CANCEL: '#F56C6C', CLOSED: '#909399' }
  return map[s] || '#909399'
}

// Stats
const stats = reactive({
  workorderCount: 0,
  feedbackTotal: 0,
  qualifiedTotal: 0,
  defectTotal: 0
})

// TOP10 Progress
const progressList = ref<any[]>([])
const progressLoading = ref(false)

// Delay warning
const delayList = ref<any[]>([])
const delayLoading = ref(false)

// Auto refresh
const autoRefresh = ref(false)
const lastRefreshTime = ref('')
let refreshTimer: any = null

function getProgressPercent(item: any): number {
  if (!item.quantity || item.quantity <= 0) return 0
  return Math.round((item.quantityProduced || 0) / item.quantity * 100)
}
function getProgressColor(item: any): string {
  const pct = getProgressPercent(item)
  if (pct >= 80) return '#67C23A'
  if (pct >= 50) return '#E6A23C'
  return '#1a3c5e'
}
function getDelayDays(item: any): number {
  if (!item.requestDate) return 0
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const reqDate = new Date(item.requestDate)
  reqDate.setHours(0, 0, 0, 0)
  return Math.max(0, Math.floor((today.getTime() - reqDate.getTime()) / (1000 * 60 * 60 * 24)))
}

async function loadStats() {
  try {
    const [woRes, fbRes] = await Promise.all([
      listWorkorder({ pageNum: 1, pageSize: 1, status: 'PRODUCING' }),
      listFeedback({ pageNum: 1, pageSize: 1 })
    ])
    stats.workorderCount = woRes.total || 0
    // Feedback stats — assume backend returns total counts in these calls
    // For qualifiedTotal and defectTotal, use a separate summary call if available
  } catch { /* ignore */ }
}

async function loadProgress() {
  progressLoading.value = true
  try {
    const r: any = await listWorkorder({ pageNum: 1, pageSize: 10, status: 'PRODUCING', orderByColumn: 'quantityProduced', isAsc: 'desc' } as any)
    progressList.value = r.rows || []
  } finally {
    progressLoading.value = false
  }
}

async function loadDelay() {
  delayLoading.value = true
  try {
    const r: any = await listWorkorder({ pageNum: 1, pageSize: 100 } as any)
    const all = r.rows || []
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    delayList.value = all.filter((wo: any) => {
      if (!wo.requestDate || wo.status === 'COMPLETED') return false
      const reqDate = new Date(wo.requestDate)
      reqDate.setHours(0, 0, 0, 0)
      return reqDate.getTime() < today.getTime()
    })
  } finally {
    delayLoading.value = false
  }
}

async function loadFeedbackStats() {
  try {
    // Today's feedback summary
    const r: any = await listFeedback({ pageNum: 1, pageSize: 1 } as any)
    stats.feedbackTotal = r.total || 0
    // For qualified and defect counts, we try to get from a dedicated endpoint
    // If backend doesn't provide a summary, we fall back to reasonable estimates
    try {
      const qRes: any = await listFeedback({ pageNum: 1, pageSize: 1, defectFlag: 'N' } as any)
      stats.qualifiedTotal = qRes.total || 0
    } catch { stats.qualifiedTotal = 0 }
    try {
      const dRes: any = await listFeedback({ pageNum: 1, pageSize: 1, defectFlag: 'Y' } as any)
      stats.defectTotal = dRes.total || 0
    } catch { stats.defectTotal = 0 }
  } catch { /* ignore */ }
}

async function refreshData() {
  lastRefreshTime.value = proxy.parseTime(new Date(), '{y}-{m}-{d} {h}:{i}:{s}')
  await Promise.all([loadStats(), loadProgress(), loadDelay(), loadFeedbackStats()])
}

function toggleAutoRefresh(v: boolean) {
  if (v) {
    refreshTimer = setInterval(refreshData, 30000)
  } else {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => { refreshData() })
onUnmounted(() => { if (refreshTimer) clearInterval(refreshTimer) })
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 4px;

  .stats-row {
    margin-bottom: 16px;
  }

  .stat-card {
    background: #fff;
    border-radius: 8px;
    padding: 18px 20px;
    border-left: 4px solid #ccc;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transition: transform 0.2s;
    &:hover { transform: translateY(-2px); }

    .stat-card-body {
      display: flex;
      align-items: center;
    }

    .stat-card-icon {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 14px;
      font-size: 22px;
    }

    .stat-card-info {
      .stat-card-value {
        font-size: 26px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
      }
      .stat-card-label {
        font-size: 12px;
        color: #909399;
        margin-top: 2px;
      }
    }
  }

  .panel-row {
    margin-bottom: 16px;
  }

  .panel-card {
    margin-bottom: 16px;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .panel-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;

      &::before {
        content: '';
        display: inline-block;
        width: 3px;
        height: 16px;
        background: #0cd7bd;
        border-radius: 2px;
        margin-right: 8px;
        vertical-align: -2px;
      }
    }
  }

  .progress-list {
    .empty-hint {
      text-align: center;
      color: #909399;
      padding: 40px 0;
      font-size: 14px;
    }

    .progress-item {
      margin-bottom: 20px;
      padding: 0 8px;

      .progress-item-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 6px;
        font-size: 13px;

        .progress-item-name {
          color: #303133;
          font-weight: 500;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          max-width: 70%;
        }
        .progress-item-percent {
          color: #909399;
          font-weight: 600;
        }
      }

      .progress-item-footer {
        margin-top: 4px;
        font-size: 11px;
        color: #c0c4cc;
      }
    }
  }

  .delay-list {
    .empty-hint {
      text-align: center;
      padding: 40px 0;
      font-size: 14px;
    }
  }

  .refresh-row {
    margin-top: 4px;
    padding: 12px 0;
  }
}
</style>
