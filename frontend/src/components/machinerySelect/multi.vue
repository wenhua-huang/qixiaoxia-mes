<template>
  <el-dialog title="设备选择" v-model="showFlag" width="80%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" v-show="showSearch" label-width="80px">
      <div style="display:flex;gap:12px;align-items:flex-start;flex-wrap:wrap">
        <el-form-item label="设备编码" prop="machineryCode" style="flex:1;min-width:180px">
          <el-input v-model="queryParams.machineryCode" placeholder="请输入" clearable @keyup.enter="handleQuery" style="width:100%" />
        </el-form-item>
        <el-form-item label="设备名称" prop="machineryName" style="flex:1;min-width:180px">
          <el-input v-model="queryParams.machineryName" placeholder="请输入" clearable @keyup.enter="handleQuery" style="width:100%" />
        </el-form-item>
        <el-form-item label="车间" prop="workshopId" style="flex:1;min-width:180px">
          <el-select v-model="queryParams.workshopId" placeholder="请选择车间" clearable style="width:100%">
            <el-option v-for="item in workshopOptions" :key="item.workshopId" :label="item.workshopName" :value="item.workshopId" />
          </el-select>
        </el-form-item>
        <el-form-item style="flex-shrink:0">
          <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
        </el-form-item>
      </div>
    </el-form>

    <el-table
      v-loading="loading"
      :data="machineryList"
      ref="tableRef"
      row-key="machineryId"
      @selection-change="handleSelectionChange"
      @row-dblclick="handleRowDbClick"
    >
      <el-table-column type="selection" width="50" align="center" :selectable="isSelectable" />
      <el-table-column label="设备编码" align="center" prop="machineryCode" width="140" />
      <el-table-column label="设备名称" align="center" prop="machineryName" :show-overflow-tooltip="true" min-width="160" />
      <el-table-column label="设备类型" align="center" prop="machineryTypeName" min-width="120" />
      <el-table-column label="所属车间" align="center" prop="workshopName" width="120" />
      <el-table-column label="状态" align="center" prop="status" width="90" />
      <el-table-column label="已关联" align="center" width="80">
        <template #default="scope">
          <el-tag v-if="excludeSet.has(scope.row.machineryId)" type="info" size="small">已添加</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <template #footer>
      <el-button type="primary" @click="confirmSelect">确 定</el-button>
      <el-button @click="showFlag = false">取 消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" name="MachinerySelect">
import { ref, reactive, onMounted } from 'vue'
import { listMachinery } from '@/api/mes/dv/machinery'
import { listAllWorkshop } from '@/api/mes/md/workshop'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ onSelected: [rows: any[]] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const machineryList = ref<any[]>([])
const selectedRows = ref<any[]>([])
const tableRef = ref()
const excludeSet = ref<Set<number>>(new Set())

// 车间选项
const workshopOptions = ref<any[]>([])

const queryParams = reactive<Record<string, any>>({
  pageNum: 1,
  pageSize: 10,
  enableFlag: '1',
  machineryCode: undefined,
  machineryName: undefined,
  workshopId: undefined,
})

function loadOptions() {
  listAllWorkshop().then(r => { workshopOptions.value = (r as any).data || [] })
}
onMounted(() => { loadOptions() })

function getList() {
  loading.value = true
  listMachinery({ ...queryParams } as any).then(r => {
    machineryList.value = r.rows || []
    total.value = r.total
    loading.value = false
  }).catch(() => { loading.value = false })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  queryParams.machineryCode = undefined
  queryParams.machineryName = undefined
  queryParams.workshopId = undefined
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  getList()
}

function isSelectable(row: any) {
  return !excludeSet.value.has(row.machineryId)
}

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function handleRowDbClick(row: any) {
  if (excludeSet.value.has(row.machineryId)) {
    ElMessage.warning('该设备已关联')
    return
  }
  emit('onSelected', [row])
  showFlag.value = false
}

function confirmSelect() {
  if (!selectedRows.value || selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条数据')
    return
  }
  emit('onSelected', selectedRows.value)
  showFlag.value = false
}

/**
 * 打开设备选择弹窗
 * @param excludeIds 已经关联的 machineryId 列表（禁选）
 */
function open(excludeIds?: number[]) {
  showFlag.value = true
  excludeSet.value = new Set(excludeIds || [])
  queryParams.machineryCode = undefined
  queryParams.machineryName = undefined
  queryParams.workshopId = undefined
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  selectedRows.value = []
  getList()
}

defineExpose({ open })
</script>
