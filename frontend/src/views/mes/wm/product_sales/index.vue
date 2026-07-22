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
      <el-table-column label="操作" align="center" width="240" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="查看" placement="top"><el-button link type="primary" icon="View" @click="handleView(scope.row)" /></el-tooltip>
          <el-tooltip content="过账出库" placement="top" v-if="scope.row.status==='DRAFT' || scope.row.status==='PARTIAL_POSTED'"><el-button link type="warning" icon="Upload" @click="handlePost(scope.row)" v-hasPermi="['mes:wm:sales:post']" /></el-tooltip>
          <el-tooltip content="发货" placement="top" v-if="scope.row.status==='POSTED' || scope.row.status==='PARTIAL_POSTED'"><el-button link type="success" icon="Van" @click="handleShip(scope.row)" v-hasPermi="['mes:wm:sales:ship']" /></el-tooltip>
          <el-tooltip content="关闭" placement="top" v-if="scope.row.status==='SHIPPED'"><el-button link type="primary" icon="CircleClose" @click="handleCloseRow(scope.row)" v-hasPermi="['mes:wm:sales:close']" /></el-tooltip>
          <el-tooltip content="修改" placement="top" v-if="scope.row.status==='DRAFT'"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:wm:sales:edit']" /></el-tooltip>
          <el-tooltip content="作废" placement="top" v-if="!isTerminal(scope.row.status) && scope.row.status!=='POSTED' && scope.row.status!=='SHIPPED'"><el-button link type="danger" icon="Close" @click="handleCancelRow(scope.row)" v-hasPermi="['mes:wm:sales:cancel']" /></el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize" @pagination="getList" />

    <!-- 表单弹窗（新增/编辑/查看） -->
    <SalesFormDialog ref="formDialogRef" @success="getList" />
    <!-- 过账出库弹窗 -->
    <SalesOutDialog ref="outDialogRef" @success="getList" />
    <!-- 销售订单选择 -->
    <SaleOrderSelect ref="orderSelectRef" @onSelected="onOrderSelected" />
    <!-- 发货弹窗 -->
    <el-dialog title="发货登记" v-model="shipOpen" width="600px" append-to-body>
      <el-form :model="shipForm" label-width="100px">
        <el-row>
          <el-col :span="24"><el-form-item label="物流公司"><el-input v-model="shipForm.logisticsCompany" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><el-form-item label="物流单号"><el-input v-model="shipForm.trackingNo" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="物流费用"><el-input-number v-model="shipForm.logisticsFee" :min="0" :precision="2" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="24"><el-form-item label="收货地址"><el-input v-model="shipForm.shippingAddress" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="12"><el-form-item label="收货人"><el-input v-model="shipForm.receiverName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="shipForm.receiverTel" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="confirmShip">确认发货</el-button>
        <el-button @click="shipOpen=false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="WmProductSales">
import { ref, reactive, getCurrentInstance } from 'vue'
import type { WmProductSalesQueryParams, WmProductSales } from '@/types/api/mes/wm/product_sales'
import { listWmProductSales, getSalesDetail, delWmProductSales, shipOut, closeSales, cancelSales, buildFromSaleOrder } from '@/api/mes/wm/product_sales'
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
const shipOpen = ref(false)
const shipTargetId = ref<number | null>(null)
const shipForm = reactive<Partial<WmProductSales>>({})

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

// 过账出库
function handlePost(row: WmProductSales) { outDialogRef.value?.open(row) }

// 发货
function handleShip(row: WmProductSales) {
  shipTargetId.value = row.salesId
  Object.keys(shipForm).forEach(k => delete (shipForm as any)[k])
  // 预填客户地址
  if (row.shippingAddress) shipForm.shippingAddress = row.shippingAddress
  if (row.receiverName) shipForm.receiverName = row.receiverName
  if (row.receiverTel) shipForm.receiverTel = row.receiverTel
  shipOpen.value = true
}
function confirmShip() {
  if (!shipTargetId.value) return
  shipOut(shipTargetId.value, shipForm).then(() => {
    proxy.$modal.msgSuccess('发货成功')
    shipOpen.value = false
    getList()
  })
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

getList()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
