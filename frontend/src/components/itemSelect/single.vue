<template>
  <el-dialog title="物料选择" v-model="showFlag" width="80%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="物料编码" prop="itemCode">
        <el-input v-model="queryParams.itemCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="物料名称" prop="itemName">
        <el-input v-model="queryParams.itemName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="规格型号" prop="specification">
        <el-input v-model="queryParams.specification" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="itemList" highlight-current-row @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedItemId" :value="scope.row.itemId" @change="handleRowChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="编码" align="center" prop="itemCode" width="130" />
      <el-table-column label="名称" align="center" prop="itemName" :show-overflow-tooltip="true" />
      <el-table-column label="规格型号" align="center" prop="specification" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="单位" align="center" prop="unitOfMeasure" width="70" />
      <el-table-column label="分类" align="center" prop="itemTypeId" width="80" />
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

<script setup lang="ts" name="ItemSelect">
import { ref, reactive, toRefs } from 'vue'
import { listItem } from '@/api/mes/md/item'
import type { MdItem } from '@/types'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ onSelected: [row: MdItem] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const selectedItemId = ref<number>()
const selectedRow = ref<MdItem>()
const itemList = ref<MdItem[]>([])

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10, enableFlag: '1' } as any
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listItem(queryParams.value).then(r => { itemList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = {}; handleQuery() }
function handleRowChange(row: MdItem) { selectedRow.value = row }
function handleRowDbClick(row: MdItem) {
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
  selectedItemId.value = id
  if (!itemList.value.length) getList()
}

defineExpose({ open })
getList()
</script>
