<template>
  <div class="app-container">
    <!-- 筛选栏 -->
    <el-form :inline="true" class="filter-form">
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />
      </el-form-item>
      <el-form-item label="车间">
        <el-select v-model="filterWorkshopIds" multiple collapse-tags clearable placeholder="车间" style="width: 200px">
          <el-option v-for="w in workshopOptions" :key="w.workshopId" :label="w.workshopName" :value="w.workshopId" />
        </el-select>
      </el-form-item>
      <el-form-item label="班组">
        <el-select v-model="filterTeamIds" multiple collapse-tags clearable placeholder="班组" style="width: 200px">
          <el-option v-for="t in teamOptions" :key="t.teamId" :label="t.teamName" :value="t.teamId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="onSearch">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- KPI 卡片 -->
    <kpi-grid :overview="overview" />

    <!-- 趋势 + 状态分布 -->
    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>生产趋势</span></template>
          <line-chart :x-data="trendX" :series="trendSeries" height="300px" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span>任务状态分布</span></template>
          <pie-chart :data="statusData" height="300px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 产能对比 -->
    <el-card shadow="hover" class="chart-card">
      <template #header><span>产能对比（按车间）</span></template>
      <bar-chart :x-data="prodX" :series="prodSeries" height="320px" />
    </el-card>

    <!-- 产能效率表 -->
    <el-card shadow="hover" class="table-card">
      <template #header><span>产能效率</span></template>
      <el-table :data="productivity" size="small" border stripe>
        <el-table-column label="分组" prop="groupName" min-width="120" />
        <el-table-column label="工单数" prop="workorderCount" width="80" align="center" />
        <el-table-column label="完成数" prop="completedCount" width="80" align="center" />
        <el-table-column label="延期数" prop="delayedCount" width="80" align="center" />
        <el-table-column label="完成率" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="rateType(row.completionRate, true)">{{ row.completionRate ?? 0 }}%</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="延期率" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="rateType(row.delayRate, false)">{{ row.delayRate ?? 0 }}%</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标准工时" prop="standardMinutes" width="100" align="center" />
        <el-table-column label="实际工时" prop="actualMinutes" width="100" align="center" />
        <el-table-column label="效率" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.efficiencyPercent != null" :type="rateType(row.efficiencyPercent, true)">{{ row.efficiencyPercent }}%</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="合格率" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="rateType(row.qualifiedRate, true)">{{ row.qualifiedRate ?? 0 }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 工单明细 -->
    <el-card shadow="hover" class="table-card">
      <template #header><span>工单明细</span></template>
      <el-table :data="detailList" size="small" border stripe v-loading="detailLoading">
        <el-table-column label="工单号" prop="workorderCode" min-width="140" />
        <el-table-column label="产品" prop="productName" min-width="140" show-overflow-tooltip />
        <el-table-column label="计划量" prop="quantity" width="90" align="center" />
        <el-table-column label="已产量" prop="quantityProduced" width="90" align="center" />
        <el-table-column label="完成率" width="90" align="center">
          <template #default="{ row }">{{ row.completionRate ?? 0 }}%</template>
        </el-table-column>
        <el-table-column label="交期" width="110" align="center">
          <template #default="{ row }">{{ fmtDate(row.requestDate) }}</template>
        </el-table-column>
        <el-table-column label="完工时间" width="160" align="center">
          <template #default="{ row }">{{ fmtDateTime(row.finishDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="workorderStatusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="延期等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="delayType(row.delayLevel)">{{ delayText(row.delayLevel) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="detailQuery.pageNum"
        v-model:limit="detailQuery.pageSize"
        @pagination="loadDetail"
      />
    </el-card>

    <!-- 延期预警 -->
    <el-card shadow="hover" class="table-card">
      <template #header><span>延期预警</span></template>
      <el-table :data="delayList" size="small" border stripe>
        <el-table-column label="单号" prop="objectCode" min-width="140" />
        <el-table-column label="名称" prop="objectName" min-width="140" show-overflow-tooltip />
        <el-table-column label="产品" prop="productName" min-width="140" show-overflow-tooltip />
        <el-table-column label="车间" prop="workshopName" width="120" />
        <el-table-column label="班组" prop="teamName" width="120" />
        <el-table-column label="计划/交期" width="110" align="center">
          <template #default="{ row }">{{ fmtDate(row.requestDate) }}</template>
        </el-table-column>
        <el-table-column label="逾期天数" prop="overdueDays" width="90" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.overdueDays > 0 ? '#f56c6c' : '#e6a23c', fontWeight: 600 }">
              {{ row.overdueDays ?? 0 }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="等级" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="delayType(row.delayLevel)">{{ delayText(row.delayLevel) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import BarChart from '@/components/Charts/BarChart.vue'
import LineChart from '@/components/Charts/LineChart.vue'
import PieChart from '@/components/Charts/PieChart.vue'
import KpiGrid from './components/KpiGrid.vue'
import { getOverview, getProductivity, getTrend, getTaskStatus, getReportDetail } from '@/api/mes/pro/report'
import { listDelay } from '@/api/mes/pro/progress'
import { listAllWorkshop } from '@/api/mes/md/workshop'
import { listTeam } from '@/api/mes/cal/team'
import { parseTime } from '@/utils/ruoyi'
import { statusText, workorderStatusType, delayType, delayText } from '@/utils/mes/progress'

const { proxy } = getCurrentInstance() as any

const dateRange = ref<[string, string] | []>(monthRange())
const filterWorkshopIds = ref<number[]>([])
const filterTeamIds = ref<number[]>([])
const workshopOptions = ref<any[]>([])
const teamOptions = ref<any[]>([])

const overview = ref<any>({})
const productivity = ref<any[]>([])
const detailList = ref<any[]>([])
const delayList = ref<any[]>([])
const total = ref(0)
const detailLoading = ref(false)
const trendX = ref<string[]>([])
const trendCreated = ref<number[]>([])
const trendCompleted = ref<number[]>([])
const statusData = ref<{ name: string; value: number }[]>([])
const prodX = ref<string[]>([])
const prodPlan = ref<number[]>([])
const prodActual = ref<number[]>([])
const detailQuery = reactive({ pageNum: 1, pageSize: 20 })

const trendSeries = computed(() => [
  { name: '新建', data: trendCreated.value },
  { name: '完工', data: trendCompleted.value }
])
const prodSeries = computed(() => [
  { name: '计划产出', data: prodPlan.value },
  { name: '实际产出', data: prodActual.value }
])

function monthRange(): [string, string] {
  const d = new Date()
  const first = new Date(d.getFullYear(), d.getMonth(), 1)
  return [parseTime(first, '{y}-{m}-{d}') as string, parseTime(d, '{y}-{m}-{d}') as string]
}
function rangeTimes() {
  const [b, e] = dateRange.value as [string, string]
  return { beginTime: b, endTime: e }
}
// tansParams 会把数组序列化成 workshopIds[0]=1 括号形式，Spring List<Long> 无法绑定，
// 这里统一拼成逗号分隔字符串（Spring 默认 StringToCollectionConverter 可拆分）。
function idListParam(ids: number[]) {
  return ids.length ? ids.join(',') : undefined
}
async function loadAll() {
  const t = rangeTimes()
  const workshopIds = idListParam(filterWorkshopIds.value)
  const teamIds = idListParam(filterTeamIds.value)
  const workshopId = filterWorkshopIds.value[0]
  const teamId = filterTeamIds.value[0]
  const [o, trResp, s, prod, d, dl]: any[] = await Promise.all([
    getOverview({ ...t, workshopIds, teamIds }),
    getTrend({ ...t, workshopId }),
    getTaskStatus({ ...t, workshopId }),
    getProductivity({ ...t, workshopIds, teamIds, groupBy: 'WORKSHOP' }),
    getReportDetail({ ...t, workshopId, teamId, pageNum: detailQuery.pageNum, pageSize: detailQuery.pageSize }),
    listDelay({ objectType: 'WORKORDER', pageNum: 1, pageSize: 10, workshopId, teamId })
  ])
  overview.value = o.data || {}
  const trend = trResp.data || []
  trendX.value = trend.map((x: any) => (x.statDate ? String(x.statDate).substring(0, 10) : ''))
  trendCreated.value = trend.map((x: any) => Number(x.createdCount) || 0)
  trendCompleted.value = trend.map((x: any) => Number(x.completedCount) || 0)
  statusData.value = (s.data || []).map((x: any) => ({ name: statusText(x.status), value: Number(x.count) || 0 }))
  productivity.value = prod.data || []
  prodX.value = productivity.value.map((x: any) => x.groupName || '')
  prodPlan.value = productivity.value.map((x: any) => Number(x.standardOutput) || 0)
  prodActual.value = productivity.value.map((x: any) => Number(x.actualOutput) || 0)
  detailList.value = d.rows || []
  total.value = d.total || 0
  delayList.value = dl.rows || []
}
function loadDetail() {
  detailLoading.value = true
  getReportDetail({
    ...rangeTimes(),
    workshopId: filterWorkshopIds.value[0],
    teamId: filterTeamIds.value[0],
    pageNum: detailQuery.pageNum,
    pageSize: detailQuery.pageSize
  })
    .then((r: any) => {
      detailList.value = r.rows || []
      total.value = r.total || 0
    })
    .finally(() => { detailLoading.value = false })
}
function onSearch() {
  detailQuery.pageNum = 1
  loadAll()
}
function resetQuery() {
  dateRange.value = monthRange()
  filterWorkshopIds.value = []
  filterTeamIds.value = []
  detailQuery.pageNum = 1
  loadAll()
}
function fmtDate(t: any) {
  return t ? proxy.parseTime(t, '{y}-{m}-{d}') : '—'
}
function fmtDateTime(t: any) {
  return t ? proxy.parseTime(t, '{y}-{m}-{d} {h}:{i}') : '—'
}
function rateType(v: any, highGood: boolean) {
  const n = Number(v) || 0
  if (highGood) return n >= 90 ? 'success' : n >= 60 ? 'warning' : 'danger'
  return n <= 5 ? 'success' : n <= 20 ? 'warning' : 'danger'
}

onMounted(async () => {
  const [w, t]: any[] = await Promise.all([listAllWorkshop(), listTeam({ pageNum: 1, pageSize: 999 })])
  workshopOptions.value = w.data || []
  teamOptions.value = t.rows || t.data || []
  loadAll()
})
</script>

<style lang="scss" scoped>
.filter-form { margin-bottom: 4px; }
.chart-card { margin-top: 12px; }
.table-card { margin-top: 12px; }
</style>
