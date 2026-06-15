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
      <el-table-column label="规格" align="center" prop="specification" width="120" />
      <el-table-column label="订购数量" align="center" prop="quantityOrdered" width="100" />
      <el-table-column label="单位" align="center" prop="unitName" width="70" />
      <el-table-column label="单价" align="center" prop="unitPrice" width="90" />
      <el-table-column label="金额" align="center" prop="amount" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">{{ statusMap[scope.row.status] || scope.row.status }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="handleUpdate(scope.row)">修改</el-button>
          <el-button link type="primary" size="small" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { listLine, getLine, delLine, addLine, updateLine } from "@/api/mes/pur/order-line"

export default {
  name: "PurOrderLine",
  props: {
    orderId: { type: Number, default: null }
  },
  data() {
    return {
      loading: false,
      lineList: [],
      ids: [],
      single: true,
      multiple: true,
      statusMap: { ORDERED: "已下单", RECEIVING: "收货中", RECEIVED: "已收货", CLOSED: "已关闭" },
      queryParams: { pageNum: 1, pageSize: 100 }
    }
  },
  watch: {
    orderId(val) { if (val) this.getList() }
  },
  created() { if (this.orderId) this.getList() },
  methods: {
    getList() {
      if (!this.orderId) return
      this.loading = true
      listLine({ ...this.queryParams, orderId: this.orderId }).then(r => {
        this.lineList = r.rows; this.loading = false
      })
    },
    handleSelectionChange(s) { this.ids = s.map(i => i.lineId); this.single = s.length !== 1; this.multiple = !s.length },
    handleAdd() {
      const itemCode = prompt("物料编码", "PAPER-XBZ-A")
      const itemName = prompt("物料名称", "箱板纸A级")
      const qty = prompt("订购数量", "1")
      if (!itemCode || !itemName || !qty) return
      const line = {
        orderId: this.orderId, itemId: 201, itemCode, itemName,
        unitOfMeasure: "TON", unitName: "吨",
        status: "ORDERED", quantityOrdered: parseFloat(qty), unitPrice: 0, amount: 0
      }
      addLine(line).then(() => { this.getList(); this.$modal.msgSuccess("新增成功") })
    },
    handleUpdate(row) {
      const itemCode = prompt("物料编码", row.itemCode)
      const itemName = prompt("物料名称", row.itemName)
      const qty = prompt("订购数量", row.quantityOrdered)
      if (itemCode && itemName && qty) {
        row.itemCode = itemCode; row.itemName = itemName; row.quantityOrdered = parseFloat(qty)
        row.amount = (row.unitPrice || 0) * row.quantityOrdered
        updateLine(row).then(() => { this.getList(); this.$modal.msgSuccess("修改成功") })
      }
    },
    handleDelete(row) {
      const ids = row ? [row.lineId] : this.ids
      this.$modal.confirm("确认删除？").then(() => delLine(ids)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") })
    }
  }
}
</script>
