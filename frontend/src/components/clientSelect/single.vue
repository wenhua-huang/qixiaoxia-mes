<template>
  <el-dialog title="客户选择" v-model="showFlag" width="60%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="客户编码" prop="clientCode">
        <el-input v-model="queryParams.clientCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="客户名称" prop="clientName">
        <el-input v-model="queryParams.clientName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="clientList" highlight-current-row @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedClientId" :value="scope.row.clientId" @change="handleRowChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="编码" align="center" prop="clientCode" width="130" />
      <el-table-column label="名称" align="center" prop="clientName" :show-overflow-tooltip="true" />
      <el-table-column label="简称" align="center" prop="clientNick" width="120" />
      <el-table-column label="类型" align="center" prop="clientType" width="90">
        <template #default="scope">{{ clientTypeText(scope.row.clientType) }}</template>
      </el-table-column>
      <el-table-column label="业务员" align="center" prop="salesperson" width="100" />
      <el-table-column label="启用" align="center" prop="enableFlag" width="70">
        <template #default="scope">{{ scope.row.enableFlag === '1' ? '是' : '否' }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <template #footer>
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="showFlag = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="ClientSelect">
import { listClient } from '@/api/mes/md/client'
import type { MdClient } from '@/types/api/mes/md/client'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ onSelected: [row: MdClient] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const selectedClientId = ref<number>()
const selectedRow = ref<MdClient>()
const clientList = ref<MdClient[]>([])

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, enableFlag: '1' } as any
})
const { queryParams } = toRefs(data)

function clientTypeText(t: string) {
  return t === 'DOMESTIC' ? '内贸' : t === 'FOREIGN' ? '外贸' : t === 'SPOT' ? '现货' : t
}
function getList() {
  loading.value = true
  listClient(queryParams.value).then(r => { clientList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10, enableFlag: '1' }; handleQuery() }
function handleRowChange(row: MdClient) { selectedRow.value = row }
function handleRowDbClick(row: MdClient) {
  selectedRow.value = row
  emit('onSelected', row)
  showFlag.value = false
}
function confirmSelect() {
  if (!selectedRow.value) { ElMessage.warning('请选择一条数据'); return }
  emit('onSelected', selectedRow.value)
  showFlag.value = false
}
function open(id?: number) {
  showFlag.value = true
  selectedClientId.value = id
  selectedRow.value = undefined
  if (!clientList.value.length) getList()
}

defineExpose({ open })
getList()
</script>
