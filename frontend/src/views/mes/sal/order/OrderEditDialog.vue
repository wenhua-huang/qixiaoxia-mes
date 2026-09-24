<template>
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
        <el-col :span="6"><el-form-item label="订单类型" prop="orderType"><el-select v-model="form.orderType" style="width:100%" @change="onHeadDimensionChange"><el-option v-for="d in orderTypeOptions" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" /></el-select></el-form-item></el-col>
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

    <ClientSelect ref="clientSelectRef" @onSelected="onClientSelected" />
    <LineEdit v-model="lineEditOpen" :line="editingLine" :order-type="form.orderType" :outsource-flag="form.outsourceFlag" :package-flag="form.packageFlag" @confirm="onLineConfirm" />
  </el-dialog>
</template>

<script>
import { getOrderDetail, createOrderWithLines, updateOrderWithLines } from '@/api/mes/sal/order'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import { resolveRouteProductBatch } from '@/api/mes/pro/routeproduct'
import { listRoute } from '@/api/mes/pro/proroute'
import { listUser } from '@/api/system/user'
import ClientSelect from '@/components/clientSelect/single.vue'
import LineEdit from './LineEdit.vue'

export default {
  name: 'OrderEditDialog',
  components: { ClientSelect, LineEdit },
  props: {
    // 销售订单类型字典(mes_sal_order_type)，由列表页统一加载后下发
    orderTypeOptions: { type: Array, default: () => [] }
  },
  emits: ['success'],
  data() {
    return {
      open: false, optType: undefined, autoGenFlag: true,
      // 业务员选项列表(SysUser,存展示用姓名 nickName,与客户/成品销售的 salesperson 口径一致)
      userOptions: [],
      form: {}, lineList: [],
      lineEditOpen: false, editingLine: null,
      // 头维度批量重算: loading + 自增序号丢弃过期响应(连续切换/明细变动)
      dimRecalcing: false, headDimReqSeq: 0,
      rules: {
        orderCode: [{ required: true, message: '销售订单号不能为空', trigger: 'blur' }],
        orderName: [{ required: true, message: '订单名称不能为空', trigger: 'blur' }],
        clientName: [{ required: true, message: '请选择客户', trigger: 'change' }]
      }
    }
  },
  computed: {
    title() { return this.form.orderId ? '修改销售订单' : '新增销售订单' }
  },
  created() { this.loadUserOptions() },
  methods: {
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
    /** 列表页 ref 调：新增弹窗；建单默认待接单由后端落库，此处仅占位 */
    openAdd() { this.reset(); this.optType = 'add'; this.handleAutoGenChange(true); this.open = true },
    /** 列表页 ref 调：编辑弹窗 */
    openEdit(id) {
      this.reset(); this.optType = 'edit'; this.autoGenFlag = false
      getOrderDetail(id).then(r => { this.form = r.data; this.lineList = r.data.lines || []; this.open = true })
    },
    reset() {
      this.form = { orderId: null, orderCode: null, orderName: null, orderType: 'STANDARD', clientId: null, clientCode: null, clientName: null, clientOrderCode: null, salesperson: null, businessLine: null, sampleFlag: 'N', outsourceFlag: 'N', packageFlag: 'N', orderDate: null, requestDate: null, totalAmount: 0, paymentMethod: null, status: 'PENDING_ACCEPT', remark: null }
      this.lineList = []; this.autoGenFlag = true; this.resetForm('form')
    },
    cancel() { this.open = false; this.reset() },
    handleAutoGenChange(v) { if (v) { genSerialCode('ORDER_NO').then(r => { this.form.orderCode = r.data }) } else { this.form.orderCode = '' } },
    handleSelectClient() { this.$refs.clientSelectRef.open() },
    onClientSelected(row) { this.form.clientId = row.clientId; this.form.clientCode = row.clientCode; this.form.clientName = row.clientName; this.form.clientNick = row.clientNick; this.form.salesperson = row.salesperson || null; this.form.businessLine = row.clientType || null },
    onLineConfirm(line) {
      if (line.lineId) { const i = this.lineList.findIndex(x => x.lineId === line.lineId); if (i >= 0) this.lineList.splice(i, 1, line) }
      else { line.lineNo = this.lineList.length + 1; line.lineId = Date.now(); this.lineList.push(line) }
      this.recalcTotalAmount()
    },
    handleEditLine(row) { this.editingLine = { ...row }; this.lineEditOpen = true },
    handleDeleteLine(idx) { this.lineList.splice(idx, 1); this.lineList.forEach((l, i) => { l.lineNo = i + 1 }); this.recalcTotalAmount() },
    recalcTotalAmount() { this.form.totalAmount = this.lineList.reduce((s, l) => s + (Number(l.lineAmount) || Number(l.unitPrice || 0) * Number(l.quantity || 0) || 0), 0) },
    handleAddLine() { this.editingLine = null; this.lineEditOpen = true },
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
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        if (!this.lineList.length) { this.$modal.msgError('请至少添加一条明细行'); return }
        const payload = { order: { ...this.form }, lines: this.lineList.map(({ quantityProduced, quantityConvertible, ...rest }) => rest) }
        const fn = this.form.orderId ? updateOrderWithLines : createOrderWithLines
        fn(payload).then(() => {
          this.$modal.msgSuccess(this.form.orderId ? '修改成功' : '新增成功')
          this.open = false
          this.$emit('success')
        })
      })
    }
  }
}
</script>
