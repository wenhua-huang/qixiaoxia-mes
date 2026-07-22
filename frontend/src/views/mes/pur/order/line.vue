<template>
  <div>
    <el-row v-if="!readOnly" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['mes:pur:order-line:add']">新增行</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:pur:order-line:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="lineList" @selection-change="handleSelectionChange" border>
      <el-table-column v-if="!readOnly" type="selection" width="45" align="center" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="规格" align="center" prop="specification" width="100" />
      <el-table-column label="订购数量" align="center" prop="quantityOrdered" width="100" />
      <el-table-column label="已收数量" align="center" prop="quantityReceived" width="100" />
      <el-table-column label="已退数量" align="center" prop="quantityReturned" width="100" />
      <el-table-column label="单价" align="center" prop="unitPrice" width="90" />
      <el-table-column label="金额" align="center" prop="amount" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope"><dict-tag :options="mes_order_status" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column v-if="!readOnly" label="操作" align="center" width="200">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" size="small" @click="handleDelete(scope.row)">删除</el-button>
          <el-button v-if="canCancelLine(scope.row)" link type="danger" size="small" @click="handleCancelLine(scope.row)">取消</el-button>
          <el-button v-if="canTerminateLine(scope.row)" link type="warning" size="small" @click="handleTerminateLine(scope.row)">终止收货</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/修改行弹窗 -->
    <el-dialog :title="title" v-model="open" width="720px" append-to-body>
      <el-tabs v-model="activeTab">
        <!-- 基本信息 Tab -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-row>
              <el-col :span="24">
                <el-form-item label="物料" prop="itemName">
                  <el-input v-model="form.itemName" readonly placeholder="请选择物料">
                    <template #append><el-button @click="handleSelectItem">搜索</el-button></template>
                  </el-input>
                </el-form-item>
                <ItemSelect ref="itemSelectRef" @onSelected="onItemSelected" />
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="规格型号" prop="specification">
                  <el-input v-model="form.specification" placeholder="规格型号" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="单位" prop="unitName">
                  <el-input v-model="form.unitName" placeholder="如：吨" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="订购数量" prop="quantityOrdered">
                  <el-input-number v-model="form.quantityOrdered" :min="0" :precision="4" style="width:100%" placeholder="数量" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="单价(元)" prop="unitPrice">
                  <el-input-number v-model="form.unitPrice" :min="0" :precision="4" style="width:100%" placeholder="单价" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="税率(%)" prop="taxRate">
                  <el-input-number v-model="form.taxRate" :min="0" :max="100" :precision="2" style="width:100%" placeholder="税率" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="24">
                <el-form-item label="备注" prop="remark">
                  <el-input v-model="form.remark" type="textarea" placeholder="备注" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 产品属性 Tab -->
        <el-tab-pane label="产品属性" name="product">
          <el-alert title="以下属性从物料主数据带出，修改仅影响本次采购。如需长期变更规格，请前往物料管理新建物料变体。" type="info" show-icon :closable="false" style="margin-bottom:12px" />
          <el-form ref="productFormRef" :model="form.lineAttrs?.product || {}" label-width="100px">
            <el-row>
              <el-col :span="12">
                <el-form-item label="产品尺寸">
                  <el-input v-model="form.lineAttrs.product.size" placeholder="如254*127*330mm" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="装箱规格">
                  <el-input v-model="form.lineAttrs.product.packageSpec" placeholder="如250个/箱" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="印刷要求">
              <el-input v-model="form.lineAttrs.product.printingReq" type="textarea" placeholder="如1色满版黑印刷" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 纸张属性 Tab -->
        <el-tab-pane label="纸张属性" name="paper">
          <el-alert title="以下属性从物料主数据带出，修改仅影响本次采购。如需长期变更规格，请前往物料管理新建物料变体。" type="info" show-icon :closable="false" style="margin-bottom:12px" />
          <el-form ref="paperFormRef" :model="form.lineAttrs?.paper || {}" label-width="100px">
            <el-row>
              <el-col :span="12">
                <el-form-item label="门幅(mm)">
                  <el-input v-model="form.lineAttrs.paper.width" placeholder="如 925" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="克重(g)">
                  <el-input v-model="form.lineAttrs.paper.weight" placeholder="如 120" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="来源/品牌">
                  <el-input v-model="form.lineAttrs.paper.source" placeholder="如联盛A2" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="种类">
                  <el-input v-model="form.lineAttrs.paper.type" placeholder="如箱板纸" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item label="预计卷数">
                  <el-input-number v-model="form.lineAttrs.paper.rollCount" :min="0" :precision="0" style="width:100%" placeholder="卷数" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 纸袋属性 Tab -->
        <el-tab-pane label="纸袋属性" name="paperBag">
          <el-alert title="以下属性从物料主数据带出，修改仅影响本次采购。如需长期变更规格，请前往物料管理新建物料变体。" type="info" show-icon :closable="false" style="margin-bottom:12px" />
          <el-form ref="paperBagFormRef" :model="form.lineAttrs?.paperBag || {}" label-width="100px">
            <el-row>
              <el-col :span="12">
                <el-form-item label="绳料规格">
                  <el-input v-model="form.lineAttrs.paperBag.ropeSpec" placeholder="如7.5cm黄牛皮圆纸绳" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="口部提拔">
                  <el-input v-model="form.lineAttrs.paperBag.mouthType" placeholder="锯齿口/平口/翻口" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="底板类型">
              <el-input v-model="form.lineAttrs.paperBag.bottomType" placeholder="无/灰底白板/加强底板" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- 取消/终止行弹窗 -->
    <el-dialog :title="lineActionTitle" v-model="lineActionOpen" width="450px" append-to-body>
      <el-form label-width="80px">
        <el-form-item :label="lineActionType === 'cancel' ? '取消原因' : '终止原因'">
          <el-select v-model="lineActionReason" placeholder="请选择原因" style="width:100%">
            <el-option v-for="d in mes_cancel_reason" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="danger" @click="confirmLineAction">确认</el-button>
        <el-button @click="lineActionOpen = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listLine, delLine, addLine, updateLine } from "@/api/mes/pur/order-line"
