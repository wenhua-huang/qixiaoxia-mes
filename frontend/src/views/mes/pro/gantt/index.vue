<template>
  <div class="gantt-page">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="生产工单" prop="workorderId">
        <el-select v-model="queryParams.workorderId" placeholder="输入编号/名称搜索" clearable
          filterable remote :remote-method="searchWorkorders" :loading="woLoading"
          @update:model-value="onWorkorderChange" style="width:340px">
          <el-option v-for="wo in workorderList" :key="wo.workorderId"
            :label="wo.workorderCode + ' — ' + wo.productName"
            :value="wo.workorderId" />
        </el-select>
      </el-form-item>
      <el-form-item label="工作站" prop="workstationId">
        <el-select v-model="queryParams.workstationId" placeholder="输入工作站搜索" clearable
          filterable remote :remote-method="searchWorkstations" :loading="wsLoading"
          @change="loadGanttData" style="width:220px">
          <el-option v-for="ws in workstationList" :key="ws.workstationId"
            :label="ws.workstationName" :value="ws.workstationId" />
        </el-select>
      </el-form-item>
      <el-form-item label="视角">
        <el-radio-group v-model="viewMode" @change="loadGanttData" size="small">
          <el-radio-button value="workorder">工单</el-radio-button>
          <el-radio-button value="workstation">工作站</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadGanttData" :icon="Search">查询</el-button>
        <el-button @click="resetQuery" :icon="Refresh">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain @click="loadGanttData" :icon="Refresh" :loading="loading">刷新</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain @click="autoSchedule" :loading="scheduling" v-hasPermi="['mes:pro:gantt:schedule']">
          自动排产
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAddTask" v-hasPermi="['mes:pro:gantt:edit']" :disabled="!queryParams.workorderId">
          新增任务
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" @click="handleDeleteTask" v-hasPermi="['mes:pro:gantt:edit']" :disabled="!selectedTask.id">
          删除任务
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="loadGanttData" />
    </el-row>

    <div class="gantt-layout">
      <WorkOrderQueue ref="queueRef" @select="onQueueSelect" @scheduled="loadGanttData" />
      <div class="gantt-main">
        <GanttChart ref="ganttRef" :tasks="ganttTasks" :loading="loading"
          @select="onTaskSelect" @bar-move="onBarMove" />
      </div>
    </div>

    <UtilizationBar :tasks="ganttTasks" />

    <SnapShotPanel ref="snapRef" :workorder-id="queryParams.workorderId" @refresh="loadGanttData" />

    <!-- 任务详情/编辑弹窗 -->
    <el-dialog :title="taskDialogTitle" v-model="detailOpen" width="500px" append-to-body @close="onDialogClose">
      <el-form :model="taskForm" label-width="110px" :disabled="taskDialogMode==='view'">
        <el-form-item label="工序">
          <el-select v-model="taskForm.processId" style="width:100%" filterable
            :disabled="taskDialogMode==='view' || !!taskForm.taskId"
            placeholder="请选择工序"
            @change="onProcessChange">
            <el-option v-for="p in processOptions" :key="p.processId"
              :label="p.processName" :value="p.processId" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作站">
          <el-select v-model="taskForm.workstationId" style="width:100%" filterable clearable
            :disabled="taskDialogMode==='view'"
            @focus="onWorkstationFocus">
            <el-option v-for="ws in filteredWorkstationList" :key="ws.workstationId"
              :label="ws.workstationName" :value="ws.workstationId" />
          </el-select>
        </el-form-item>
        <el-form-item label="排产数量">
          <el-input-number v-model="taskForm.quantity" :min="1" style="width:100%" :disabled="taskDialogMode==='view'" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="taskForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" :disabled="taskDialogMode==='view'" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="taskForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" :disabled="taskDialogMode==='view'" />
        </el-form-item>
        <el-form-item label="计划时长(分钟)">
          <el-input-number v-model="taskForm.duration" :min="1" style="width:100%" :disabled="taskDialogMode==='view'" />
        </el-form-item>
        <el-form-item label="调机时长(分钟)">
          <el-input-number v-model="taskForm.setupDuration" :min="0" style="width:100%" :disabled="taskDialogMode==='view'" />
        </el-form-item>
        <el-form-item label="状态">
          <el-tag :type="statusMap[selectedTask.status]?.type">{{ statusMap[selectedTask.status]?.label || selectedTask.status }}</el-tag>
        </el-form-item>
        <el-form-item label="已生产">{{ selectedTask.quantityProduced || 0 }}</el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="taskForm.colorCode" :disabled="taskDialogMode==='view'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="taskDialogMode==='view'" type="primary" @click="taskDialogMode='edit'" v-hasPermi="['mes:pro:gantt:edit']">编辑</el-button>
        <el-button v-if="taskDialogMode==='edit'" type="primary" @click="submitTaskEdit" :loading="taskSaving">保存</el-button>
        <el-button @click="detailOpen=false">{{ taskDialogMode==='view' ? '关闭' : '取消' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ProGantt">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getWorkOrderGantt, getWorkstationGantt } from '@/api/mes/pro/gantt'
import { listWorkorder, getWorkorderDetail } from '@/api/mes/pro/workorder'
import { listWorkstation } from '@/api/mes/md/workstation'
import { addTask, updateTask, delTask } from '@/api/mes/pro/task'
import GanttChart from '@/components/GanttChart/index.vue'
import SnapShotPanel from './SnapShotPanel.vue'
import WorkOrderQueue from './WorkOrderQueue.vue'
import UtilizationBar from './UtilizationBar.vue'
import type { GanttTask } from '@/types/api/mes/pro/gantt'

const route = useRoute()

const showSearch = ref(true)
const loading = ref(false)
const scheduling = ref(false)
const woLoading = ref(false)
const wsLoading = ref(false)
const ganttRef = ref()
const queueRef = ref()
const ganttTasks = ref<GanttTask[]>([])
const workorderList = ref<any[]>([])
const workstationList = ref<any[]>([])

const viewMode = ref<'workorder'|'workstation'>('workorder')
const queryParams = reactive({
  workorderId: null as number | null,
  workstationId: null as number | null
})

// 任务详情弹窗
const detailOpen = ref(false)
const selectedTask = ref<any>({})

const statusMap: Record<string, any> = {
  PREPARE: { label: '待排产', type: 'info' },
  NORMAL: { label: '正常', type: 'success' },
  PRODUCING: { label: '生产中', type: 'warning' },
  COMPLETED: { label: '已完成', type: '' },
  PAUSED: { label: '暂停', type: 'danger' },
  CANCEL: { label: '取消', type: 'info' }
}

// 任务编辑弹窗状态
const taskDialogMode = ref<'view'|'edit'>('view')
const taskSaving = ref(false)
const routeProcesses = ref<any[]>([])  // 当前工单的工艺路线工序列表
const taskForm = reactive({
  taskId: null as number | null,
  processId: null as number | null,
  processName: '',
  workstationId: null as number | null,
  workstationName: '',
  quantity: 1,
  startTime: '' as string | null,
  endTime: '' as string | null,
  duration: 1,
  setupDuration: 0,
  colorCode: '#409eff',
})
const taskDialogTitle = computed(() => {
  const prefix = taskDialogMode.value === 'view' ? '任务详情 — ' : '编辑任务 — '
  return prefix + (selectedTask.value.processName || selectedTask.value.text || '')
})

// 工序选项：优先从 routeProcesses 获取，否则从当前甘特图任务中提取
const processOptions = computed(() => {
  if (routeProcesses.value.length > 0) {
    return routeProcesses.value.map((p: any) => ({
      processId: p.processId, processName: p.processName || p.processCode, processCode: p.processCode
    }))
  }
  // 从已加载的甘特图任务中提取去重工序
  const seen = new Set<number>()
  const list: any[] = []
  for (const p of ganttTasks.value) {
    for (const c of (p.children || [])) {
      const pid = (c as any).processId
      if (pid && !seen.has(pid)) {
        seen.add(pid)
        list.push({ processId: pid, processName: c.processName || `工序#${pid}` })
      }
    }
  }
  return list
})

// 按工序过滤工作站列表（#4）
const filteredWorkstationList = computed(() => {
  return workstationList.value
})

// 工序变更时更新 taskForm
function onProcessChange(processId: number | null) {
  if (!processId) return
  const proc = processOptions.value.find((p: any) => p.processId === processId)
  if (proc) {
    taskForm.processId = proc.processId
    taskForm.processName = proc.processName || ''
  }
}

// 对话框关闭时重置状态
function onDialogClose() {
  taskDialogMode.value = 'edit'
  resetTaskForm()
}

function resetTaskForm() {
  taskForm.taskId = null
  taskForm.processId = null
  taskForm.processName = ''
  taskForm.workstationId = null
  taskForm.workstationName = ''
  taskForm.quantity = 1
  taskForm.startTime = null
  taskForm.endTime = null
  taskForm.duration = 1
  taskForm.setupDuration = 0
  taskForm.colorCode = '#409eff'
}

/** 将 ISO 格式时间转换为 yyyy-MM-dd HH:mm:ss（#6） */
function normalizeTime(isoStr: string | null | undefined): string | null {
  if (!isoStr) return null
  return isoStr.replace('T', ' ')
}

/** 将时间字符串解析为 Date（强制本地时间） */
function parseTimeStr(str: string | null): Date | null {
  if (!str) return null
  // 用正则提取各部分，避免 new Date('yyyy-MM-ddTHH:mm:ss') 的跨浏览器歧义
  const m = str.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})/)
  if (!m) return null
  return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6])
}

