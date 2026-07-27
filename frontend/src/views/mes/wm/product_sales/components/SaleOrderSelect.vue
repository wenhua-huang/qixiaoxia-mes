<template>
  <el-dialog title="选择销售订单" v-model="show" width="800px" append-to-body>
    <el-form :inline="true" size="small">
      <el-form-item label="订单编码">
        <el-input v-model="query.orderCode" placeholder="请输入" clearable @keyup.enter="load" style="width:160px" />
      </el-form-item>
      <el-form-item label="客户">
        <el-input v-model="query.clientName" placeholder="请输入" clearable @keyup.enter="load" style="width:140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="load">搜索</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="list" size="small" border highlight-current-row @row-click="handleRow">
      <el-table-column label="订单编码" prop="orderCode" width="150" />
      <el-table-column label="客户名称" prop="clientName" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="PO号" prop="clientOrderCode" width="120" />
      <el-table-column label="业务员" prop="salesperson" width="90" />
      <el-table-column label="状态" prop="status" width="80" align="center" />
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:current-page="query.pageNum"
                v-model:page-size="query.pageSize" @pagination="load" />
    <template #footer>
      <el-button @click="show = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { listOrder } from '@/api/mes/sal/order'

const emit = defineEmits<{ onSelected: [row: any] }>()
const show = ref(false)
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<any>({ pageNum: 1, pageSize: 10, status: 'CONFIRMED' })

function open() {
  show.value = true
  query.pageNum = 1
  load()
}
function load() {
  loading.value = true
  listOrder(query).then((r: any) => {
    list.value = r.rows
    total.value = r.total
  }).finally(() => { loading.value = false })
}
function handleRow(row: any) {
  emit('onSelected', row)
  show.value = false
}
defineExpose({ open })
</script>
