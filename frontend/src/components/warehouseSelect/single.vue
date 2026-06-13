<template>
  <el-dialog title="仓库选择" v-model="showFlag" width="80%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="编码" prop="warehouseCode">
        <el-input v-model="queryParams.warehouseCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="名称" prop="warehouseName">
        <el-input v-model="queryParams.warehouseName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" highlight-current-row @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedId" :value="scope.row.warehouseId" @change="handleRowChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="编码" align="center" prop="warehouseCode" width="120" />
      <el-table-column label="名称" align="center" prop="warehouseName" :show-overflow-tooltip="true" />
      <el-table-column label="类型" align="center" prop="warehouseType" width="90" />
      <el-table-column label="地址" align="center" prop="address" :show-overflow-tooltip="true" />
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <template #footer>
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="showFlag = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="WarehouseSelect">
import { ref, reactive, toRefs } from 'vue'
import { listWmWarehouse } from '@/api/mes/wm/warehouse'
import type { WmWarehouse } from '@/types'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ onSelected: [row: WmWarehouse] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const selectedId = ref<number>()
const selectedRow = ref<WmWarehouse>()
const list = ref<WmWarehouse[]>([])

const data = reactive({ queryParams: { pageNum: 1, pageSize: 10 } as any })
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listWmWarehouse(queryParams.value).then(r => { list.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = {}; handleQuery() }
function handleRowChange(row: WmWarehouse) { selectedRow.value = row }
function handleRowDbClick(row: WmWarehouse) { selectedRow.value = row; emit('onSelected', row); showFlag.value = false }
function confirmSelect() {
  if (!selectedRow.value) { ElMessage.warning('请选择一条数据'); return }
  emit('onSelected', selectedRow.value); showFlag.value = false
}
function open(id?: number) { showFlag.value = true; selectedId.value = id; if (!list.value.length) getList() }
defineExpose({ open })
getList()
</script>
