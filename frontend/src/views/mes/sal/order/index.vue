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
          <el-button v-if="['PENDING_ACCEPT','CONFIRMED'].includes(scope.row.status) && !hasWorkorder(scope.row)" link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:sal:order:edit']">改</el-button>
          <el-button v-if="scope.row.status==='CONFIRMED' || scope.row.status==='PRODUCING'" link type="warning" size="small" @click="handleToWorkorder(scope.row)" v-hasPermi="['mes:sal:order:workorder']">生成工单</el-button>
          <el-button v-if="scope.row.status==='SHIPPED'" link type="success" size="small" @click="handleClose(scope.row)" v-hasPermi="['mes:sal:order:edit']">结单</el-button>
          <el-button v-if="['PENDING_ACCEPT','CONFIRMED','PRODUCING'].includes(scope.row.status)" link type="danger" size="small" @click="handleCancel(scope.row)" v-hasPermi="['mes:sal:order:edit']">取消</el-button>
          <el-button v-if="['PENDING_ACCEPT','CONFIRMED'].includes(scope.row.status) && !hasWorkorder(scope.row)" link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:sal:order:remove']"></el-button>
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

    <!-- 新增/修改销售订单(头+明细行) -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8"><el-form-item label="销售订单号" prop="orderCode"><el-input v-model="form.orderCode" :disabled="optType==='edit'||autoGenFlag" placeholder="SO20260715001" /></el-form-item></el-col>
          <el-col :span="6" v-if="optType==='add'"><el-form-item><el-switch v-model="autoGenFlag" active-color="#13ce66" size="small" @change="handleAutoGenChange" /><span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span></el-form-item></el-col>
          <el-col :span="10"><el-form-item label="订单名称" prop="orderName"><el-input v-model="form.orderName" placeholder="订单名称" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="客户" prop="clientName">
              <el-input v-model="form.clientName" readonly placeholder="请选择客户"><template #append><el-button icon="Search" @click="handleSelectClient" /></template></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="客户PO号" prop="clientOrderCode"><el-input v-model="form.clientOrderCode" placeholder="客户PO号" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="业务员" prop="salesperson"><el-select v-model="form.salesperson" placeholder="请选择业务员" clearable filterable style="width:100%"><el-option v-for="u in userOptions" :key="u.userName" :label="u.nickName || u.userName" :value="u.nickName || u.userName" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="6"><el-form-item label="业务线" prop="businessLine"><el-select v-model="form.businessLine" placeholder="请选择" style="width:100%"><el-option label="内贸" value="DOMESTIC" /><el-option label="外贸" value="FOREIGN" /><el-option label="现货" value="SPOT" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="订单类型" prop="orderType"><el-select v-model="form.orderType" style="width:100%" @change="onHeadDimensionChange"><el-option v-for="d in salOrderTypeOptions" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="是否有样品" prop="sampleFlag"><el-switch v-model="form.sampleFlag" active-value="Y" inactive-value="N" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="付款方式" prop="paymentMethod"><el-select v-model="form.paymentMethod" placeholder="请选择" clearable style="width:100%"><el-option label="月结30天" value="月结30天" /><el-option label="月结60天" value="月结60天" /><el-option label="月结90天" value="月结90天" /><el-option label="现结" value="现结" /><el-option label="预付款" value="预付款" /><el-option label="货到付款" value="货到付款" /><el-option label="信用证" value="信用证" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="6"><el-form-item label="是否外发" prop="outsourceFlag"><el-switch v-model="form.outsourceFlag" active-value="Y" inactive-value="N" @change="onHeadDimensionChange" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="是否包装" prop="packageFlag"><el-switch v-model="form.packageFlag" active-value="Y" inactive-value="N" @change="onHeadDimensionChange" /></el-form-item></el-col>
        </el-row>
        <el-row>
          <el-col :span="8"><el-form-item label="订单日期" prop="orderDate"><el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="需求交期" prop="requestDate"><el-date-picker v-model="form.requestDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="订单总金额" prop="totalAmount"><el-input-number v-model="form.totalAmount" :min="0" :precision="2" style="width:100%" disabled /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <el-divider content-position="center">明细行</el-divider>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5"><el-button type="primary" plain size="small" @click="handleAddLine">添加行</el-button></el-col>
      </el-row>
      <el-table :data="lineList" size="small" v-loading="dimRecalcing">
        <el-table-column label="行号" align="center" prop="lineNo" width="60" />
        <el-table-column label="产品" align="center" prop="productName" :show-overflow-tooltip="true" />
        <el-table-column label="数量" align="center" prop="quantity" width="90" />
        <el-table-column label="单价" align="center" prop="unitPrice" width="90" />
        <el-table-column label="行金额" align="center" prop="lineAmount" width="100" />
        <el-table-column label="尺寸" align="center" prop="productSize" width="130" :show-overflow-tooltip="true" />
        <el-table-column label="工艺路线" align="center" prop="routeName" width="140" :show-overflow-tooltip="true">
          <template #default="scope">{{ scope.row.routeName || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="140">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="handleEditLine(scope.row)">改</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteLine(scope.$index)">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer><el-button type="primary" @click="submitForm">保 存</el-button><el-button @click="cancel">关 闭</el-button></template>
    </el-dialog>

    <ClientSelect ref="clientSelectRef" @onSelected="onClientSelected" />
    <LineEdit v-model="lineEditOpen" :line="editingLine" :order-type="form.orderType" :outsource-flag="form.outsourceFlag" :package-flag="form.packageFlag" @confirm="onLineConfirm" />
    <ToWorkorderDialog v-model="twOpen" :order="twOrder" @created="getList" />
  </div>
