<template>
  <el-dialog title="生产工单选择" v-model="showFlag" width="80%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="工单编码" prop="workorderCode">
        <el-input v-model="queryParams.workorderCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工单名称" prop="workorderName">
        <el-input v-model="queryParams.workorderName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status" v-if="!props.statusList || props.statusList.length === 0">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:130px">
          <el-option label="待生产" value="PREPARE" /><el-option label="生产中" value="PRODUCING" />
          <el-option label="已完成" value="COMPLETED" /><el-option label="已取消" value="CANCEL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="workorderList" highlight-current-row @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedWorkorderId" :value="scope.row.workorderId" @change="handleRowChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="工单编码" align="center" prop="workorderCode" width="130" />
      <el-table-column label="工单名称" align="center" prop="workorderName" :show-overflow-tooltip="true" min-width="160" />
      <el-table-column label="产品" align="center" prop="productName" :show-overflow-tooltip="true" width="130" />
      <el-table-column label="计划数量" align="center" prop="quantity" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <span :style="{color: statusColor[scope.row.status] || '#909399'}">{{ statusMap[scope.row.status] || scope.row.status }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="props.showQuantityRecpt" label="待入库量" align="center" width="100">
        <template #default="scope">
          {{ formatPending(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="需求日期" align="center" width="110">
        <template #default="scope">
          <span>{{ scope.row.requestDate ? scope.row.requestDate.substring(0,10) : '-' }}</span>
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

<script setup lang="ts" name="WorkorderSelect">
import { ref, reactive, toRefs } from 'vue'
import { listWorkorder } from '@/api/mes/pro/workorder'
import { ElMessage } from 'element-plus'

// 固定过滤条件（不传则保持默认行为，向后兼容）：
//  statusList: 固定状态过滤（如 ['PRODUCING','COMPLETED']），传后弹窗里不再显示状态下拉
//  requireProduced: 只显示 quantity_produced > 0 的工单
//  showQuantityRecpt: 列表显示「待入库量」列（quantity_produced - 已入库量）
const props = defineProps<{
  statusList?: string[]
  requireProduced?: boolean
  showQuantityRecpt?: boolean
}>()

const emit = defineEmits<{ onSelected: [row: any] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const selectedWorkorderId = ref<number>()
const selectedRow = ref<any>()
const workorderList = ref<any[]>([])

const statusMap: Record<string,string> = { PREPARE:'待生产', PRODUCING:'生产中', COMPLETED:'已完成', CANCEL:'已取消', CLOSED:'已关闭' }
const statusColor: Record<string,string> = { PREPARE:'#E6A23C', PRODUCING:'#409EFF', COMPLETED:'#67C23A', CANCEL:'#909399', CLOSED:'#909399' }

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10 } as any
})
const { queryParams } = toRefs(data)

// 把 props 固定过滤条件合并进 queryParams（每次查询前调用，保证 resetQuery 后也不丢）
function applyFixedFilters() {
  if (props.statusList && props.statusList.length > 0) {
    queryParams.value.statusList = [...props.statusList]
  }
  if (props.requireProduced) {
    // 用极小正数表达 "> 0"，避免改 SQL 写专用分支
    queryParams.value.quantityProducedMin = 0.000001
  }
}

function getList() {
  loading.value = true
  applyFixedFilters()
  listWorkorder(queryParams.value).then((r: any) => { workorderList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  queryParams.value = { pageNum: 1, pageSize: 10 }
  applyFixedFilters()
  getList()
}
// 待入库量 = 已生产 - 已入库（quantityRecpt 由后端子查询返回）
function formatPending(row: any): string {
  const produced = Number(row.quantityProduced) || 0
  const recpt = Number(row.quantityRecpt) || 0
  const pending = produced - recpt
  return pending > 0 ? String(pending) : '0'
}
function handleRowChange(row: any) { selectedRow.value = row }
function handleRowDbClick(row: any) {
  selectedRow.value = row
  emit('onSelected', row)
  showFlag.value = false
}
function confirmSelect() {
  if (!selectedRow.value) { ElMessage.warning('请选择一条数据'); return }
  emit('onSelected', selectedRow.value)
  showFlag.value = false
}

function open() {
  showFlag.value = true
  getList()
}

defineExpose({ open })
</script>
