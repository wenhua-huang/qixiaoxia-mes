<template>
  <div>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain size="small" @click="handleAdd" v-hasPermi="['mes:pur:order-line:add']">新增行</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain size="small" :disabled="multiple" @click="handleDelete" v-hasPermi="['mes:pur:order-line:remove']">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="lineList" @selection-change="handleSelectionChange" border>
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="物料编码" align="center" prop="itemCode" width="120" />
      <el-table-column label="物料名称" align="center" prop="itemName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="规格" align="center" prop="specification" width="100" />
      <el-table-column label="订购数量" align="center" prop="quantityOrdered" width="100" />
      <el-table-column label="已收数量" align="center" prop="quantityReceived" width="100" />
      <el-table-column label="单价" align="center" prop="unitPrice" width="90" />
      <el-table-column label="金额" align="center" prop="amount" width="100" />
      <el-table-column label="门幅" align="center" prop="paperWidth" width="80" />
      <el-table-column label="克重" align="center" prop="paperWeight" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope"><dict-tag :options="mes_order_status" :value="scope.row.status" /></template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/修改行弹窗 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
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
            <el-form-item label="门幅(mm)" prop="paperWidth">
              <el-input v-model="form.paperWidth" placeholder="如 925" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="克重(g)" prop="paperWeight">
              <el-input v-model="form.paperWeight" placeholder="如 120" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="纸张种类" prop="paperType">
              <el-select v-model="form.paperType" placeholder="请选择" style="width:100%">
                <el-option label="乌卡" value="乌卡" />
                <el-option label="俄卡" value="俄卡" />
                <el-option label="箱板纸" value="箱板纸" />
                <el-option label="白牛皮" value="白牛皮" />
                <el-option label="TC箱板纸" value="TC箱板纸" />
                <el-option label="瑞典赤牛" value="瑞典赤牛" />
              </el-select>
            </el-form-item>
          </el-col>
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
      <template #footer>
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { listLine, delLine, addLine, updateLine } from "@/api/mes/pur/order-line"
import ItemSelect from "@/components/itemSelect/single.vue"

export default {
  name: "PurOrderLine",
  components: { ItemSelect },
  setup() {
    const { mes_order_status } = useDict("mes_order_status")
    return { mes_order_status }
  },
  props: { orderId: { type: Number, default: null } },
  data() {
    return {
      loading: false, lineList: [], ids: [], single: true, multiple: true,
      open: false, title: "新增采购订单行",
      form: {},
      rules: {
        itemName: [{ required: true, message: "请选择物料", trigger: "change" }],
        quantityOrdered: [{ required: true, message: "订购数量不能为空", trigger: "blur" }],
        unitName: [{ required: true, message: "单位不能为空", trigger: "blur" }],
      }
    }
  },
  watch: { orderId(val) { if (val) this.getList() } },
  created() { if (this.orderId) this.getList() },
  methods: {
    getList() {
      if (!this.orderId) return
      this.loading = true
      listLine({ pageNum: 1, pageSize: 100, orderId: this.orderId }).then(r => {
        this.lineList = r.rows || []; this.loading = false
      })
    },
    reset() { this.form = { itemId: 201, unitOfMeasure: "TON", unitName: "吨", quantityOrdered: 0, unitPrice: 0, amount: 0, taxRate: 0, status: "ORDERED" } },
    handleSelectionChange(s) { this.ids = s.map(i => i.lineId); this.single = s.length !== 1; this.multiple = !s.length },
    handleSelectItem() { this.$refs.itemSelectRef.open() },
    onItemSelected(row) {
      this.form.itemId = row.itemId; this.form.itemCode = row.itemCode || row.itemCode
      this.form.itemName = row.itemName; this.form.specification = row.specification || row.spec
      this.form.unitOfMeasure = row.unitOfMeasure || ""; this.form.unitName = row.unitName || ""
    },
    handleAdd() { this.reset(); this.open = true; this.title = "新增采购订单行" },
    handleUpdate(row) {
      this.reset()
      this.form = JSON.parse(JSON.stringify(row))
      this.open = true; this.title = "修改采购订单行"
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        this.form.orderId = this.orderId
        this.form.amount = (this.form.unitPrice || 0) * (this.form.quantityOrdered || 0)
        if (this.form.lineId) {
          updateLine(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
        } else {
          addLine(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
        }
      })
    },
    handleDelete(row) {
      const ids = row ? [row.lineId] : this.ids
      this.$modal.confirm("确认删除？").then(() => delLine(ids)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") })
    }
  }
}
</script>
