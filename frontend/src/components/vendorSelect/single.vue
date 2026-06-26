<template>
  <el-dialog title="供应商选择" v-model="showFlag" width="80%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="供应商编码" prop="vendorCode">
        <el-input v-model="queryParams.vendorCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商名称" prop="vendorName">
        <el-input v-model="queryParams.vendorName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="vendorList" highlight-current-row @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedVendorId" :value="scope.row.vendorId" @change="handleRowChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="编码" align="center" prop="vendorCode" width="120" />
      <el-table-column label="全称" align="center" prop="vendorName" :show-overflow-tooltip="true" />
      <el-table-column label="简称" align="center" prop="vendorNick" width="100" />
      <el-table-column label="等级" align="center" prop="vendorLevel" width="80" />
      <el-table-column label="电话" align="center" prop="tel" width="130" />
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

<script setup lang="ts" name="VendorSelect">
import { ref, reactive, toRefs } from 'vue'
import { listVendor } from '@/api/mes/md/vendor'
import type { MdVendor } from '@/types'

const emit = defineEmits<{ onSelected: [row: MdVendor] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const selectedVendorId = ref<number>()
const selectedRow = ref<MdVendor>()
const vendorList = ref<MdVendor[]>([])

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, enableFlag: '1' } as any
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listVendor(queryParams.value).then(r => { vendorList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = {}; handleQuery() }
function handleRowChange(row: MdVendor) { selectedRow.value = row }
function handleRowDbClick(row: MdVendor) {
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
  selectedVendorId.value = id
  if (!vendorList.value.length) getList()
}

defineExpose({ open })

import { ElMessage } from 'element-plus'
getList()
</script>
