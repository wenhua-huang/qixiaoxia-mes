<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="订单号" prop="orderCode"><el-input v-model="queryParams.orderCode" placeholder="销售订单号" clearable @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="订单名称" prop="orderName"><el-input v-model="queryParams.orderName" placeholder="订单名称" clearable @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="客户" prop="clientName"><el-input v-model="queryParams.clientName" placeholder="客户名称" clearable @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="客户PO号" prop="clientOrderCode"><el-input v-model="queryParams.clientOrderCode" placeholder="客户PO号" clearable @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="业务线" prop="businessLine">
        <el-select v-model="queryParams.businessLine" placeholder="全部" clearable style="width:110px">
          <el-option label="内贸" value="DOMESTIC" /><el-option label="外贸" value="FOREIGN" /><el-option label="现货" value="SPOT" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单类型" prop="orderType">
        <el-select v-model="queryParams.orderType" placeholder="全部" clearable style="width:110px">
          <el-option v-for="d in salOrderTypeOptions" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width:110px">
          <el-option v-for="d in salStatusOptions" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-select v-model="queryParams.source" placeholder="全部" clearable style="width:110px">
          <el-option label="直接新增" :value="1" /><el-option label="CRM系统" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" size="small" @click="handleQuery">搜索</el-button><el-button size="small" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['mes:sal:order:add']">新增</el-button></el-col>
      <el-col :span="1.5"><el-button type="success" plain size="small" :disabled="single || !canEditSelected" @click="handleUpdate" v-hasPermi="['mes:sal:order:edit']">修改</el-button></el-col>
      <el-col :span="1.5"><el-button type="danger" plain size="small" :disabled="multiple || !canDeleteSelected" @click="handleDelete" v-hasPermi="['mes:sal:order:remove']">删除</el-button></el-col>
      <el-col :span="1.5"><el-button type="warning" plain size="small" @click="handleExport" v-hasPermi="['mes:sal:order:export']">导出</el-button></el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="销售订单号" align="center" prop="orderCode" width="140">
        <template #default="s"><el-link type="primary" @click="handleView(s.row)">{{ s.row.orderCode }}</el-link></template>
      </el-table-column>
      <el-table-column label="订单名称" align="center" prop="orderName" :show-overflow-tooltip="true" />
      <el-table-column label="订单类型" align="center" prop="orderType" width="90"><template #default="s"><el-tag :type="orderTypeMeta(s.row.orderType).type" size="small">{{ orderTypeMeta(s.row.orderType).text }}</el-tag></template></el-table-column>
      <el-table-column label="客户" align="center" prop="clientName" :show-overflow-tooltip="true" />
      <el-table-column label="客户PO号" align="center" prop="clientOrderCode" width="120" />
      <el-table-column label="业务线" align="center" prop="businessLine" width="80"><template #default="s">{{ businessLineText(s.row.businessLine) }}</template></el-table-column>
      <el-table-column label="来源" align="center" prop="source" width="90"><template #default="s"><el-tag :type="sourceTag(s.row.source)">{{ sourceText(s.row.source) }}</el-tag></template></el-table-column>
      <el-table-column label="交期" align="center" prop="requestDate" width="110"><template #default="s">{{ parseTime(s.row.requestDate, '{y}-{m}-{d}') }}</template></el-table-column>
      <el-table-column label="总金额" align="center" prop="totalAmount" width="100" />
      <el-table-column label="生产进度" align="center" width="150">
        <template #default="s">
          <el-progress :percentage="progressBar(s.row).percentage" :stroke-width="10" :status="progressBar(s.row).status" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90"><template #default="s"><el-tag :type="statusMeta(s.row.status).type">{{ statusMeta(s.row.status).text }}</el-tag></template></el-table-column>
      <el-table-column label="操作" align="center" width="360" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看</el-button>
          <el-button v-if="scope.row.status==='PENDING_ACCEPT'" link type="success" size="small" @click="handleAccept(scope.row)" v-hasPermi="['mes:sal:order:edit']">接单</el-button>
          <el-button v-if="canModify(scope.row)" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:sal:order:edit']">改</el-button>
          <el-button v-if="scope.row.status==='CONFIRMED' || scope.row.status==='PRODUCING'" link type="warning" size="small" @click="handleToWorkorder(scope.row)" v-hasPermi="['mes:sal:order:workorder']">生成工单</el-button>
          <el-button v-if="scope.row.status==='SHIPPED'" link type="success" size="small" @click="handleClose(scope.row)" v-hasPermi="['mes:sal:order:edit']">结单</el-button>
          <el-button v-if="['PENDING_ACCEPT','CONFIRMED','PRODUCING'].includes(scope.row.status)" link type="danger" size="small" @click="handleCancel(scope.row)" v-hasPermi="['mes:sal:order:edit']">取消</el-button>
          <el-button v-if="canModify(scope.row)" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:sal:order:remove']"></el-button>
          <el-dropdown @command="(cmd) => handleRowExport(scope.row, cmd)" v-hasPermi="['mes:sal:order:exportDetail']">
            <el-button link type="primary" size="small">导出<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pdf">导出 PDF</el-dropdown-item>
                <el-dropdown-item command="excel">导出 Excel</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <OrderEditDialog ref="editDialogRef" :order-type-options="salOrderTypeOptions" @success="getList" />
    <ToWorkorderDialog v-model="twOpen" :order="twOrder" @created="getList" />
  </div>
