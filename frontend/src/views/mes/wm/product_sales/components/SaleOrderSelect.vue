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
      <el-table-column label="状态" prop="status" width="80" align="center">
        <template #default="s">
          <el-tag size="small" :type="statusType(s.row.status)">{{ statusText(s.row.status) }}</el-tag>
        </template>
      </el-table-column>
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
import request from '@/utils/request'
import { getDicts } from '@/api/system/dict/data'

const emit = defineEmits<{ onSelected: [row: any] }>()
const show = ref(false)
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<any>({ pageNum: 1, pageSize: 10 })
// 销售订单状态字典(mes_sal_order_status)，用于状态列文案/标签色
const statusOptions = ref<any[]>([])
getDicts('mes_sal_order_status').then(r => { statusOptions.value = r.data || [] })

function open() {
  show.value = true
  query.pageNum = 1
  load()
}
function load() {
  loading.value = true
  // 仅可选 已确认/生产中 两态；URLSearchParams 重复 key 供 Spring 绑定 List<String>
  const params = new URLSearchParams()
  params.append('pageNum', String(query.pageNum))
  params.append('pageSize', String(query.pageSize))
  params.append('includeProgress', 'false')
  params.append('statusList', 'CONFIRMED')
  params.append('statusList', 'PRODUCING')
  if (query.orderCode) params.append('orderCode', query.orderCode)
  if (query.clientName) params.append('clientName', query.clientName)
  // 注意：本项目 request 拦截器对 GET params 走 tansParams(Object.keys 遍历)，
  // URLSearchParams 无枚举键会被清空，故把重复 key 查询串直接拼到 URL
  request.get('/mes/sal/order/list?' + params.toString()).then((r: any) => {
    list.value = r.rows
    total.value = r.total
  }).finally(() => { loading.value = false })
}
function statusText(s: string) {
  const d = statusOptions.value.find((x: any) => x.dictValue === s)
  return d ? d.dictLabel : s
}
function statusType(s: string) {
  const d = statusOptions.value.find((x: any) => x.dictValue === s)
  return d?.listClass || ''
}
function handleRow(row: any) {
  emit('onSelected', row)
  show.value = false
}
defineExpose({ open })
</script>
