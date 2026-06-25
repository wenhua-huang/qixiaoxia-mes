<template>
  <el-dialog title="采购订单选择" v-model="showFlag" width="80%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="订单编码" prop="orderCode">
        <el-input v-model="queryParams.orderCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="订单名称" prop="orderName">
        <el-input v-model="queryParams.orderName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="vendorName">
        <el-input v-model="queryParams.vendorName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="orderList" highlight-current-row @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedOrderId" :value="scope.row.orderId" @change="handleRowChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="订单编码" align="center" prop="orderCode" width="150" />
      <el-table-column label="订单名称" align="center" prop="orderName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="供应商" align="center" prop="vendorName" width="150" />
      <el-table-column label="下单日期" align="center" prop="orderDate" width="110" />
      <el-table-column label="预计到货" align="center" prop="expectedDate" width="110" />
      <el-table-column label="采购类型" align="center" prop="purchaseType" width="90">
        <template #default="scope">{{ purchaseTypeMap[scope.row.purchaseType] || scope.row.purchaseType }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">{{ statusMap[scope.row.status] || scope.row.status }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <template #footer>
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="showFlag = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="PurOrderSelect">
import { ref, reactive, toRefs } from 'vue'
import { listOrder } from '@/api/mes/pur/order'
import type { PurOrder } from '@/types/api/mes/pur/order'

const emit = defineEmits<{ onSelected: [row: PurOrder] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const selectedOrderId = ref<number>()
const selectedRow = ref<PurOrder>()
const orderList = ref<PurOrder[]>([])

const purchaseTypeMap: Record<string, string> = {
  PAPER: '纸张', AUX: '辅料', PACK: '包材', OTHER: '其他'
}
const statusMap: Record<string, string> = {
  DRAFT: '草稿', APPROVED: '已审批', ORDERED: '已下单', RECEIVING: '收货中', RECEIVED: '已收货', CLOSED: '已关闭', CANCEL: '已取消'
}

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10 } as any
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listOrder(queryParams.value).then(r => { orderList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = {}; handleQuery() }
function handleRowChange(row: PurOrder) { selectedRow.value = row }
function handleRowDbClick(row: PurOrder) {
  selectedRow.value = row
  emit('onSelected', row)
  showFlag.value = false
}
function confirmSelect() {
  if (!selectedRow.value) { return ElMessage.warning('请选择一条数据') }
  emit('onSelected', selectedRow.value)
  showFlag.value = false
}
function open(id?: number) {
  showFlag.value = true
  selectedOrderId.value = id
  if (!orderList.value.length) getList()
}

defineExpose({ open })

import { ElMessage } from 'element-plus'
getList()
</script>
