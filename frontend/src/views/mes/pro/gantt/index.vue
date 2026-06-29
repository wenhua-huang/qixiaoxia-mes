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

    <!-- 任务详情弹窗 -->

    <!-- 任务详情弹窗 -->
    <el-dialog :title="'任务详情 — ' + (selectedTask.processName || selectedTask.text)" v-model="detailOpen" width="450px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工序">{{ selectedTask.processName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[selectedTask.status]?.type">{{ statusMap[selectedTask.status]?.label || selectedTask.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始">{{ selectedTask.start?.slice(0,16) }}</el-descriptions-item>
        <el-descriptions-item label="结束">{{ selectedTask.end?.slice(0,16) }}</el-descriptions-item>
        <el-descriptions-item label="计划时长">{{ selectedTask.duration || '-' }} 分钟</el-descriptions-item>
        <el-descriptions-item label="排产数量">{{ selectedTask.quantity || '-' }}</el-descriptions-item>
        <el-descriptions-item label="已生产">{{ selectedTask.quantityProduced || 0 }}</el-descriptions-item>
        <el-descriptions-item label="颜色"> <span :style="{display:'inline-block',width:16,height:16,background:selectedTask.colorCode||'#409eff',borderRadius:3}"></span></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="ProGantt">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getWorkOrderGantt, getWorkstationGantt } from '@/api/mes/pro/gantt'
import { listWorkorder } from '@/api/mes/pro/workorder'
import { listWorkstation } from '@/api/mes/md/workstation'
import GanttChart from '@/components/GanttChart/index.vue'
import SnapShotPanel from './SnapShotPanel.vue'
import WorkOrderQueue from './WorkOrderQueue.vue'
import UtilizationBar from './UtilizationBar.vue'
import type { GanttTask } from '@/types/api/mes/pro/gantt'

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

onMounted(() => { /* remote搜索，无需预加载 */ })

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
  detailOpen.value = true
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
