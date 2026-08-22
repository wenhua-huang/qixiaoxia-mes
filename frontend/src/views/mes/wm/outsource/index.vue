<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="外协单号" prop="orderCode">
        <el-input v-model="queryParams.orderCode" placeholder="请输入外协单号" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="外协厂商" prop="vendorName">
        <el-input v-model="queryParams.vendorName" placeholder="请输入厂商名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="d in mes_outsource_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="d in mes_outsource_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Promotion" size="small" @click="handleCreate" v-hasPermi="['mes:wm:outsource:add']">外协发货</el-button>
      </el-col>
      <el-col :span="1.5">
          <el-button type="warning" plain icon="Van" size="small" :disabled="draftRows.length === 0" :loading="batchLoading" @click="handleBatchExecute" v-hasPermi="['mes:wm:outsource:execute']">
          批量执行发料<span v-if="draftRows.length > 0">（{{ draftRows.length }}）</span>
        </el-button>
      </el-col>
      <el-col :span="1.5">
          <el-button type="success" plain icon="Check" size="small" :disabled="receivableRows.length === 0" :loading="batchLoading" @click="handleBatchReceive" v-hasPermi="['mes:wm:outsource:receive']">
          批量收货<span v-if="receivableRows.length > 0">（{{ receivableRows.length }}）</span>
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 列表 -->
      <el-table ref="tableRef" v-loading="loading" :data="list" border row-key="orderId" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" :selectable="canSelectRow" />
      <el-table-column label="外协单号" align="center" prop="orderCode" width="160" />
      <el-table-column label="来源" align="center" prop="sourceType" width="80">
        <template #default="{ row }">
          <dict-tag :options="mes_outsource_type" :value="row.sourceType" />
        </template>
      </el-table-column>
      <el-table-column label="外协厂商" align="center" prop="vendorName" width="100" />
      <el-table-column label="工单编码" align="center" prop="workorderCode" width="140" />
      <el-table-column label="工序" align="center" prop="processName" width="100">
        <template #default="{ row }">{{ row.processName || '-' }}</template>
      </el-table-column>
      <el-table-column label="发料总量" align="center" prop="issueTotalQty" width="100">
        <template #default="{ row }">{{ row.issueTotalQty != null ? row.issueTotalQty + '吨' : '-' }}</template>
      </el-table-column>
      <el-table-column label="收货总量" align="center" prop="recptTotalQty" width="100">
        <template #default="{ row }">{{ row.recptTotalQty != null ? row.recptTotalQty + '吨' : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作人" align="center" prop="operator" width="80" />
      <el-table-column label="发料时间" align="center" prop="issueTime" width="160" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="{ row }">
          <dict-tag :options="mes_outsource_status" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="检验状态" align="center" width="100">
        <template #default="{ row }">
          <qc-status-tag :status="row.qcStatus" @click="goIqc(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="270" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="warning" size="small" :loading="actionLoadingId === row.orderId" :disabled="actionLoadingId != null" @click="handleExecute(row)" v-hasPermi="['mes:wm:outsource:execute']">执行发料</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="danger" size="small" :loading="actionLoadingId === row.orderId" :disabled="actionLoadingId != null" @click="handleDelete(row)" v-hasPermi="['mes:wm:outsource:remove']">删除</el-button>
          <el-button v-if="canReceive(row.status)" link type="success" size="small" :loading="actionLoadingId === row.orderId" :disabled="actionLoadingId != null" @click="handleReceive(row)" v-hasPermi="['mes:wm:outsource:receive']">收货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 外协发货弹窗 -->
    <OutsourceCreateDialog ref="createDialogRef" @success="getList" />

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="外协单详情" width="800px" append-to-body>
      <el-descriptions v-if="detail" :column="3" border size="small">
        <el-descriptions-item label="外协单号">{{ detail.orderCode }}</el-descriptions-item>
        <el-descriptions-item label="来源类型"><dict-tag :options="mes_outsource_type" :value="detail.sourceType" /></el-descriptions-item>
        <el-descriptions-item label="外协厂商">{{ detail.vendorName }}</el-descriptions-item>
        <el-descriptions-item label="工单编码">{{ detail.workorderCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ detail.processName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态"><dict-tag :options="mes_outsource_status" :value="detail.status" /></el-descriptions-item>
        <el-descriptions-item label="发料总量">{{ detail.issueTotalQty != null ? detail.issueTotalQty + '吨' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货总量">{{ detail.recptTotalQty ? detail.recptTotalQty + '吨' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="报工ID">{{ detail.feedbackId || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">发料明细</el-divider>
      <el-table :data="detail?.issueLines || []" border size="small">
        <el-table-column label="物料编码" prop="itemCode" width="120" />
        <el-table-column label="物料名称" prop="itemName" min-width="120" show-overflow-tooltip />
        <el-table-column label="数量" prop="quantity" width="90" />
        <el-table-column label="单位" prop="unitName" width="60" />
        <el-table-column label="批次" prop="batchCode" width="120" />
        <el-table-column label="仓库" prop="warehouseName" width="100" />
      </el-table>
      <template v-if="detail?.recptLines?.length > 0">
        <el-divider content-position="left">收货明细</el-divider>
        <el-table :data="detail.recptLines" border size="small">
          <el-table-column label="物料编码" prop="itemCode" width="120" />
          <el-table-column label="物料名称" prop="itemName" min-width="120" show-overflow-tooltip />
          <el-table-column label="数量" prop="quantity" width="90" />
          <el-table-column label="单位" prop="unitName" width="60" />
          <el-table-column label="仓库" prop="warehouseName" width="100" />
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { listOutsource, getOutsource, executeOutsource, batchExecuteOutsource, receiveOutsource, batchReceiveOutsource, delOutsource } from '@/api/mes/wm/outsource'
import OutsourceCreateDialog from './OutsourceCreateDialog.vue'
import QcStatusTag from '../../qc/components/QcStatusTag.vue'
import { showBatchResult } from './batchResult'

interface OutsourceOrderRow {
  orderId: number
  orderCode: string
  sourceType: string
  vendorName: string
  workorderCode: string
  processName: string
  issueTotalQty: number | null
  recptTotalQty: number | null
  operator: string
  issueTime: string
  status: string
  feedbackId?: number | null
  iqcId?: number | null
  iqcCode?: string | null
  qcStatus?: string | null
  issueLines?: unknown[]
  recptLines?: unknown[]
}

interface OutsourceQuery {
  pageNum: number
  pageSize: number
  orderCode: string
  vendorName: string
  status: string
  sourceType: string
}

const { proxy } = getCurrentInstance() as any
const { mes_outsource_status, mes_outsource_type } = proxy.useDict('mes_outsource_status', 'mes_outsource_type')
const queryFormRef = ref()
const tableRef = ref()
const loading = ref(false)
const batchLoading = ref(false)
const actionLoadingId = ref<number | null>(null)
const showSearch = ref(true)
const list = ref<OutsourceOrderRow[]>([])
const total = ref(0)
const createDialogRef = ref()
const detailVisible = ref(false)
const detail = ref<OutsourceOrderRow | null>(null)
const selectedRows = ref<OutsourceOrderRow[]>([])
const draftRows = computed(() => selectedRows.value.filter(r => r.status === 'DRAFT'))
const receivableRows = computed(() => selectedRows.value.filter(r => canReceive(r.status)))

const queryParams = reactive<OutsourceQuery>({
  pageNum: 1, pageSize: 10,
  orderCode: '', vendorName: '', status: '', sourceType: ''
})

function getList() {
  loading.value = true
  listOutsource(queryParams).then((res: { rows: OutsourceOrderRow[]; total: number }) => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).catch(() => {}).finally(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { queryFormRef.value?.resetFields(); handleQuery() }
function handleCreate() { createDialogRef.value?.open() }

async function handleDetail(row: OutsourceOrderRow) {
  try {
    const res = await getOutsource(row.orderId)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) { /* request interceptor already shown error */ }
}

// 返回 true=用户确认，false=取消
async function confirmAction(msg: string): Promise<boolean> {
  try { await proxy.$modal.confirm(msg); return true } catch { return false }
}

async function handleReceive(row: OutsourceOrderRow) {
  if (!await confirmAction('确认收货？收货后将入库并推进流转卡。')) return
  actionLoadingId.value = row.orderId
  try {
    await receiveOutsource(row.orderId)
    proxy.$modal.msgSuccess('收货成功')
    getList()
  } finally { actionLoadingId.value = null }
}

async function handleExecute(row: OutsourceOrderRow) {
  if (!await confirmAction('确认执行发料？将扣减库存并将单据置为「已发料」。')) return
  actionLoadingId.value = row.orderId
  try {
    await executeOutsource(row.orderId)
    proxy.$modal.msgSuccess('发料成功')
    getList()
  } finally { actionLoadingId.value = null }
}

// 删除草稿外协单（仅 DRAFT 状态，后端再次校验状态并拒绝非草稿）
async function handleDelete(row: OutsourceOrderRow) {
  if (!await confirmAction(`确认删除草稿单 ${row.orderCode}？删除后不可恢复。`)) return
  actionLoadingId.value = row.orderId
  try {
    await delOutsource(row.orderId)
    proxy.$modal.msgSuccess('删除成功')
    getList()
  } finally { actionLoadingId.value = null }
}

// 严格状态机：仅 SHIPPED（厂商已发货）可收货
function canReceive(status: string) {
  return status === 'SHIPPED'
}
function canSelectRow(row: OutsourceOrderRow) {
  return row.status === 'DRAFT' || canReceive(row.status)
}
function handleSelectionChange(rows: OutsourceOrderRow[]) {
  selectedRows.value = rows
}

// 来料检验状态 tag：点击跳转 IQC 列表并按外协单过滤
const router = useRouter()
function goIqc(row: OutsourceOrderRow) {
  router.push({ path: '/qc/qciqc', query: { sourceDocId: String(row.orderId), sourceDocType: 'wm_outsource_order' } })
}

// 批量操作结果汇总抽到 ./batchResult（全成功提示；失败弹窗列原因）

function clearTableSelection() {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

async function handleBatchExecute() {
  const ids = draftRows.value.map(r => r.orderId)
  if (ids.length === 0) return
  if (!await confirmAction(`确认对选中的 ${ids.length} 张草稿单执行发料？将扣减库存并将单据置为「已发料」。`)) return
  try {
    batchLoading.value = true
    await showBatchResult(batchExecuteOutsource(ids), '发料')
    clearTableSelection()
    getList()
  } finally {
    batchLoading.value = false
  }
}

async function handleBatchReceive() {
  const ids = receivableRows.value.map(r => r.orderId)
  if (ids.length === 0) return
  if (!await confirmAction(`确认对选中的 ${ids.length} 张外协单收货？将入库、建报工并推进流转卡。`)) return
  try {
    batchLoading.value = true
    await showBatchResult(batchReceiveOutsource(ids), '收货')
    clearTableSelection()
    getList()
  } finally {
    batchLoading.value = false
  }
}

onMounted(() => getList())
</script>