import { cancelLine, terminateLine } from "@/api/mes/pur/order"
import ItemSelect from "@/components/itemSelect/single.vue"
import { getItem } from "@/api/mes/md/item"

export default {
  name: "PurOrderLine",
  components: { ItemSelect },
  setup() {
    const { mes_order_status, mes_cancel_reason } = useDict("mes_order_status", "mes_cancel_reason")
    return { mes_order_status, mes_cancel_reason }
  },
  props: { orderId: { type: Number, default: null }, initLines: { type: Array, default: null }, readOnly: { type: Boolean, default: false } },
  emits: ['change'],
  data() {
    return {
      loading: false, lineList: [], ids: [], single: true, multiple: true,
      open: false, title: "新增采购订单行",
      activeTab: "basic",
      form: {},
      // 取消/终止行
      lineActionOpen: false,
      lineActionTitle: "",
      lineActionType: "",
      lineActionTargetId: null,
      lineActionReason: null,
      rules: {
        itemName: [{ required: true, message: "请选择物料", trigger: "change" }],
        quantityOrdered: [{ required: true, message: "订购数量不能为空", trigger: "blur" }],
        unitName: [{ required: true, message: "单位不能为空", trigger: "blur" }],
      }
    }
  },
  watch: { orderId(val) { if (val) this.loadLines() } },
  created() { this.loadLines() },
  methods: {
    loadLines() {
      if (this.initLines !== null) {
        this.lineList = this.initLines
      } else if (this.orderId) {
        this.getList()
      }
    },

    getList() {
      if (!this.orderId) return
      this.loading = true
      listLine({ pageNum: 1, pageSize: 100, orderId: this.orderId }).then(r => {
        this.lineList = r.rows || []; this.loading = false
      })
    },
    reset() {
      this.activeTab = "basic"
      this.form = {
        itemId: null, itemCode: null, itemName: null, specification: null,
        unitOfMeasure: null, unitName: null,
        quantityOrdered: 0, unitPrice: 0, amount: 0, taxRate: 0,
        // 行状态不前端硬编码，由后端按订单头状态自动设置（行跟随头）
        lineAttrs: { paper: {}, paperBag: {}, product: {} }
      }
    },
    handleSelectionChange(s) { this.ids = s.map(i => i.lineId); this.single = s.length !== 1; this.multiple = !s.length },
    handleSelectItem() { this.$refs.itemSelectRef.open() },
    onItemSelected(row) {
      // 基本字段回填
      this.form.itemId = row.itemId
      this.form.itemCode = row.itemCode || row.itemCode
      this.form.itemName = row.itemName
      this.form.specification = row.specification || row.spec
      this.form.unitOfMeasure = row.unitOfMeasure || ""
      this.form.unitName = row.unitName || ""

      // 获取物料完整属性（含行业子表），回填到 lineAttrs
      if (row.itemId) {
        getItem(row.itemId).then(r => {
          const item = r.data
          if (!item) return

          // 初始化 lineAttrs 各分组
          if (!this.form.lineAttrs) {
            this.form.lineAttrs = { paper: {}, paperBag: {}, product: {} }
          }

          // 纸张属性回填
          if (item.attrPaper) {
            this.form.lineAttrs.paper = {
              width: item.attrPaper.paperWidth || "",
              weight: item.attrPaper.paperWeight || "",
              source: item.attrPaper.paperSource || "",
              type: item.attrPaper.paperType || "",
              rollCount: null
            }
          }

          // 纸袋属性回填
          if (item.attrPaperBag) {
            this.form.lineAttrs.paperBag = {
              ropeSpec: item.attrPaperBag.ropeSpec || "",
              mouthType: item.attrPaperBag.mouthType || "",
              bottomType: item.attrPaperBag.bottomType || ""
            }
          }

          // 产品属性回填（从物料主表字段）
          this.form.lineAttrs.product = {
            size: item.productSize || "",
            packageSpec: item.packageSpec || "",
            printingReq: item.printingReq || ""
          }
        })
      }
    },
    handleAdd() { this.reset(); this.open = true; this.title = "新增采购订单行" },
    handleUpdate(row) {
      this.reset()
      // 深拷贝，确保 lineAttrs 不引用同一对象
      this.form = JSON.parse(JSON.stringify(row))
      // 确保 lineAttrs 结构完整（旧数据可能没有 JSON 列）
      if (!this.form.lineAttrs) {
        this.form.lineAttrs = { paper: {}, paperBag: {}, product: {} }
      }
      if (!this.form.lineAttrs.paper) this.form.lineAttrs.paper = {}
      if (!this.form.lineAttrs.paperBag) this.form.lineAttrs.paperBag = {}
      if (!this.form.lineAttrs.product) this.form.lineAttrs.product = {}
      this.open = true; this.title = "修改采购订单行"
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.form.orderId = this.orderId
        this.form.amount = (this.form.unitPrice || 0) * (this.form.quantityOrdered || 0)
        if (this.form.lineId) {
          updateLine(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList(); this.$emit('change') })
        } else {
          addLine(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList(); this.$emit('change') })
        }
      })
    },
    /** 行是否可取消：ORDERED/RECEIVING 且已收数量为0 */
    canCancelLine(row) {
      const received = Number(row.quantityReceived) || 0
      return ['ORDERED', 'RECEIVING'].includes(row.status) && received === 0
    },
    /** 行是否可终止收货：RECEIVING 且已收>0 且 <订购 */
    canTerminateLine(row) {
      const received = Number(row.quantityReceived) || 0
      const ordered = Number(row.quantityOrdered) || 0
      return row.status === 'RECEIVING' && received > 0 && received < ordered
    },
    handleCancelLine(row) {
      this.lineActionTitle = "取消采购订单行"
      this.lineActionType = "cancel"
      this.lineActionTargetId = row.lineId
      this.lineActionReason = null
      this.lineActionOpen = true
    },
    handleTerminateLine(row) {
      this.lineActionTitle = "终止收货"
      this.lineActionType = "terminate"
      this.lineActionTargetId = row.lineId
      this.lineActionReason = null
      this.lineActionOpen = true
    },
    confirmLineAction() {
      const api = this.lineActionType === 'cancel'
        ? cancelLine(this.lineActionTargetId, this.lineActionReason)
        : terminateLine(this.lineActionTargetId, this.lineActionReason)
      api.then(() => {
        this.lineActionOpen = false
        this.getList()
        this.$modal.msgSuccess(this.lineActionType === 'cancel' ? "取消成功" : "终止成功")
        this.$emit('change')
      })
    },
    handleDelete(row) {
      const ids = row ? [row.lineId] : this.ids
      this.$modal.confirm("确认删除？").then(() => delLine(ids)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功"); this.$emit('change') })
    }
  }
}
</script>