/** 将 Date 格式化为 yyyy-MM-dd HH:mm:ss（本地时间） */
function formatLocalTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

// ── 时间联动：开始/结束 → 自动算时长，时长变更 → 自动算结束 ──
let _timeLock = false  // 防止循环触发

watch(() => taskForm.startTime, (val) => {
  if (_timeLock || !val || !taskForm.endTime) return
  const s = parseTimeStr(val), e = parseTimeStr(taskForm.endTime)
  if (s && e) {
    _timeLock = true
    taskForm.duration = Math.max(1, Math.round((e.getTime() - s.getTime()) / 60000))
    _timeLock = false
  }
})

watch(() => taskForm.endTime, (val) => {
  if (_timeLock || !val || !taskForm.startTime) return
  const s = parseTimeStr(taskForm.startTime), e = parseTimeStr(val)
  if (s && e) {
    _timeLock = true
    taskForm.duration = Math.max(1, Math.round((e.getTime() - s.getTime()) / 60000))
    _timeLock = false
  }
})

watch(() => taskForm.duration, (val) => {
  if (_timeLock || !val || !taskForm.startTime) return
  const s = parseTimeStr(taskForm.startTime)
  if (s) {
    _timeLock = true
    taskForm.endTime = formatLocalTime(new Date(s.getTime() + val * 60000))
    _timeLock = false
  }
})