</template>

<script>
import { listOrder, closeOrder, cancelOrder, acceptOrder, delOrder } from '@/api/mes/sal/order'
import { getDicts } from '@/api/system/dict/data'
import OrderEditDialog from './OrderEditDialog.vue'
import ToWorkorderDialog from './ToWorkorderDialog.vue'

// 进度满格阈值（百分比）；CLOSED/CANCEL 由进度条 exception 态标识
const PROGRESS_FULL = 100

export default {
  name: 'SalOrder',
  components: { OrderEditDialog, ToWorkorderDialog },
  data() {
    return {
      loading: true, ids: [], selectedRows: [], single: true, multiple: true, showSearch: true, total: 0,
      orderList: [],
      // 销售订单状态字典(mes_sal_order_status)
      salStatusOptions: [],
      // 销售订单类型字典(mes_sal_order_type)
      salOrderTypeOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, orderCode: null, orderName: null, clientName: null, clientOrderCode: null, businessLine: null, orderType: null, status: null, source: null },
      twOpen: false, twOrder: {}
    }
  },
  created() { this.loadStatusDict(); this.loadOrderTypeDict(); this.getList() },
  methods: {
    /** 进度条展示模型：百分比兜底 0；CLOSED/CANCEL 走 exception，满进度走 success */
    progressBar(row) {
      const percentage = Number(row.progressPercent || 0)
      const terminated = row.status === 'CLOSED' || row.status === 'CANCEL'
      return { percentage, status: terminated ? 'exception' : percentage >= PROGRESS_FULL ? 'success' : '' }
    },
    /** 已派生未取消工单（含未开工）：改/删必须隐藏，后端闸门同样拦截 */
    hasWorkorder(row) { return Number(row.workorderCount || 0) > 0 },
    /** 可改/可删：待接单/已确认（与后端 SalOrderStatus.isEditable 同口径）且未派生工单 */
    canModify(row) { return ['PENDING_ACCEPT', 'CONFIRMED'].includes(row.status) && !this.hasWorkorder(row) },
    getList() { this.loading = true; listOrder({ ...this.queryParams, includeProgress: true }).then(r => { this.orderList = r.rows; this.total = r.total; this.loading = false }).catch(() => { this.loading = false }) },
    /** 加载销售订单状态字典 */
    loadStatusDict() { getDicts('mes_sal_order_status').then(r => { this.salStatusOptions = r.data || [] }) },
    /** 加载销售订单类型字典 */
    loadOrderTypeDict() { getDicts('mes_sal_order_type').then(r => { this.salOrderTypeOptions = r.data || [] }) },
    /** 状态 → {text, type}，由字典驱动渲染 */
    statusMeta(s) {
      const d = this.salStatusOptions.find(o => o.dictValue === s)
      return { text: d ? d.dictLabel : (s || ''), type: d && d.listClass ? d.listClass : '' }
    },
    /** 订单类型 → {text, type}，由字典驱动渲染 */
    orderTypeMeta(t) {
      const d = this.salOrderTypeOptions.find(o => o.dictValue === t)
      return { text: d ? d.dictLabel : (t || ''), type: d && d.listClass ? d.listClass : '' }
    },
    businessLineText(b) { return { DOMESTIC: '内贸', FOREIGN: '外贸', SPOT: '现货' }[b] || b },
    sourceText(s) { return { 1: '直接新增', 2: 'CRM系统' }[s] || (s == null ? '' : s) },
    sourceTag(s) { return s === 2 ? 'warning' : 'info' },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryRef'); this.handleQuery() },
    handleSelectionChange(sel) { this.selectedRows = sel; this.ids = sel.map(i => i.orderId); this.single = sel.length !== 1; this.multiple = !sel.length },
    handleAdd() { this.$refs.editDialogRef.openAdd() },
    /** 跳转只读详情页（展示状态/生产进度/明细） */
    handleView(row) { this.$router.push({ path: '/mes/sal/order_detail', query: { orderId: row.orderId } }) },
    handleUpdate(row) {
      const id = row.orderId || this.ids
      if (id) this.$refs.editDialogRef.openEdit(id)
    },
    handleAccept(row) { this.$modal.confirm('确认接单 "' + row.orderCode + '"？接单后进入已确认状态，可转工单生产。').then(() => acceptOrder(row.orderId)).then(() => { this.getList(); this.$modal.msgSuccess('接单成功') }).catch(() => {}) },
    handleClose(row) { this.$modal.confirm('确认结单 "' + row.orderCode + '"？结单后不可恢复。').then(() => closeOrder(row.orderId)).then(() => { this.getList(); this.$modal.msgSuccess('结单成功') }).catch(() => {}) },
    handleCancel(row) { this.$modal.confirm('确认取消 "' + row.orderCode + '" ?').then(() => cancelOrder(row.orderId)).then(() => { this.getList(); this.$modal.msgSuccess('取消成功，关联工单需另行处理') }).catch(() => {}) },
    handleDelete(row) { const ids = row.orderId || this.ids; this.$modal.confirm('是否确认删除销售订单 "' + ids + '" ?').then(() => delOrder(ids)).then(() => { this.getList(); this.$modal.msgSuccess('删除成功') }).catch(() => {}) },
    handleToWorkorder(row) { this.twOrder = row; this.twOpen = true },
    handleExport() { this.download('mes/sal/order/export', { ...this.queryParams }, `sal_order_${new Date().getTime()}.xlsx`) },
    handleRowExport(row, cmd) {
      const url = cmd === 'pdf' ? 'mes/sal/order/exportPdf/' + row.orderId : 'mes/sal/order/exportExcel/' + row.orderId
      const suffix = cmd === 'pdf' ? 'pdf' : 'xlsx'
      this.download(url, {}, `sal_order_${row.orderCode}.${suffix}`)
    }
  },
  computed: {
    /** 顶部「修改」：选中行均可改可删（待接单/已确认且未派生工单，含未开工）才可用 */
    canEditSelected() { return this.selectedRows.length > 0 && this.selectedRows.every(r => this.canModify(r)) },
    /** 顶部「删除」：同修改口径 */
    canDeleteSelected() { return this.selectedRows.length > 0 && this.selectedRows.every(r => this.canModify(r)) }
  }
}
</script>
