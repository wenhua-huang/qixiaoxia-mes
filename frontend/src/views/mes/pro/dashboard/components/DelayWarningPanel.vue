<template>
  <el-card class="panel-card delay-warning-panel" shadow="hover">
    <template #header>
      <div class="panel-header">
        <span class="panel-title">延期预警</span>
        <el-select
          v-model="riskLevel"
          size="small"
          placeholder="全部等级"
          clearable
          style="width: 120px"
          @change="onRiskChange"
        >
          <el-option label="临期" value="WARNING" />
          <el-option label="延期" value="DELAY" />
        </el-select>
      </div>
    </template>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="工单延期" name="WORKORDER">
        <el-table
          :data="rows"
          size="small"
          v-loading="loading"
          max-height="380"
          stripe
          style="cursor: pointer"
          @row-click="openRow"
        >
          <el-table-column label="工单" min-width="160">
            <template #default="{ row }">
              <div class="cell-main">{{ row.objectCode }}</div>
              <div class="cell-sub" :title="row.objectName">{{ row.objectName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="产品" prop="productName" min-width="120" :show-overflow-tooltip="true" />
          <el-table-column label="交期" width="100" align="center">
            <template #default="{ row }">{{ fmtDate(row.requestDate) }}</template>
          </el-table-column>
          <el-table-column label="逾期天数" width="88" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.overdueDays > 0 ? 'danger' : 'warning'">
                {{ row.overdueDays ?? 0 }} 天
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="110" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.progressPercent ?? 0" :stroke-width="10" />
            </template>
          </el-table-column>
          <el-table-column label="风险" width="88" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="delayType(row.delayLevel)">{{ delayText(row.delayLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <dict-tag :options="statusOptions" :value="row.status" />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="工序任务延期" name="TASK">
        <el-table
          :data="rows"
          size="small"
          v-loading="loading"
          max-height="380"
          stripe
          style="cursor: pointer"
          @row-click="openRow"
        >
          <el-table-column label="任务" min-width="160">
            <template #default="{ row }">
              <div class="cell-main">{{ row.objectCode }}</div>
              <div class="cell-sub" :title="row.objectName">{{ row.objectName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="工作站" prop="workstationName" width="110" :show-overflow-tooltip="true">
            <template #default="{ row }">{{ row.workstationName || '—' }}</template>
          </el-table-column>
          <el-table-column label="车间/班组" width="120">
            <template #default="{ row }">
              <div class="cell-sub">{{ row.workshopName || '—' }}</div>
              <div class="cell-sub">{{ row.teamName || '—' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="计划结束" width="120" align="center">
            <template #default="{ row }">{{ fmtDateTime(row.planTime) }}</template>
          </el-table-column>
          <el-table-column label="逾期天数" width="88" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.overdueDays > 0 ? 'danger' : 'warning'">
                {{ row.overdueDays ?? 0 }} 天
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="110" align="center">
            <template #default="{ row }">
              <el-progress :percentage="row.progressPercent ?? 0" :stroke-width="10" />
            </template>
          </el-table-column>
          <el-table-column label="风险" width="88" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="delayType(row.delayLevel)">{{ delayText(row.delayLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <dict-tag :options="statusOptions" :value="row.status" />
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <div class="pager">
      <el-pagination
        small
        layout="prev, pager, next, total"
        :total="total"
        :page-size="pageSize"
        :current-page="pageNum"
        @current-change="onPageChange"
      />
    </div>

    <WorkorderProgressDialog v-model="dialogOpen" :workorder-id="dialogWorkorderId" />
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, getCurrentInstance } from 'vue'
import { listDelay } from '@/api/mes/pro/progress'
import { delayText, delayType, WORKORDER_STATUS_OPTIONS } from '@/utils/mes/progress'
import { parseTime } from '@/utils/ruoyi'
import WorkorderProgressDialog from '@/views/mes/pro/workorder/components/WorkorderProgressDialog.vue'

const { proxy } = getCurrentInstance() as any
const { mes_pro_task_status } = proxy.useDict('mes_pro_task_status')

const activeTab = ref<'WORKORDER' | 'TASK'>('WORKORDER')
// 工单状态暂无字典，用集中维护的选项；任务状态走 mes_pro_task_status 字典
const statusOptions = computed(() =>
  activeTab.value === 'TASK' ? mes_pro_task_status.value : WORKORDER_STATUS_OPTIONS
)
const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const riskLevel = ref<string>('')
const pageNum = ref(1)
const pageSize = 10
const dialogOpen = ref(false)
const dialogWorkorderId = ref<number | null>(null)

function fmtDate(t: any): string {
  return (t && parseTime(t, '{y}-{m}-{d}')) || '—'
}
function fmtDateTime(t: any): string {
  return (t && parseTime(t, '{y}-{m}-{d} {h}:{i}')) || '—'
}

async function load() {
  loading.value = true
  try {
    const res: any = await listDelay({
      objectType: activeTab.value,
      riskLevel: riskLevel.value || undefined,
      pageNum: pageNum.value,
      pageSize
    })
    rows.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  pageNum.value = 1
  load()
}
function onRiskChange() {
  pageNum.value = 1
  load()
}
function onPageChange(p: number) {
  pageNum.value = p
  load()
}
function openRow(row: any) {
  if (row.workorderId) {
    dialogWorkorderId.value = row.workorderId
    dialogOpen.value = true
  }
}

defineExpose({ refresh: load })
onMounted(load)
</script>

<style scoped lang="scss">
.delay-warning-panel {
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

  .cell-main {
    font-weight: 500;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .cell-sub {
    font-size: 12px;
    color: #909399;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .pager {
    display: flex;
    justify-content: flex-end;
    margin-top: 10px;
  }
}
</style>
