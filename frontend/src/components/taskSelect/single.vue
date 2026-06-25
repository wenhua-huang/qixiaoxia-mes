<template>
  <el-dialog title="生产任务选择" v-model="showFlag" width="80%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="任务编码" prop="taskCode">
        <el-input v-model="queryParams.taskCode" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="任务名称" prop="taskName">
        <el-input v-model="queryParams.taskName" placeholder="请输入" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" size="small" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="taskList" highlight-current-row @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedTaskId" :value="scope.row.taskId" @change="handleRowChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="任务编码" align="center" prop="taskCode" width="140" />
      <el-table-column label="任务名称" align="center" prop="taskName" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="工单" align="center" prop="workorderName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="工作站" align="center" prop="workstationName" width="100" />
      <el-table-column label="工序" align="center" prop="processName" width="100" />
      <el-table-column label="排产数量" align="center" prop="quantity" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <span :style="{color: statusColor[scope.row.status] || '#909399'}">{{ statusMap[scope.row.status] || scope.row.status }}</span>
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

<script setup lang="ts" name="TaskSelect">
import { ref, reactive, toRefs } from 'vue'
import { listTask } from '@/api/mes/pro/task'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{ onSelected: [row: any] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const selectedTaskId = ref<number>()
const selectedRow = ref<any>()
const taskList = ref<any[]>([])

const statusMap: Record<string,string> = { PREPARE:'待排产',NORMAL:'正常',PRODUCING:'生产中',COMPLETED:'已完成',PAUSED:'暂停',CANCEL:'取消' }
const statusColor: Record<string,string> = { PREPARE:'#E6A23C',NORMAL:'#409EFF',PRODUCING:'#67C23A',COMPLETED:'#909399',PAUSED:'#E6A23C',CANCEL:'#F56C6C' }

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 10 } as any
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listTask(queryParams.value).then((r: any) => { taskList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { queryParams.value = {}; handleQuery() }
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

function open(workorderId?: number) {
  showFlag.value = true
  if (workorderId) queryParams.value.workorderId = workorderId
  getList()
}

defineExpose({ open })
</script>
