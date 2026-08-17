<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="出库单编码" prop="salesCode">
        <el-input v-model="queryParams.salesCode" placeholder="请输入" clearable style="width:170px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户" prop="clientName">
        <el-input v-model="queryParams.clientName" placeholder="请输入" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户订单号" prop="clientOrderCode">
        <el-input v-model="queryParams.clientOrderCode" placeholder="PO号" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:130px">
          <el-option v-for="d in sales_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Link" @click="handleFromSaleOrder" v-hasPermi="['mes:wm:sales:add']">从销售订单</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:wm:sales:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:wm:sales:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="出库单编码" align="center" prop="salesCode" width="150" />
      <el-table-column label="出库单名称" align="center" prop="salesName" :show-overflow-tooltip="true" min-width="140" />
      <el-table-column label="客户名称" align="center" prop="clientName" :show-overflow-tooltip="true" min-width="130" />
      <el-table-column label="客户订单号" align="center" prop="clientOrderCode" width="120" />
      <el-table-column label="出库日期" align="center" width="110">
        <template #default="scope">{{ parseTime(scope.row.salesDate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="应出/已出" align="center" width="110">
        <template #default="scope"><span>{{ scope.row.totalQuantity || 0 }} / {{ scope.row.postedQuantity || 0 }}</span></template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope"><dict-tag :options="sales_status" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="检验状态" align="center" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.qcStatus && scope.row.qcStatus !== 'NONE'" size="small" :type="qcTagType(scope.row.qcStatus)"
            style="cursor: pointer" @click="goOqc(scope.row)">{{ qcTagText(scope.row.qcStatus) }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="查看" placement="top"><el-button link type="primary" icon="View" @click="handleView(scope.row)" /></el-tooltip>
          <el-tooltip content="出库确认" placement="top" v-if="scope.row.status==='DRAFT' || scope.row.status==='PARTIAL_POSTED'"><el-button link type="warning" icon="Upload" @click="handlePost(scope.row)" v-hasPermi="['mes:wm:sales:post']" /></el-tooltip>
          <el-tooltip content="发运登记" placement="top" v-if="scope.row.status==='POSTED' || scope.row.status==='PARTIAL_POSTED' || scope.row.status==='SHIPPED'"><el-button link type="success" icon="Van" @click="handleShip(scope.row)" v-hasPermi="['mes:wm:sales:ship']" /></el-tooltip>
          <el-tooltip content="关闭" placement="top" v-if="scope.row.status==='SHIPPED'"><el-button link type="primary" icon="CircleClose" @click="handleCloseRow(scope.row)" v-hasPermi="['mes:wm:sales:close']" /></el-tooltip>
          <el-tooltip content="修改" placement="top" v-if="scope.row.status==='DRAFT'"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:sales:edit']" /></el-tooltip>
          <el-tooltip content="作废" placement="top" v-if="!isTerminal(scope.row.status) && scope.row.status!=='POSTED' && scope.row.status!=='SHIPPED'"><el-button link type="danger" icon="Close" @click="handleCancelRow(scope.row)" v-hasPermi="['mes:wm:sales:cancel']" /></el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 表单弹窗（新增/编辑/查看） -->
    <SalesFormDialog ref="formDialogRef" @success="getList" />
    <!-- 出库确认弹窗 -->
    <SalesOutDialog ref="outDialogRef" @success="getList" />
    <!-- 销售订单选择 -->
    <SaleOrderSelect ref="orderSelectRef" @onSelected="onOrderSelected" />
  </div>
</template>

<script setup lang="ts" name="WmProductSales">
import { ref, reactive, getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import type { WmProductSalesQueryParams, WmProductSales } from '@/types/api/mes/wm/product_sales'
import { listWmProductSales, getSalesDetail, delWmProductSales, closeSales, cancelSales, buildFromSaleOrder } from '@/api/mes/wm/product_sales'
import SalesFormDialog from './components/SalesFormDialog.vue'
import SalesOutDialog from './components/SalesOutDialog.vue'
import SaleOrderSelect from './components/SaleOrderSelect.vue'

const { proxy } = getCurrentInstance() as any
const { mes_wm_sales_status: sales_status } = proxy.useDict('mes_wm_sales_status')

const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const multiple = ref(true)
const total = ref(0)
const dataList = ref<WmProductSales[]>([])
const queryParams = reactive<WmProductSalesQueryParams>({ pageNum: 1, pageSize: 10 } as WmProductSalesQueryParams)

const formDialogRef = ref()
const outDialogRef = ref()
const orderSelectRef = ref()
const router = useRouter()

const TERMINAL = ['CLOSED', 'CANCELED']
const isTerminal = (s: string) => TERMINAL.includes(s)

function getList() {
  loading.value = true
  listWmProductSales(queryParams).then(r => {
    dataList.value = r.rows
    total.value = r.total
    loading.value = false
  }).catch(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  Object.keys(queryParams).forEach(k => { if (k !== 'pageNum' && k !== 'pageSize') (queryParams as any)[k] = undefined })
  handleQuery()
}
function handleSelectionChange(s: WmProductSales[]) {
  ids.value = s.map(i => i.salesId); multiple.value = !s.length
}

function handleUpdate(row: WmProductSales) {
  getSalesDetail(row.salesId).then(r => { if (r.data) formDialogRef.value?.openEdit(r.data, r.data.lines) })
}
function handleView(row: WmProductSales) {
  getSalesDetail(row.salesId).then(r => { if (r.data) formDialogRef.value?.openView(r.data, r.data.lines) })
}
function handleDelete(row?: WmProductSales) {
  const _ids = row?.salesId ? [row.salesId] : ids.value
  proxy.$modal.confirm('确认删除选中的出库单？仅草稿状态可删。').then(() => delWmProductSales(_ids))
    .then(() => { getList(); proxy.$modal.msgSuccess('删除成功') }).catch(() => {})
}
function handleExport() { proxy.download('/mes/wm/product_sales/export', { ...queryParams }, `product_sales_${Date.now()}.xlsx`) }

// 出库确认
function handlePost(row: WmProductSales) { outDialogRef.value?.open(row) }

// 发运登记：跳转独立发运工作台
function handleShip(row: WmProductSales) {
  router.push({ path: '/mes/wm/product_sales_ship', query: { salesId: row.salesId } })
}

// 关闭
function handleCloseRow(row: WmProductSales) {
  proxy.$modal.confirm(`关闭出库单【${row.salesCode}】？关闭后不可再操作。`).then(() => closeSales(row.salesId))
    .then(() => { getList(); proxy.$modal.msgSuccess('已关闭') }).catch(() => {})
}

// 作废
function handleCancelRow(row: WmProductSales) {
  const msg = row.status === 'PARTIAL_POSTED'
    ? `作废出库单【${row.salesCode}】？已出库的部分库存将回滚恢复。`
    : `作废出库单【${row.salesCode}】？`
  proxy.$modal.confirm(msg).then(() => cancelSales(row.salesId))
    .then(() => { getList(); proxy.$modal.msgSuccess('已作废') }).catch(() => {})
}

// 从销售订单生成
function handleFromSaleOrder() { orderSelectRef.value?.open() }
function onOrderSelected(row: any) {
  buildFromSaleOrder(row.orderId).then((r: any) => {
    if (!r.data) return
    const draft = r.data
    formDialogRef.value?.openAdd({
      salesOrderId: draft.salesOrderId,
      salesOrderCode: draft.salesOrderCode,
      clientId: draft.clientId, clientCode: draft.clientCode, clientName: draft.clientName,
      clientOrderCode: draft.clientOrderCode, salesperson: draft.salesperson,
      lines: draft.lines
    } as WmProductSales)
  })
}

// 检验状态 tag：点击跳转 OQC 列表页并按来源单据过滤
const QC_TAG: Record<string, { type: string; text: string }> = {
  PASSED: { type: 'success', text: '检验合格' },
  CONCESSION: { type: 'warning', text: '让步接收' },
  PENDING: { type: 'info', text: '待检验' },
  FAILED: { type: 'danger', text: '检验不合格' }
}
function qcTagType(s: string) { return QC_TAG[s]?.type || 'info' }
function qcTagText(s: string) { return QC_TAG[s]?.text || s }
function goOqc(row: WmProductSales) {
  router.push({ path: '/qc/qcoqc', query: { sourceDocId: String(row.salesId) } })
}

getList()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
