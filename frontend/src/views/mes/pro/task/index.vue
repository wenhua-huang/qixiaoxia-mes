<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="任务编码" prop="taskCode">
        <el-input v-model="queryParams.taskCode" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工单名称" prop="workorderName">
        <el-input v-model="queryParams.workorderName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工位" prop="workstationId">
        <el-input v-model="queryParams.workstationName" placeholder="请输入工位名称" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="工序名称" prop="processName">
        <el-input v-model="queryParams.processName" placeholder="请输入" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width:150px">
          <el-option label="待排产" value="PREPARE" />
          <el-option label="正常" value="NORMAL" />
          <el-option label="生产中" value="PRODUCING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="暂停" value="PAUSED" />
          <el-option label="取消" value="CANCEL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['mes:pro:task:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['mes:pro:task:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['mes:pro:task:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Download" @click="handleExport" v-hasPermi="['mes:pro:task:export']">导出</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="taskList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务编码" align="center" prop="taskCode" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="任务名称" align="center" prop="taskName" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="生产工单" align="center" prop="workorderName" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="工位" align="center" prop="workstationName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="工序" align="center" prop="processName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="计划数量" align="center" prop="quantity" width="90" />
      <el-table-column label="已生产" align="center" prop="quantityProduced" width="80" />
      <el-table-column label="颜色" align="center" prop="colorCode" width="80">
        <template #default="scope">
          <el-color-picker v-model="scope.row.colorCode" disabled size="small" v-if="scope.row.colorCode" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="155">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="155">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="总时长(h)" align="center" prop="duration" width="90" />
      <el-table-column label="状态" align="center" prop="status" width="85">
        <template #default="scope">
          <span :style="{ color: statusColor[scope.row.status] }">{{ statusMap[scope.row.status] || scope.row.status }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template #default="scope">
          <el-button link type="success" icon="Position" @click="handleDispatch(scope.row)" v-if="scope.row.status==='NORMAL'||scope.row.status==='PREPARE'" v-hasPermi="['mes:pro:task:edit']">下发</el-button>
          <el-button link type="primary" icon="CircleCheck" @click="handleComplete(scope.row)" v-if="scope.row.status==='PRODUCING'" v-hasPermi="['mes:pro:task:edit']">完成</el-button>
          <el-button link type="warning" icon="CircleClose" @click="handleCancelTask(scope.row)" v-if="scope.row.status!=='COMPLETED'&&scope.row.status!=='CANCEL'" v-hasPermi="['mes:pro:task:edit']">取消</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mes:pro:task:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mes:pro:task:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 对话框 -->
    <el-dialog :title="title" v-model="open" width="820px" append-to-body @close="cancel">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="16">
            <el-form-item label="任务编码" prop="taskCode">
              <el-input v-model="form.taskCode" placeholder="请输入" :disabled="optType !== 'add'" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label-width="80" v-if="optType === 'add'">
              <el-switch v-model="autoGenFlag" @change="handleAutoGenChange" size="small" />
              <span style="margin-left: 6px; font-size: 12px; color: #909399">自动生成</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="任务名称" prop="taskName">
              <el-input v-model="form.taskName" placeholder="选择工单和数量后自动生成：[产品名]【数量】单位" :disabled="optType === 'view'" maxlength="255" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="生产工单" prop="workorderName">
              <el-input v-model="form.workorderName" placeholder="请选择生产工单" readonly :disabled="optType === 'view'">
                <template #append v-if="optType !== 'view'">
                  <el-button icon="Search" @click="handleWorkorderSelect" />
                </template>
              </el-input>
              <workorderSelect ref="woSelectRef" @onSelected="onWorkorderSelected" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工位" prop="workstationName">
              <el-input v-model="form.workstationName" placeholder="请选择工作站" :disabled="optType === 'view'">
                <template #append v-if="optType !== 'view'">
                  <el-button icon="Search" @click="handleWorkstationSelect" />
                </template>
              </el-input>
            </el-form-item>
            <WorkstationSelect ref="wsSelectRef" :processId="form.processId" @onSelected="onWorkstationSelected" />
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="工序" prop="processId">
              <el-select v-model="form.processId" placeholder="请选择" style="width: 100%" :disabled="optType === 'view'" filterable @change="onProcessChange">
                <el-option v-for="p in processOptions" :key="p.processId" :label="p.processName" :value="p.processId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排产数量" prop="quantity">
              <el-input-number v-model="form.quantity" :min="1" :max="99999999" style="width: 100%" :disabled="optType === 'view'" placeholder="请输入排产数量" @change="updateTaskNameHint" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="甘特图颜色" prop="colorCode">
              <el-color-picker v-model="form.colorCode" :disabled="optType === 'view'" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-input v-model="form.colorCode" maxlength="7" placeholder="请输入颜色编码，或点击左侧选择" :disabled="optType === 'view'" style="margin-top: 8px" />
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="8">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择日期" clearable style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" :disabled="optType === 'view'" @change="calculateEndTime" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总时长(h)" prop="duration">
              <el-input-number v-model="form.duration" :min="0" :precision="2" :step="1" style="width: 100%" :disabled="optType === 'view'" placeholder="请输入" @change="calculateEndTime" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" placeholder="自动计算" disabled style="width: 100%" value-format="YYYY-MM-DD HH:mm:ss" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="设备编码" prop="machineCode">
              <el-input v-model="form.machineCode" placeholder="请输入" :disabled="optType === 'view'" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" placeholder="请输入" :disabled="optType === 'view'" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm" v-if="optType !== 'view'">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ProSchedule">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listTask, getTask, delTask, addTask, updateTask, dispatchTask, completeTask, cancelTask } from '@/api/mes/pro/task'
import { listWorkorder, getWorkorder } from '@/api/mes/pro/workorder'
import { listAllProcess } from '@/api/mes/pro/process'
import { listRouteProcessByRouteId } from '@/api/mes/pro/routeprocess'
import { listRouteProduct } from '@/api/mes/pro/routeproduct'
import { genSerialCode } from '@/api/mes/sys/autocoderule'
import WorkstationSelect from '@/components/workstationSelect/single.vue'
import workorderSelect from '@/components/workorderSelect/single.vue'

const { proxy } = getCurrentInstance() as any

const wsSelectRef = ref<InstanceType<typeof WorkstationSelect>>()
const woSelectRef = ref()
const formRef = ref()
const taskList = ref<any[]>([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref<number[]>([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref('')
const optType = ref('')
const autoGenFlag = ref(false)
const workorderOptions = ref<any[]>([])
const processOptions = ref<any[]>([])

const statusMap: Record<string, string> = {
  PREPARE: '待排产', NORMAL: '正常', PRODUCING: '生产中', COMPLETED: '已完成', PAUSED: '暂停', CANCEL: '取消'
}
const statusColor: Record<string, string> = {
  PREPARE: '#E6A23C', NORMAL: '#409EFF', PRODUCING: '#67C23A', COMPLETED: '#909399', PAUSED: '#E6A23C', CANCEL: '#F56C6C'
}

const data = reactive({
  form: {} as any,
  queryParams: { pageNum: 1, pageSize: 10 } as any,
  rules: {
    taskCode: [{ required: true, message: '任务编码不能为空', trigger: 'blur' }],
    taskName: [{ required: true, message: '任务名称不能为空', trigger: 'blur' }],
    workorderId: [{ required: true, message: '请选择生产工单', trigger: 'change' }],
    workstationName: [{ required: true, message: '工作站不能为空', trigger: 'blur' }],
    processId: [{ required: true, message: '请选择工序', trigger: 'change' }],
    quantity: [{ required: true, message: '排产数量不能为空', trigger: 'blur' }],
    startTime: [{ required: true, message: '请选择开始生产时间', trigger: 'blur' }],
    duration: [{ required: true, message: '请输入生产总时长', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

// ===================== 列表加载 =====================

function getList() {
  loading.value = true
  listTask(queryParams.value).then((r: any) => {
    taskList.value = r.rows || []
    total.value = r.total || 0
    loading.value = false
  }).catch(() => { loading.value = false })
}

function loadSelectOptions() {
  listWorkorder({ pageNum: 1, pageSize: 1000 }).then((r: any) => { workorderOptions.value = r.rows || [] })
  listAllProcess().then((r: any) => { processOptions.value = r.data || [] })
}

// ===================== 工单变更 → 级联加载工序 & 产品信息 =====================

function onWorkorderChange(workorderId: number) {
  if (!workorderId) return
  form.value.processId = undefined as any
  getWorkorder(workorderId).then((r: any) => {
    const wo = r.data
    if (!wo) return
    form.value.itemId = wo.itemId
    form.value.itemName = wo.itemName
    form.value.itemCode = wo.itemCode
    form.value.unitOfMeasure = wo.unitOfMeasure || wo.unitName
    updateTaskNameHint()
    if (wo.routeProductId) {
      listRouteProduct({ pageSize: 100 }).then((rpRes: any) => {
        const rp = (rpRes.rows || []).find((p: any) => p.recordId === wo.routeProductId)
        if (rp?.routeId) {
          listRouteProcessByRouteId(rp.routeId).then((procRes: any) => {
            const procList = procRes.data || []
            if (procList.length > 0) {
              processOptions.value = procList.map((p: any) => ({
                processId: p.processId, processName: p.processName || p.processCode, processCode: p.processCode
              }))
            }
          })
        }
      })
    }
  })
}

// ===================== 任务名称自动生成 =====================

function updateTaskNameHint() {
  const qty = form.value.quantity as number
  const itemName = form.value.itemName
  const unit = form.value.unitOfMeasure || form.value.unitName || ''
  if (itemName && qty) form.value.taskName = `${itemName}【${qty}】${unit}`
}

// ===================== 结束时间自动计算 (endTime = startTime + duration * 8h) =====================

function calculateEndTime() {
  const st = form.value.startTime as string
  const dur = form.value.duration as number
  if (!st || dur == null || dur <= 0) { form.value.endTime = undefined as any; return }
  const startDate = new Date(st)
  const endMs = startDate.getTime() + dur * 8 * 3600 * 1000
  const endDate = new Date(endMs)
  const pad = (n: number) => n.toString().padStart(2, '0')
  form.value.endTime = `${endDate.getFullYear()}-${pad(endDate.getMonth() + 1)}-${pad(endDate.getDate())} ${pad(endDate.getHours())}:${pad(endDate.getMinutes())}:${pad(endDate.getSeconds())}`
}

// ===================== 工单选择 =====================
function handleWorkorderSelect() { woSelectRef.value?.open() }
function onWorkorderSelected(row: any) {
  if (!row) return
  form.value.workorderId = row.workorderId
  form.value.workorderName = row.workorderName
  form.value.itemId = row.productId
  form.value.itemName = row.productName
  form.value.itemCode = row.productCode
  form.value.unitOfMeasure = row.unitOfMeasure
  form.value.unitName = row.unitName
  onWorkorderChange(row.workorderId)
}

// ===================== 工序选择 =====================

function onProcessChange(_processId: number) {
  // 工序变更时同步信息（如需要可在此扩展）
}

// ===================== 工作站选择 =====================

function handleWorkstationSelect() { wsSelectRef.value?.open() }

function onWorkstationSelected(row: any) {
  if (row) {
    form.value.workstationId = row.workstationId
    form.value.workstationCode = row.workstationCode
    form.value.workstationName = row.workstationName
  }
}

// ===================== 任务编码自动生成 =====================

function handleAutoGenChange(val: boolean) {
  if (val) {
    genSerialCode('TASK_CODE').then((r: any) => { form.value.taskCode = r.data }).catch(() => { form.value.taskCode = 'TSK' + Date.now() })
  } else {
    form.value.taskCode = ''
  }
}

// ===================== CRUD =====================

function cancel() { open.value = false; reset() }

function reset() {
  form.value = {} as any
  autoGenFlag.value = false
  if (formRef.value) proxy.resetForm('formRef')
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }

function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((i: any) => i.taskId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  reset(); loadSelectOptions(); open.value = true; title.value = '新增排产任务'; optType.value = 'add'
}

function handleUpdate(row?: any) {
  reset(); loadSelectOptions()
  const id = row?.taskId || ids.value[0]
  if (!id) { proxy.$modal.msgWarning('请选择一条记录'); return }
  getTask(id).then((r: any) => {
    form.value = r.data || {}
    open.value = true; title.value = '修改排产任务'; optType.value = 'edit'
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    if (form.value.taskId != null) {
      updateTask(form.value).then(() => { proxy.$modal.msgSuccess('修改成功'); open.value = false; getList() })
    } else {
      addTask(form.value).then(() => { proxy.$modal.msgSuccess('新增成功'); open.value = false; getList() })
    }
  })
}

function handleDelete(row?: any) {
  const _ids = row?.taskId ? [row.taskId] : ids.value
  if (!_ids || _ids.length === 0) { proxy.$modal.msgWarning('请选择要删除的记录'); return }
  proxy.$modal.confirm('是否确认删除所选排产任务？').then(() => delTask(_ids.join(','))).then(() => {
    getList(); proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

// ===================== 状态操作 =====================

function handleDispatch(row: any) {
  proxy.$modal.confirm(`确认下发任务"${row.taskName || row.taskCode}"到工作站生产？`).then(() => {
    dispatchTask(row.taskId).then(() => { proxy.$modal.msgSuccess('下发成功'); getList() })
  }).catch(() => {})
}

function handleComplete(row: any) {
  proxy.$modal.confirm(`确认完成任务"${row.taskName || row.taskCode}"？`).then(() => {
    completeTask(row.taskId).then(() => { proxy.$modal.msgSuccess('完成'); getList() })
  }).catch(() => {})
}

function handleCancelTask(row: any) {
  proxy.$modal.confirm(`确认取消任务"${row.taskName || row.taskCode}"？`).then(() => {
    cancelTask(row.taskId).then(() => { proxy.$modal.msgSuccess('已取消'); getList() })
  }).catch(() => {})
}

function handleExport() {
  proxy.download('/mes/pro/task/export', { ...queryParams.value }, `schedule_${Date.now()}.xlsx`)
}

getList()
</script>

<style lang="scss" scoped>
.app-container { padding: 16px; }
.mb8 { margin-bottom: 8px; }
</style>
