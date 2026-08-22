<template>
  <el-dialog
    v-model="visible"
    :title="`工单进度 - ${detail?.workorderCode || ''}`"
    :fullscreen="true"
    append-to-body
    :close-on-click-modal="false"
    width="100%"
  >
    <div v-loading="loading">
      <!-- 工单头信息 -->
      <el-descriptions :column="4" border size="small" class="wo-head">
        <el-descriptions-item label="工单">{{ detail?.workorderCode }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ detail?.productName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag>{{ statusText(detail?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="延期风险">
          <el-tag v-if="detail" :type="delayType(detail.delayLevel)">{{ delayText(detail.delayLevel) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="计划量">{{ detail?.quantity }}</el-descriptions-item>
        <el-descriptions-item label="已产量">{{ detail?.quantityProduced }}</el-descriptions-item>
        <el-descriptions-item label="交期">{{ fmtDate(detail?.requestDate) }}</el-descriptions-item>
        <el-descriptions-item label="完工时间">{{ fmtDateTime(detail?.finishDate) }}</el-descriptions-item>
        <el-descriptions-item label="计划开始/结束">
          {{ fmtDateTime(detail?.planStartTime) }} ~ {{ fmtDateTime(detail?.planEndTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="实际开始/结束">
          {{ fmtDateTime(detail?.actualStartTime) }} ~ {{ fmtDateTime(detail?.actualEndTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="完成率">
          <el-progress :percentage="detail?.completionRate ?? 0" :stroke-width="14" style="width:160px" />
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="activeTab" class="prog-tabs" @tab-change="onTabChange">
        <el-tab-pane label="工序进度" name="process">
          <ProcessProgressTable :rows="detail?.processes || []" />
        </el-tab-pane>
        <el-tab-pane label="流转卡(子工单)" name="card">
          <CardSuborderTable :rows="detail?.cards || []" />
        </el-tab-pane>
        <el-tab-pane label="甘特图（实际叠加）" name="gantt">
          <div v-loading="ganttLoading" class="gantt-wrap">
            <GanttChart ref="ganttRef" :tasks="ganttTasks" :readonly="true" :loading="ganttLoading" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="goSchedule">前往排产甘特</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { parseTime } from '@/utils/ruoyi'
import GanttChart from '@/components/GanttChart/index.vue'
import { getWorkorderProgress } from '@/api/mes/pro/progress'
import { getWorkOrderGantt } from '@/api/mes/pro/gantt'
import { statusText, delayText, delayType } from '@/utils/mes/progress'
import ProcessProgressTable from './ProcessProgressTable.vue'
import CardSuborderTable from './CardSuborderTable.vue'

const props = defineProps<{ modelValue: boolean; workorderId: number | null }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const router = useRouter()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const detail = ref<any>(null)
const ganttTasks = ref<any[]>([])
const ganttLoading = ref(false)
const ganttRef = ref()
const activeTab = ref('process')
let reqSeq = 0

watch(
  () => [props.modelValue, props.workorderId],
  async ([open, id]) => {
    if (open && id) {
      activeTab.value = 'process'
      await loadAll(id as number)
    }
  }
)

async function loadAll(id: number) {
  const seq = ++reqSeq
  loading.value = true
  ganttLoading.value = true
  detail.value = null
  ganttTasks.value = []
  try {
    const [progRes, ganttRes] = await Promise.all([
      getWorkorderProgress(id).catch(() => null),
      getWorkOrderGantt(id).catch(() => null)
    ])
    if (seq !== reqSeq) return
    detail.value = progRes?.data || null
    ganttTasks.value = ganttRes?.data?.tasks || []
    await nextTick()
    ganttRef.value?.render()
  } finally {
    if (seq === reqSeq) {
      loading.value = false
      ganttLoading.value = false
    }
  }
}

async function onTabChange(name: string | number) {
  if (name === 'gantt') {
    await nextTick()
    ganttRef.value?.render()
  }
}

function goSchedule() {
  router.push({ path: '/mes/pro/gantt', query: { workorderId: props.workorderId as any } })
}

function fmtDate(v: any): string {
  if (!v) return '—'
  return parseTime(v, '{y}-{m}-{d}')
}

function fmtDateTime(v: any): string {
  if (!v) return '—'
  return parseTime(v, '{y}-{m}-{d} {h}:{i}')
}
</script>

<style scoped>
.wo-head { margin-bottom: 12px; }
.prog-tabs { margin-top: 4px; }
.gantt-wrap { min-height: 500px; border: 1px solid #ebeef5; border-radius: 4px; padding: 8px; }
</style>