onMounted(async () => {
  // 从URL query参数获取workorderId（来自工单列表的"排产"按钮跳转）
  const rawId = route.query.workorderId
  const woId = Array.isArray(rawId) ? rawId[0] : rawId  // #7 防数组NaN
  if (woId) {
    queryParams.workorderId = Number(woId)
    // 加载工单信息填充到搜索下拉框，同时缓存路线工序供新增任务使用
    try {
      const res: any = await getWorkorderDetail(Number(woId))
      const wo = res?.data?.workorder || res?.data
      if (wo) {
        workorderList.value = [{
          workorderId: wo.workorderId,
          workorderCode: wo.workorderCode,
          productName: wo.productName
        }]
      }
      // 缓存路线工序列表
      routeProcesses.value = res?.data?.routeProcesses || []
    } catch { /* ignore */ }
    loadGanttData()
  }
})

// remote搜索工单（300ms防抖，name LIKE + 排除取消/完成）
let woTimer: any = null
function searchWorkorders(kw: string) {
  if (!kw) { workorderList.value = []; return }
  clearTimeout(woTimer)
  woTimer = setTimeout(async () => {
    woLoading.value = true
    try {
      const res: any = await listWorkorder({ workorderCode: kw, pageSize: 20 })
      workorderList.value = res?.rows || []
    } finally { woLoading.value = false }
  }, 300)
}

