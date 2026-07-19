<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="单据编号" prop="orderCode">
        <el-input
          v-model="queryParams.orderCode"
          placeholder="请输入采购订单编码(唯一)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="单据名称" prop="orderName">
        <el-input
          v-model="queryParams.orderName"
          placeholder="请输入采购订单名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商ID" prop="vendorId">
        <el-input
          v-model="queryParams.vendorId"
          placeholder="请输入供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商编码" prop="vendorCode">
        <el-input
          v-model="queryParams.vendorCode"
          placeholder="请输入供应商编码"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商名称" prop="vendorName">
        <el-input
          v-model="queryParams.vendorName"
          placeholder="请输入供应商名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="下单日期">
        <el-date-picker v-model="orderDateRange" type="daterange" range-separator="至"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:100%"
          @change="onOrderDateRangeChange" />
      </el-form-item>
      <el-form-item label="预计到货">
        <el-date-picker v-model="expectedDateRange" type="daterange" range-separator="至"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:100%"
          @change="onExpectedDateRangeChange" />
      </el-form-item>
      <el-form-item label="采购人" prop="purchaser">
        <el-select v-model="queryParams.purchaser" placeholder="采购人" clearable filterable style="width:140px">
          <el-option v-for="u in userOptions" :key="u.userName" :label="u.nickName || u.userName" :value="u.userName" />
        </el-select>
      </el-form-item>
      <el-form-item label="审批人" prop="approver">
        <el-input
          v-model="queryParams.approver"
          placeholder="请输入审批人"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>      <el-form-item label="关联客户订单" prop="sourceOrderCode">
        <el-input
          v-model="queryParams.sourceOrderCode"
          placeholder="请输入关联客户订单号(如PO#ORD66003MT)"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary"  size="small" @click="handleQuery">搜索</el-button>
        <el-button  size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          
          size="small"
          @click="handleAdd"
          v-hasPermi="['mes:pur:order:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          
          size="small"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['mes:pur:order:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          
          size="small"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['mes:pur:order:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          size="small"
          :disabled="single"
          @click="handleView()"
          v-hasPermi="['mes:pur:order:query']"
        >查询</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          
          size="small"
          @click="handleExport"
          v-hasPermi="['mes:pur:order:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
<el-table-column label="ID" align="center" prop="orderId" />      <el-table-column label="单据编号" align="center" prop="orderCode" />
	      <el-table-column label="单据名称" align="center" prop="orderName" />
      
      
      <el-table-column label="供应商名称" align="center" prop="vendorName" />
      <el-table-column label="采购类型" align="center" prop="purchaseType" width="100">
          <template #default="scope">
            <span><dict-tag :options="mes_purchase_type" :value="scope.row.purchaseType" /></span>
          </template>
        </el-table-column>
      <el-table-column label="下单日期" align="center" prop="orderDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.orderDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预计到货" align="center" prop="expectedDate" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.expectedDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="采购人" align="center" prop="purchaser">
        <template #default="scope">{{ nickOf(scope.row.purchaser) }}</template>
      </el-table-column>
      <el-table-column label="审批人" align="center" prop="approver">
        <template #default="scope">{{ nickOf(scope.row.approver) }}</template>
      </el-table-column>
      <el-table-column label="采购数量" align="center" width="120">
        <template #default="scope">
          <div>{{ num(scope.row.receivedQuantity) }} / {{ num(scope.row.totalQuantity) }}</div>
          <div v-if="gt0(scope.row.cancelledQuantity)" class="amt-sub cancel-text">已取消 {{ num(scope.row.cancelledQuantity) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="采购金额" align="center" width="130">
        <template #default="scope">
          <div>{{ money(scope.row.totalAmount) }}</div>
          <div v-if="gt0(scope.row.receivedAmount) || gt0(scope.row.cancelledAmount)" class="amt-sub">
            <span>已收 {{ money(scope.row.receivedAmount) }}</span>
            <span v-if="gt0(scope.row.cancelledAmount)" class="cancel-text"> · 已取消 {{ money(scope.row.cancelledAmount) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="币种" align="center" prop="currency" />
      <el-table-column label="关联客户订单" align="center" prop="sourceOrderCode" />
      <el-table-column label="状态" align="center" prop="status" width="110">
          <template #default="scope">
            <span><dict-tag :options="mes_order_status" :value="scope.row.status" /></span>
            <el-tooltip v-if="isOverdue(scope.row)" content="已超期未到货" placement="top">
              <span style="color:#f56c6c;margin-left:2px;cursor:help">⚠</span>
            </el-tooltip>
          </template>
        </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-tooltip content="查看" placement="top"><el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['mes:pur:order:query']"></el-button></el-tooltip>
          <el-tooltip content="修改" placement="top"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pur:order:edit']"></el-button></el-tooltip>
          <el-tooltip content="删除" placement="top"><el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:pur:order:remove']"></el-button></el-tooltip>
          <el-button v-if="scope.row.status === 'DRAFT'" link type="success" size="small" @click="handleApprove(scope.row)" v-hasPermi="['mes:pur:order:approve']">审批</el-button>
          <el-button v-if="scope.row.status === 'APPROVED'" link type="warning" size="small" @click="handleOrder(scope.row)" v-hasPermi="['mes:pur:order:order']">下单</el-button>
          <el-button v-if="['DRAFT','APPROVED','ORDERED'].includes(scope.row.status)" link type="danger" size="small" @click="handleCancel(scope.row)" v-hasPermi="['mes:pur:order:cancel']">取消</el-button>
          <el-button v-if="['RECEIVED','RECEIVING'].includes(scope.row.status)" link type="info" size="small" @click="handleClose(scope.row)" v-hasPermi="['mes:pur:order:close']">关闭</el-button>
          <el-button v-if="['RECEIVED','RECEIVING'].includes(scope.row.status)" link type="danger" size="small" @click="handleReturn(scope.row)" v-hasPermi="['mes:pur:order:return']">退货</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <pagination
      v-show="total>0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改采购订单头对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="8">
<el-form-item label="单据编号" prop="orderCode">
	              <el-input v-model="form.orderCode" placeholder="PO20260614001" :disabled="optType === 'edit' || optType === 'view' || autoGenFlag" />
            </el-form-item>
          </el-col>
          <el-col :span="6" v-if="optType === 'add'">
            <el-form-item>
              <el-switch v-model="autoGenFlag" active-color="#13ce66" size="small" @change="handleAutoGenChange" />
              <span style="margin-left:6px;font-size:12px;color:#13ce66">自动生成</span>
            </el-form-item>
          </el-col>
          <el-col :span="10">
<el-form-item label="单据名称" prop="orderName">
	              <el-input v-model="form.orderName" placeholder="单据名称" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="供应商" prop="vendorName">
              <el-input v-model="form.vendorName" readonly placeholder="请选择供应商">
                <template #append><el-button icon="Search" :disabled="optType === 'view'" @click="handleSelectVendor" /></template>
              </el-input>
            </el-form-item>
            <VendorSelect ref="vendorSelectRef" @onSelected="onVendorSelected" />
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购类型" prop="purchaseType">
              <el-select v-model="form.purchaseType" placeholder="请选择" style="width:100%" :disabled="optType === 'view'">
                <el-option v-for="d in mes_purchase_type" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="下单日期" prop="orderDate">
              <el-input v-model="form.orderDate" type="date" placeholder="yyyy-MM-dd" style="width:100%" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="预计到货" prop="expectedDate">
              <el-input v-model="form.expectedDate" type="date" placeholder="yyyy-MM-dd" style="width:100%" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="采购金额">
              <el-input v-model="form.totalAmount" readonly placeholder="自动统计" />
              <div v-if="form.orderId && (gt0(form.receivedAmount) || gt0(form.cancelledAmount))" class="amt-sub" style="margin-top:4px">
                <span>已收 {{ money(form.receivedAmount) }} 元</span>
                <span v-if="gt0(form.cancelledAmount)" class="cancel-text"> · 已取消 {{ money(form.cancelledAmount) }} 元</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择" style="width:100%" disabled>
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已审批" value="APPROVED" />
                <el-option label="已下单" value="ORDERED" />
              </el-select>
              <div style="font-size:11px;color:#909399;line-height:1.4;margin-top:2px">状态通过列表「审批/下单/取消/关闭」按钮推进</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="采购人" prop="purchaser">
              <el-select v-model="form.purchaser" placeholder="请选择采购人" clearable filterable style="width:100%" :disabled="optType === 'view'">
                <el-option v-for="u in userOptions" :key="u.userName" :label="u.nickName || u.userName" :value="u.userName" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="关联客户订单" prop="sourceOrderCode">
              <el-select v-model="form.sourceOrderCode" placeholder="请选择销售订单" clearable filterable style="width:100%" :disabled="optType === 'view'">
                <el-option v-for="so in salOrderOptions" :key="so.orderId" :label="so.orderCode + (so.orderName ? ' / ' + so.orderName : '')" :value="so.orderCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8" v-if="optType !== 'add'">
            <el-form-item label="审核人">
              <el-input :model-value="nickOf(form.approver)" readonly :placeholder="form.approver ? '' : '尚未审批'" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="16">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="备注" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template v-if="form.orderId">
        <el-divider content-position="center">采购订单行</el-divider>
        <PurOrderLine :orderId="form.orderId" :initLines="orderLines" ref="orderLineRef" :readOnly="optType === 'view'" @change="refreshOrderTotals" />
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="optType !== 'view'" type="primary" @click="submitForm">保存单据</el-button>
          <el-button @click="cancel">关 闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 取消采购订单弹窗 -->
    <el-dialog title="取消采购订单" v-model="cancelOpen" width="450px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="取消原因">
          <el-select v-model="cancelReason" placeholder="请选择取消原因" style="width:100%">
            <el-option v-for="d in mes_cancel_reason" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" @click="confirmCancel">确认取消</el-button>
        <el-button @click="cancelOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 强制关闭采购订单弹窗 -->
    <el-dialog title="强制关闭采购订单" v-model="forceCloseOpen" width="500px" append-to-body>
      <el-alert title="该订单存在未完全收货的行，强制关闭后将放弃未收部分，且不可恢复。" type="warning" show-icon :closable="false" style="margin-bottom:12px" />
      <el-form label-width="80px">
        <el-form-item label="关闭原因">
          <el-select v-model="forceCloseReason" placeholder="请选择关闭原因" style="width:100%">
            <el-option v-for="d in mes_cancel_reason" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" @click="confirmForceClose">确认关闭</el-button>
        <el-button @click="forceCloseOpen = false">取 消</el-button>
      </template>
    </el-dialog>

    <rt-vendor-wizard v-model="returnWizardOpen" :purOrder="returnWizardOrder" @success="onReturnWizardSuccess" />
  </div>
</template>

<script>
import { listOrder, getOrder, delOrder, addOrder, updateOrder, approveOrder, orderOrder, closeOrder, cancelOrder } from "@/api/mes/pur/order"
import { listAllOrder } from "@/api/mes/sal/order"
import { genSerialCode } from "@/api/mes/sys/autocoderule"
import { listUser } from "@/api/system/user"
import useUserStore from '@/store/modules/user'
import VendorSelect from "@/components/vendorSelect/single.vue"
import PurOrderLine from "./line.vue"
import RtVendorWizard from "@/components/rtVendorWizard/index.vue"

export default {
  name: "Order",
  setup() {
    const { mes_order_status, mes_purchase_type, mes_cancel_reason } = useDict("mes_order_status", "mes_purchase_type", "mes_cancel_reason")
    return { mes_order_status, mes_purchase_type, mes_cancel_reason }
  },
  components: { VendorSelect, PurOrderLine, RtVendorWizard },
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 采购订单头表格数据
      orderList: [],
      // 人员选项列表
      userOptions: [],
      // 销售订单下拉选项
      salOrderOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,        orderCode: null,
        orderName: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        purchaseType: null,
        status: null,
        currency: null,
        
        approver: null,
        totalQuantity: null,
        totalAmount: null,
        sourceOrderCode: null,
      },
      // 表单参数
      form: {},
      // 自动生成开关
      autoGenFlag: true,
      orderDateRange: null,
      expectedDateRange: null,
      optType: undefined,
      orderLines: [],
      // 取消弹窗
      cancelOpen: false,
      cancelTargetId: null,
      cancelReason: null,
      // 强制关闭弹窗
      forceCloseOpen: false,
      forceCloseTargetId: null,
      forceCloseReason: null,
      // 退货向导
      returnWizardOpen: false,
      returnWizardOrder: null,
      // 表单校验
      rules: {        orderCode: [
        ],
        vendorName: [
          { required: true, message: "供应商不能为空", trigger: "change" }
        ],
      }
    }
  },
  created() {
    this.loadUserOptions()
    this.loadSalOrderOptions()
    this.getList()
  },
  methods: {
    /** 加载人员选项列表 */
    loadUserOptions() {
      listUser({ pageSize: 999 }).then(response => {
        this.userOptions = response.rows || []
      })
    },
    /** 加载销售订单下拉选项（已确认的销售订单） */
    loadSalOrderOptions() {
      listAllOrder().then(response => {
        this.salOrderOptions = response.data || []
      })
    },
    /** 查询采购订单头列表 */
    getList() {
      this.loading = true
      listOrder(this.queryParams).then(response => {
        this.orderList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        orderId: null,        orderCode: null,
        orderName: null,
        vendorId: null,
        vendorCode: null,
        vendorName: null,
        purchaseType: null,
        status: "DRAFT",
        currency: "CNY",
        orderDate: new Date().toISOString().slice(0,10),
        expectedDate: new Date().toISOString().slice(0,10),
        purchaser: null,
        approver: null,
        totalQuantity: null,
        totalAmount: null,
        sourceOrderCode: null,
        remark: null,
        createBy: null,
        createTime: null,
        updateBy: null,
        updateTime: null
      }
      this.optType = undefined
      this.orderLines = []
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    onOrderDateRangeChange(val) {
      this.queryParams.params = this.queryParams.params || {}
      this.queryParams.params.beginOrderDate = val?.[0] || null
      this.queryParams.params.endOrderDate = val?.[1] || null
    },
    onExpectedDateRangeChange(val) {
      this.queryParams.params = this.queryParams.params || {}
      this.queryParams.params.beginExpectedDate = val?.[0] || null
      this.queryParams.params.endExpectedDate = val?.[1] || null
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.orderId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAutoGenChange(val) {
      if (val) {
        genSerialCode('PUR_ORDER_CODE').then(r => {
          this.form.orderCode = r.data
        })
      } else {
        this.form.orderCode = ''
      }
    },
    handleSelectVendor() { this.$refs.vendorSelectRef.open() },
    onVendorSelected(row) {
      this.form.vendorId = row.vendorId
      this.form.vendorName = row.vendorName
      this.form.vendorCode = row.vendorCode
    },
    handleAdd() {
      this.reset()
      this.optType = 'add'
      this.autoGenFlag = true
      // 确保日期为字符串（防止 data()/el-date-picker 序列化异常）
      const today = new Date()
      const y = today.getFullYear()
      const m = String(today.getMonth() + 1).padStart(2, '0')
      const d = String(today.getDate()).padStart(2, '0')
      this.form.orderDate = y + '-' + m + '-' + d
      // 采购人默认当前登录用户
      this.form.purchaser = useUserStore().name
      // 触发编码预览
      this.handleAutoGenChange(true)
      this.open = true
      this.title = "添加采购订单头"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      this.optType = 'edit'
      const orderId = row.orderId || this.ids
      getOrder(orderId).then(response => {
        this.form = response.data.order
        this.orderLines = response.data.lines || []
        this.open = true
        this.title = "修改采购订单头"
      })
    },
    /** 查看按钮操作（只读） */
    handleView(row) {
      this.reset()
      this.optType = 'view'
      // row 可能是 undefined（工具栏勾选调用）；这时用 ids[0]
      const orderId = (row && row.orderId) || (Array.isArray(this.ids) ? this.ids[0] : this.ids)
      getOrder(orderId).then(response => {
        this.form = response.data.order
        this.orderLines = response.data.lines || []
        this.open = true
        this.title = "查看采购订单头"
      })
    },
    /** 根据 userName 返回 nickName（找不到则回落 userName） */
    nickOf(userName) {
      if (!userName) return ''
      const u = this.userOptions.find(x => x.userName === userName)
      return (u && u.nickName) || userName
    },
    /** 订单行变动后刷新订单头汇总（总金额/总数量） */
    refreshOrderTotals() {
      if (this.form.orderId) {
        getOrder(this.form.orderId).then(response => {
          this.form.totalAmount = response.data.order.totalAmount
          this.form.totalQuantity = response.data.order.totalQuantity
        })
      }
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.orderId != null) {
            updateOrder(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addOrder(this.form).then(response => {
              this.$modal.msgSuccess("新增成功，请添加订单行")
              this.form.orderId = response.data.orderId || response.orderId
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const orderIds = row.orderId || this.ids
      this.$modal.confirm('是否确认删除采购订单头编号为"' + orderIds + '"的数据项？').then(function() {
        return delOrder(orderIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 审批操作 */
    handleApprove(row) {
      this.$modal.confirm('确认审批采购订单 "' + row.orderCode + '" 吗？').then(() => {
        return approveOrder(row.orderId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("审批成功")
      }).catch(() => {})
    },
    /** 下单操作 */
    handleOrder(row) {
      this.$modal.confirm('确认下达采购订单 "' + row.orderCode + '" 给供应商吗？').then(() => {
        return orderOrder(row.orderId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("下单成功")
      }).catch(() => {})
    },
    /** 关闭操作 */
    handleClose(row) {
      if (row.status === 'RECEIVING') {
        // 强制关闭：需要填写关闭原因
        this.forceCloseTargetId = row.orderId
        this.forceCloseReason = null
        this.forceCloseOpen = true
      } else {
        // 正常关闭：简单确认
        this.$modal.confirm('确认关闭采购订单 "' + row.orderCode + '" 吗？关闭后不可恢复。').then(() => {
          return closeOrder(row.orderId, null)
        }).then(() => {
          this.getList()
          this.$modal.msgSuccess("关闭成功")
        }).catch(() => {})
      }
    },
    /** 确认强制关闭 */
    confirmForceClose() {
      if (!this.forceCloseReason) {
        this.$modal.msgWarning("请选择关闭原因")
        return
      }
      closeOrder(this.forceCloseTargetId, this.forceCloseReason).then(() => {
        this.forceCloseOpen = false
        this.getList()
        this.$modal.msgSuccess("关闭成功")
      })
    },
    /** 退货操作：打开退货向导并预填当前 PO */
    handleReturn(row) {
      this.returnWizardOrder = { orderId: row.orderId, orderCode: row.orderCode, vendorName: row.vendorName }
      this.returnWizardOpen = true
    },
    /** 退货向导成功回调 */
    onReturnWizardSuccess() {
      this.getList()
    },
    /** 取消操作 */
    handleCancel(row) {
      this.cancelTargetId = row.orderId
      this.cancelReason = null
      this.cancelOpen = true
    },
    /** 确认取消 */
    confirmCancel() {
      cancelOrder(this.cancelTargetId, this.cancelReason).then(() => {
        this.cancelOpen = false
        this.getList()
        this.$modal.msgSuccess("取消成功")
      })
    },
    /** 判断是否超期未到货 */
    isOverdue(row) {
      if (row.status !== 'ORDERED' && row.status !== 'RECEIVING') return false
      if (!row.expectedDate) return false
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      return new Date(row.expectedDate) < today
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('mes/pur/order/export', {
        ...this.queryParams
      }, `order_${new Date().getTime()}.xlsx`)
    },
    /** 数值格式化：空值显示 0，保留 4 位小数（去尾 0） */
    num(v) {
      const n = Number(v) || 0
      return n
    },
    /** 金额格式化：2 位小数 */
    money(v) {
      const n = Number(v) || 0
      return n.toFixed(2)
    },
    /** 是否 > 0（用于决定是否显示次要信息行） */
    gt0(v) {
      return (Number(v) || 0) > 0
    }
  }
}
</script>

<style scoped>
.amt-sub {
  font-size: 11px;
  color: #909399;
  line-height: 1.4;
  margin-top: 2px;
}
.cancel-text {
  color: #f56c6c;
}
</style>