</template>

<script>
import { listOrder, getOrderDetail, createOrderWithLines, updateOrderWithLines, closeOrder, cancelOrder, acceptOrder, delOrder } from '@/api/mes/sal/order'
import { getDicts } from '@/api/system/dict/data'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import { resolveRouteProductBatch } from '@/api/mes/pro/routeproduct'
import { listRoute } from '@/api/mes/pro/proroute'
import { listUser } from '@/api/system/user'
import ClientSelect from '@/components/clientSelect/single.vue'
import LineEdit from './LineEdit.vue'
import ToWorkorderDialog from './ToWorkorderDialog.vue'

// 进度满格阈值（百分比）；CLOSED/CANCEL 由进度条 exception 态标识
const PROGRESS_FULL = 100

export default {
  name: 'SalOrder',
  components: { ClientSelect, LineEdit, ToWorkorderDialog },
  data() {
    return {
      loading: true, ids: [], selectedRows: [], single: true, multiple: true, showSearch: true, total: 0,
      orderList: [], title: '', open: false, optType: undefined, autoGenFlag: true,
      // 销售订单状态字典(mes_sal_order_status)
      salStatusOptions: [],
      // 销售订单类型字典(mes_sal_order_type)
      salOrderTypeOptions: [],
      // 业务员选项列表(SysUser,存展示用姓名 nickName,与客户/成品销售的 salesperson 口径一致)
      userOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, orderCode: null, orderName: null, clientName: null, clientOrderCode: null, businessLine: null, orderType: null, status: null, source: null },
      form: {}, lineList: [],
      lineEditOpen: false, editingLine: null,
      // 头维度批量重算: loading + 自增序号丢弃过期响应(连续切换/明细变动)
      dimRecalcing: false, headDimReqSeq: 0,
      twOpen: false, twOrder: {},
      rules: {
        orderCode: [{ required: true, message: '销售订单号不能为空', trigger: 'blur' }],
        orderName: [{ required: true, message: '订单名称不能为空', trigger: 'blur' }],
        clientName: [{ required: true, message: '请选择客户', trigger: 'change' }]
      }
    }
  },
  created() { this.loadUserOptions(); this.loadStatusDict(); this.loadOrderTypeDict(); this.getList() },
  methods: {
    /** 进度条展示模型：百分比兜底 0；CLOSED/CANCEL 走 exception，满进度走 success */
    progressBar(row) {
      const percentage = Number(row.progressPercent || 0)
      const terminated = row.status === 'CLOSED' || row.status === 'CANCEL'
      return { percentage, status: terminated ? 'exception' : percentage >= PROGRESS_FULL ? 'success' : '' }
    },
    /** 已派生未取消工单（含未开工）：改/删必须隐藏，后端闸门同样拦截 */
    hasWorkorder(row) { return Number(row.workorderCount || 0) > 0 },
    /** 业务员下拉数据源：按展示名去重(重复 value 会导致 el-select 的 filterable 过滤失效) */
    loadUserOptions() {
      listUser({ pageSize: 999 }).then(r => {
        const seen = new Set()
        this.userOptions = (r.rows || []).filter(u => {
          const name = u.nickName || u.userName
          if (!name || seen.has(name)) return false
          seen.add(name); return true
        })
      })
    },
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
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { orderId: null, orderCode: null, orderName: null, orderType: 'STANDARD', clientId: null, clientCode: null, clientName: null, clientOrderCode: null, salesperson: null, businessLine: null, sampleFlag: 'N', outsourceFlag: 'N', packageFlag: 'N', orderDate: null, requestDate: null, totalAmount: 0, paymentMethod: null, status: 'PENDING_ACCEPT', remark: null }
      this.optType = undefined; this.lineList = []; this.autoGenFlag = true; this.resetForm('form')
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryRef'); this.handleQuery() },
    handleSelectionChange(sel) { this.selectedRows = sel; this.ids = sel.map(i => i.orderId); this.single = sel.length !== 1; this.multiple = !sel.length },
    handleAutoGenChange(v) { if (v) { genSerialCode('ORDER_NO').then(r => { this.form.orderCode = r.data }) } else { this.form.orderCode = '' } },
    handleSelectClient() { this.$refs.clientSelectRef.open() },
    onClientSelected(row) { this.form.clientId = row.clientId; this.form.clientCode = row.clientCode; this.form.clientName = row.clientName; this.form.clientNick = row.clientNick; this.form.salesperson = row.salesperson || null; this.form.businessLine = row.clientType || null },
    handleAdd() { this.reset(); this.optType = 'add'; this.handleAutoGenChange(true); this.open = true; this.title = '新增销售订单' },
    /** 跳转只读详情页（展示状态/生产进度/明细） */
    handleView(row) { this.$router.push({ path: '/mes/sal/order_detail', query: { orderId: row.orderId } }) },
    handleUpdate(row) {
      this.reset(); this.optType = 'edit'; const id = row.orderId || this.ids
      getOrderDetail(id).then(r => { this.form = r.data; this.lineList = r.data.lines || []; this.autoGenFlag = false; this.open = true; this.title = '修改销售订单' })
    },
    onLineConfirm(line) {
      if (line.lineId) { const i = this.lineList.findIndex(x => x.lineId === line.lineId); if (i >= 0) this.lineList.splice(i, 1, line) }
      else { line.lineNo = this.lineList.length + 1; line.lineId = Date.now(); this.lineList.push(line) }
      this.recalcTotalAmount()
    },
    handleEditLine(row) { this.editingLine = { ...row }; this.lineEditOpen = true },
    /** 订单类型/外发/包装变更后, 按新头维度批量重算明细默认路线(命中才覆盖) */
    async onHeadDimensionChange() {
      if (!this.lineList.length) return
      try { await this.$modal.confirm('订单类型/标志已变更，是否按新条件重新匹配全部明细的工艺路线？') }
      catch { return }
      const seq = ++this.headDimReqSeq
      const itemIds = [...new Set(this.lineList.map(l => l.productId).filter(Boolean))]
      if (!itemIds.length) return
      // 行快照: 重算期间用户增删改行后, 过期结果整体丢弃
      const snapshot = this.lineList.map(l => l.lineId + ':' + l.productId).join('|')
      this.dimRecalcing = true
      try {
        const [batchRes, routeRes] = await Promise.all([
          resolveRouteProductBatch({ itemIds, orderType: this.form.orderType, outsourceFlag: this.form.outsourceFlag, packageFlag: this.form.packageFlag }),
          listRoute({ pageSize: 1000 })
        ])
        if (seq !== this.headDimReqSeq || snapshot !== this.lineList.map(l => l.lineId + ':' + l.productId).join('|')) return
        const map = batchRes.data || {}
        const routeMap = {}
        ;(routeRes.rows || []).forEach(rt => { routeMap[rt.routeId] = rt })
        const blocked = []
        this.lineList.forEach(l => {
          const m = map[l.productId]
          if (!m) return
          if (m.hardBlocked) { blocked.push(l.productName); return }
          if (m.matched) {
            l.routeProductId = m.routeProductId
            const rt = routeMap[m.routeId]
            l.routeCode = rt ? rt.routeCode : null
            l.routeName = rt ? (rt.routeName || rt.routeCode) : null
          }
        })
        if (blocked.length) this.$modal.msgError('以下产品无匹配的外发工艺路线：' + blocked.join('、'))
        else this.$modal.msgSuccess('已按新条件重新匹配工艺路线')
      } catch {
        if (seq === this.headDimReqSeq) this.$modal.msgError('工艺路线重算失败，请重试')
      } finally {
        if (seq === this.headDimReqSeq) this.dimRecalcing = false
      }
    },
    handleDeleteLine(idx) { this.lineList.splice(idx, 1); this.lineList.forEach((l, i) => { l.lineNo = i + 1 }); this.recalcTotalAmount() },
    recalcTotalAmount() { this.form.totalAmount = this.lineList.reduce((s, l) => s + (Number(l.lineAmount) || Number(l.unitPrice || 0) * Number(l.quantity || 0) || 0), 0) },
    handleAddLine() { this.editingLine = null; this.lineEditOpen = true },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (!this.lineList.length) { this.$modal.msgError('请至少添加一条明细行'); return }
        const payload = { order: { ...this.form }, lines: this.lineList.map(({ quantityProduced, quantityConvertible, ...rest }) => rest) }
        const fn = this.form.orderId ? updateOrderWithLines : createOrderWithLines
        fn(payload).then(() => { this.$modal.msgSuccess(this.form.orderId ? '修改成功' : '新增成功'); this.open = false; this.getList() })
      })
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
    /** 顶部「修改」：选中行均为待接单/已确认且均未派生工单（含未开工）才可用 */
    canEditSelected() { return this.selectedRows.length > 0 && this.selectedRows.every(r => ['PENDING_ACCEPT','CONFIRMED'].includes(r.status) && !this.hasWorkorder(r)) },
    /** 顶部「删除」：同修改口径，待接单/已确认且未派生工单 */
    canDeleteSelected() { return this.selectedRows.length > 0 && this.selectedRows.every(r => ['PENDING_ACCEPT','CONFIRMED'].includes(r.status) && !this.hasWorkorder(r)) }
  }
}
</script>