// remote搜索工作站（300ms防抖）
let wsTimer: any = null
function searchWorkstations(kw: string) {
  if (!kw) { workstationList.value = []; return }
  clearTimeout(wsTimer)
  wsTimer = setTimeout(async () => {
    wsLoading.value = true
    try {
      const res: any = await listWorkstation({ workstationName: kw, pageSize: 20 } as any)
      workstationList.value = res?.rows || []
    } finally { wsLoading.value = false }
  }, 300)
}

function onQueueSelect(id: number) {
  queryParams.workorderId = id
  queryParams.workstationId = null
  queueRef.value?.setActive(id)
  loadGanttData()
}

function onWorkorderChange(val: number | null) {
  queryParams.workorderId = val
  queueRef.value?.setActive(val)
  // 加载路线工序（供新增任务时选择）
  if (val) {
    getWorkorderDetail(val).then((res: any) => {
      routeProcesses.value = res?.data?.routeProcesses || []
    }).catch(() => {})
  } else {
    routeProcesses.value = []
  }
  loadGanttData()
}

async function loadGanttData() {
  loading.value = true
  try {
    if (queryParams.workorderId) {
      const [ganttRes, matRes] = await Promise.all([
        getWorkOrderGantt(queryParams.workorderId),
        request({ url: `/mes/pro/workorder/checkMaterial/${queryParams.workorderId}`, method: 'get' }).catch(() => null)
      ])
      ganttTasks.value = ganttRes?.data?.tasks || []
      // 物料状态注入甘特图数据
      if (matRes?.data?.length) {
        const allOk = (matRes.data as any[]).every((m: any) => m.sufficient)
        const status = allOk ? 'ok' : 'shortage'
        const shortageNames = (matRes.data as any[]).filter((m: any) => !m.sufficient).map((m: any) => m.itemName).join('、')
        ganttTasks.value.forEach(p => p.materialStatus = { status, shortageNames })
      } else if (matRes?.data) {
        ganttTasks.value.forEach(p => p.materialStatus = { status: 'ok', shortageNames: '' })
      }

      // 自动排产：无任务时自动调用排产API，用户可见结果
      const hasTasks = ganttTasks.value.some(p => (p.children || []).length > 0)
      if (!hasTasks) {
        ElMessage.info('正在自动排产…')
        try {
          await request({ url: `/mes/pro/gantt/schedule/${queryParams.workorderId}`, method: 'post' })
          const retryRes = await getWorkOrderGantt(queryParams.workorderId)
          ganttTasks.value = retryRes?.data?.tasks || []
          const count = ganttTasks.value[0]?.children?.length || 0
          if (count > 0) {
            ElMessage.success(`排产完成，共 ${count} 个工序任务`)
          } else {
            ElMessage.warning('排产完成但无任务，请检查工单工艺路线是否配置完整')
          }
        } catch {
          ElMessage.error('自动排产失败，请手动点击「自动排产」按钮')
        }
      }
    } else if (queryParams.workstationId && viewMode.value === 'workstation') {
      const res = await getWorkstationGantt(queryParams.workstationId)
      ganttTasks.value = (res as any)?.data?.tasks || []
    } else {
      ganttTasks.value = []
    }
    ganttRef.value?.render()
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.workorderId = null
  queryParams.workstationId = null
  ganttTasks.value = []
}

function onTaskSelect(task: GanttTask) {
  selectedTask.value = task
  taskDialogMode.value = 'edit'
  // 填充表单数据
  taskForm.taskId = Number(task.id) || null
  taskForm.processId = (task as any).processId || null         // #5 填充工序
  taskForm.processName = task.processName || (task as any).processName || ''
  taskForm.workstationId = (task as any).workstationId || null
  taskForm.workstationName = (task as any).workstationName || ''
  taskForm.quantity = task.quantity || 1
  taskForm.startTime = normalizeTime(task.start)               // #6 ISO→空格格式
  taskForm.endTime = normalizeTime(task.end)
  taskForm.duration = task.duration || 1
  taskForm.setupDuration = (task as any).setupDuration || 0
  taskForm.colorCode = task.colorCode || '#409eff'
  detailOpen.value = true
}

// 工作站下拉框聚焦时加载列表
function onWorkstationFocus() {
  if (workstationList.value.length === 0) {
    searchWorkstations('')
  }
}

// 新增任务
function handleAddTask() {
  if (!queryParams.workorderId) return
  // 确保有工序可选
  if (processOptions.value.length === 0) {
    ElMessage.warning('请先加载工单的甘特图数据，确保工艺路线有工序')
    return
  }
  // 进入编辑模式：taskId=null 表示新增，工序选择器启用
  selectedTask.value = { processName: '新增任务', text: '新增任务', status: 'NORMAL' }
  taskDialogMode.value = 'edit'
  resetTaskForm()
  // 从甘特图 project 获取默认数量
  const project = ganttTasks.value[0]
  taskForm.quantity = (project as any).quantity || 1
  detailOpen.value = true
}

// 删除选中任务
async function handleDeleteTask() {
  const taskId = selectedTask.value.id
  if (!taskId) return
  try {
    await ElMessageBox.confirm('确认删除该排产任务？', '提示', { type: 'warning' })
  } catch { return }
  try {
    await delTask(Number(taskId))
    ElMessage.success('删除成功')
    detailOpen.value = false
    selectedTask.value = {}
    loadGanttData()
  } catch { ElMessage.error('删除失败') }
}

// 提交任务编辑
async function submitTaskEdit() {
  taskSaving.value = true
  try {
    const payload: any = {
      workorderId: queryParams.workorderId,
      processId: taskForm.processId,           // #2 添加工序关联
      processName: taskForm.processName,
      quantity: taskForm.quantity,
      startTime: taskForm.startTime,
      endTime: taskForm.endTime,
      duration: taskForm.duration,
      setupDuration: taskForm.setupDuration,
      colorCode: taskForm.colorCode,
    }
    if (taskForm.workstationId) {
      const ws = workstationList.value.find(w => w.workstationId === taskForm.workstationId)
      payload.workstationId = taskForm.workstationId
      payload.workstationCode = ws?.workstationCode || ''
      payload.workstationName = ws?.workstationName || ''
    }
    if (taskForm.taskId) {
      payload.taskId = taskForm.taskId
      await updateTask(payload)
    } else {
      await addTask(payload)
    }
    ElMessage.success(taskForm.taskId ? '修改成功' : '新增成功')
    detailOpen.value = false
    onDialogClose()
    selectedTask.value = {}
    loadGanttData()
  } catch { ElMessage.error('保存失败') }
  finally { taskSaving.value = false }
}

async function autoSchedule() {
  if (!queryParams.workorderId) return
  scheduling.value = true
  try {
    await request({ url: '/mes/pro/gantt/schedule/' + queryParams.workorderId, method: 'post' })
    ElMessage.success('排产计算完成')
    loadGanttData()
  } catch { ElMessage.error('排产失败') }
  finally { scheduling.value = false }
}

async function onBarMove(task: GanttTask, newStart: string, newEnd: string) {
  try {
    const res: any = await request({
      url: `/mes/pro/gantt/task/${task.id}/move`,
      method: 'put',
      params: { newStart, newEnd }
    })
    // move API 直接返回最新甘特图数据（含级联结果）
    if (res?.data?.tasks) {
      ganttTasks.value = res.data.tasks
    } else {
      // 兜底：本地更新当前任务
      for (const p of ganttTasks.value) {
        for (const c of p.children || []) {
          if (c.id === task.id) {
            c.start = newStart; c.end = newEnd
            c.duration = Math.round((new Date(newEnd).getTime() - new Date(newStart).getTime()) / 60000)
          }
        }
      }
    }
    ganttRef.value?.render()
    // 更新弹窗数据
    const updated = findTask(task.id)
    if (updated) { selectedTask.value = updated; detailOpen.value = true }
  } catch { /* 静默 */ }
}

function findTask(id: string): GanttTask|undefined {
  for (const p of ganttTasks.value)
    for (const c of p.children || [])
      if (c.id === id) return c
}
</script>

<style scoped lang="scss">
.gantt-page { padding: 8px; background: #fff; }
.mb8 { margin-bottom: 8px; }
.gantt-layout { display: flex; gap: 0; }
.gantt-main { flex: 1; overflow: hidden; }
</style>
