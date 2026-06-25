<template>
  <el-dialog title="工作站选择" v-model="showFlag" width="80%" center :modal="false" append-to-body>
    <el-form :model="queryParams" ref="queryRef" size="small" v-show="showSearch" label-width="80px">
      <div style="display:flex;gap:12px;align-items:flex-start;flex-wrap:wrap">
        <el-form-item label="编码" prop="workstationCode" style="flex:1;min-width:180px">
          <el-input v-model="queryParams.workstationCode" placeholder="请输入" clearable @keyup.enter="handleQuery" style="width:100%" />
        </el-form-item>
        <el-form-item label="工序" prop="processId" style="flex:1;min-width:180px">
          <el-select v-model="queryParams.processId" placeholder="请选择工序" clearable style="width:100%">
            <el-option v-for="item in processOptions" :key="item.processId" :label="item.processName" :value="item.processId" />
          </el-select>
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

    <el-table v-loading="loading" :data="workstationList" highlight-current-row @row-dblclick="handleRowDbClick">
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio v-model="selectedWorkstationId" :value="scope.row.workstationId" @change="handleRowChange(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="编码" align="center" prop="workstationCode" width="130" />
      <el-table-column label="名称" align="center" prop="workstationName" :show-overflow-tooltip="true" min-width="150" />
      <el-table-column label="工序" align="center" prop="processName" :show-overflow-tooltip="true" min-width="120" />
      <el-table-column label="产能" align="center" prop="capacity" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80" />
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

<script setup lang="ts" name="WorkstationSelect">
import { ref, reactive, onMounted } from 'vue'
import { listWorkstation } from '@/api/mes/md/workstation'
import { listAllProcess } from '@/api/mes/pro/process'
import { listAllWorkshop } from '@/api/mes/md/workshop'
import { ElMessage } from 'element-plus'

const props = defineProps<{ processId?: number }>()
const emit = defineEmits<{ onSelected: [row: any] }>()

const showFlag = ref(false)
const loading = ref(false)
const total = ref(0)
const showSearch = ref(true)
const selectedWorkstationId = ref<number>()
const selectedRow = ref<any>()
const workstationList = ref<any[]>([])

// 工序选项
const processOptions = ref<any[]>([])
// 车间选项
const workshopOptions = ref<any[]>([])

// 查询参数用 reactive，在 open() 中整体替换
const queryParams = reactive<Record<string, any>>({
  pageNum: 1,
  pageSize: 10,
  processId: undefined,
  workshopId: undefined
})

// 加载工序和车间下拉选项
function loadOptions() {
  listAllProcess().then(r => { processOptions.value = (r as any).data || [] })
  listAllWorkshop().then(r => { workshopOptions.value = (r as any).data || [] })
}
onMounted(() => { loadOptions() })

function getList() {
  loading.value = true
  // 将 reactive 对象转为普通对象传给 API（确保 axios 能正确序列化）
  listWorkstation({ ...queryParams }).then(r => { workstationList.value = r.rows; total.value = r.total; loading.value = false })
}
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  queryParams.workstationCode = undefined
  queryParams.workstationName = undefined
  queryParams.workshopId = undefined
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  queryParams.processId = props.processId
  getList()
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

/**
 * 打开工作站选择弹窗
 * @param explicitProcessId 显式传入的工序ID（优先使用），其次使用 prop
 */
function open(explicitProcessId?: number) {
  showFlag.value = true
  queryParams.workstationCode = undefined
  queryParams.workstationName = undefined
  queryParams.workshopId = undefined
  queryParams.pageNum = 1
  queryParams.pageSize = 10
  // 只有工序ID在下拉选项中存在时才预选，否则不筛选
  const id = explicitProcessId ?? props.processId
  const validIds = new Set(processOptions.value.map((o: any) => o.processId))
  queryParams.processId = id != null && validIds.has(id) ? id : undefined
  getList()
}

defineExpose({ open })
</script>
