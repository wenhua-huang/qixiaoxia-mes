<template>
  <div class="gc-root">
    <!-- 工具栏 -->
    <div class="gc-toolbar">
      <b>甘特图排产</b>
      <el-button-group size="small">
        <el-button @click="shiftRange(-1)">◀ 前</el-button>
        <el-button @click="shiftRange(1)">后 ▶</el-button>
      </el-button-group>
      <el-button-group size="small">
        <el-button :type="mode==='day'?'primary':''" @click="mode='day';render()">时</el-button>
        <el-button :type="mode==='week'?'primary':''" @click="mode='week';render()">日</el-button>
      </el-button-group>
      <el-button size="small" @click="zoom(-10)">-</el-button>
      <el-button size="small" @click="zoom(10)">+</el-button>
    </div>

    <div class="gc-main" ref="mainRef">
      <!-- 左侧 -->
      <div class="gc-left" :style="{width:leftW+'px'}">
        <div class="gc-left-hd">任务</div>
        <div v-for="(r,i) in rows" :key="'l'+i" class="gc-left-cell"
          :class="{proj:r.t==='p', lane:!!r.bars, 'lane-pending':r.laneType==='PENDING', 'lane-vendor':r.laneType==='VENDOR'}"
          :style="{height:rowH+'px'}">
          <span v-if="r.t==='p' && (r.raw as any).materialStatus?.status==='shortage'"
            :title="'缺料：' + ((r.raw as any).materialStatus?.shortageNames || '')" style="cursor:help">🔴</span>
          <span v-else-if="r.t==='p' && (r.raw as any).materialStatus?.status==='ok'"
            :title="物料齐套" style="cursor:help">🟢</span>
          <span :style="{paddingLeft:r.t==='p'||r.bars?'4px':'20px'}">{{ r.text }}</span>
        </div>
      </div>
      <!-- 右侧 -->
      <div class="gc-right" ref="rightRef" @scroll="onScroll">
        <div class="gc-time-hd" :style="{width:totalW+'px'}">
          <div v-for="(c,i) in cols" :key="'h'+i" class="gc-time-cell"
            :class="{we:c.isWE}" :style="{left:c.left+'px',width:colW+'px'}">
            {{ c.label }}
          </div>
        </div>
        <div class="gc-grid" :style="{width:totalW+'px'}">
          <div v-for="(r,i) in rows" :key="'g'+i" class="gc-row" :style="{height:rowH+'px'}">
            <div v-for="(c,j) in cols" :key="'c'+j" class="gc-cell"
              :class="{we:c.isWE}" :style="{left:c.left+'px',width:colW+'px'}" />
            <!-- 工单视图：单任务条（可拖拽/拉伸） -->
            <GanttBar v-if="r.t==='t' && r.s && r.e && !r.bars" :row="r" :pos-x="posX" :readonly="readonly"
              :constrained="constrained"
              @bar-click="(e: MouseEvent)=>onBarClick(e,r)" @bar-down="(e: MouseEvent)=>onBarDown(e,r)"
              @resize-down="(e: MouseEvent, s: 'left'|'right')=>onResizeDown(e,r,s)" />
            <!-- 机台泳道视图：一行多条任务条（仅点击派工/查看，不拖拽） -->
            <GanttBar v-for="b in (r.bars||[])" :key="'bar'+b.id" :row="b" :pos-x="posX" :readonly="true"
              @bar-click="(e: MouseEvent)=>onLaneBarClick(e,b)" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import type { GanttTask, WorkstationLane } from '@/types/api/mes/pro/gantt'
import request from '@/utils/request'
import GanttBar, { type GanttRow } from './GanttBar.vue'
import { useGanttDrag } from './useGanttDrag'
import { buildLaneRows } from './useWorkstationRows'

const props = withDefaults(defineProps<{
  tasks: GanttTask[]
  loading?: boolean
  readonly?: boolean
  /** 机台泳道数据：传入后按机台分行（一行多条任务条），与 tasks 工单视图互斥 */
  lanes?: WorkstationLane[] | null
}>(), { readonly: false, lanes: null })
const emit = defineEmits<{ (e: 'select', t: GanttTask): void; (e: 'barMove', t: GanttTask, newStart: string, newEnd: string): void }>()

// ---- 配置 ----
const leftW = 180
const rowH = 30
const mode = ref<'day'|'week'>('week')
const unitW = ref(80) // 每小时/每天 px
const colW = computed(() => mode.value === 'day' ? unitW.value : unitW.value * 2)

// 时间范围
const now = new Date()
const range = reactive({
  s: new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay() + 1),
  e: new Date(now.getFullYear(), now.getMonth(), now.getDate() - now.getDay() + 8)
})

// ---- 扁平行 ----
type Row = GanttRow
const rows = ref<Row[]>([])
const rightRef = ref<HTMLElement>()

// ---- 日历数据 ----
const dayStatusMap = ref<Map<string, boolean>>(new Map()) // date → working?
async function fetchCalendar() {
  try {
    const res: any = await request({
      url: '/mes/pro/gantt/calendar/dayStatus',
      method: 'get',
      params: {
        from: range.s.toISOString().slice(0,10),
        to: range.e.toISOString().slice(0,10)
      }
    })
    const list = res?.data || []
    const m = new Map<string, boolean>()
    for (const d of list) { m.set(d.date, d.working) }
    dayStatusMap.value = m
  } catch { /* ignore */ }
}

// ---- 时间列 ----
interface Col { label: string; left: number; isWE: boolean }
const cols = ref<Col[]>([])

// ---- 总宽度 ----
const totalW = computed(() => cols.value.length * colW.value)

// ---- px 换算 ----
const msPerUnit = computed(() => mode.value === 'day' ? 3600000 : 86400000)
function posX(d: Date): number {
  return (d.getTime() - range.s.getTime()) / msPerUnit.value * colW.value
}
// 后端日期为 yyyy-MM-dd HH:mm:ss，空格分隔在部分 Safari 不可解析，统一转 T
const parseDate = (s: string) => new Date(String(s).replace(' ', 'T'))

// ---- 构建 ----
function buildRows() {
  // 机台泳道视图：每个工作站一行、行内多条任务条
  if (props.lanes && props.lanes.length) {
    rows.value = buildLaneRows(props.lanes, parseDate)
    return
  }
  const rr: Row[] = []
  for (const p of props.tasks) {
    rr.push({ id: p.id, text: p.text, t: 'p', s: null, e: null, c: '', raw: p })
    if (p.children) {
      let prevEndMs = 0
      let isFirst = true
      for (const c of p.children) {
        const row: Row = {
          id: c.id, text: c.processName || c.text, t: 't',
          s: c.start ? parseDate(c.start) : null,
          e: c.end ? parseDate(c.end) : null,
          c: c.colorCode || '#409eff', raw: c,
          // 首工序无约束，后续工序不能早于前道结束时间
          minStartMs: isFirst ? 0 : prevEndMs,
          aS: c.actualStartTime ? parseDate(c.actualStartTime) : null,
          aE: c.actualEndTime ? parseDate(c.actualEndTime) : null,
          progress: typeof c.progressPercent === 'number' ? Math.min(100, Math.max(0, c.progressPercent)) : 0,
          delayLevel: c.delayLevel || 'NORMAL'
        }
        if (c.end) prevEndMs = parseDate(c.end).getTime()
        isFirst = false
        rr.push(row)
      }
    }
  }
  rows.value = rr
}

function buildCols() {
  const cc: Col[] = []
  const cur = new Date(range.s)
  let x = 0
  while (cur < range.e) {
    const ds = `${cur.getFullYear()}-${String(cur.getMonth()+1).padStart(2,'0')}-${String(cur.getDate()).padStart(2,'0')}`
    const working = dayStatusMap.value.get(ds)
    const isWE = working !== undefined ? !working : false  // 无数据默认工作日
    cc.push({
      label: mode.value === 'day' ? `${cur.getMonth()+1}/${cur.getDate()} ${cur.getHours()}时` : `${cur.getMonth()+1}/${cur.getDate()}`,
      left: x, isWE
    })
    x += colW.value
    mode.value === 'day' ? cur.setHours(cur.getHours() + 1) : cur.setDate(cur.getDate() + 1)
  }
  cols.value = cc
}

async function render() {
  await fetchCalendar()
  buildRows()
  buildCols()
}

/** 机台泳道视图：按 lanes 内全部任务的时间范围自动定位视窗（默认窗口只覆盖本周，8 月任务会落到视口外） */
function fitRangeToLanes() {
  let min = Infinity, max = -Infinity
  for (const lane of props.lanes || []) {
    for (const t of lane.tasks || []) {
      for (const v of [t.start, t.end, t.actualStartTime, t.actualEndTime]) {
        if (!v) continue
        const tm = parseDate(v).getTime()
        if (!Number.isNaN(tm)) { if (tm < min) min = tm; if (tm > max) max = tm }
      }
    }
  }
  if (!Number.isFinite(min)) return
  const HOUR = 3600000, DAY = 86400000
  let s = min - HOUR, e = max + HOUR
  const MAX = 60 * DAY
  if (e - s > MAX) e = s + MAX
  mode.value = e - s <= 2 * DAY ? 'day' : 'week'
  const grid = mode.value === 'day' ? HOUR : DAY
  range.s = new Date(Math.floor(s / grid) * grid)
  range.e = new Date(e)
}

// ---- init ----
onMounted(render)
watch(() => [props.tasks, props.lanes], () => nextTick(render), { deep: true })
// 机台泳道数据切换（切视角/刷新）时自动定位时间窗；引用变化才触发，用户手动翻页/缩放不受影响
watch(() => props.lanes, (lanes) => {
  if (lanes && lanes.length) { fitRangeToLanes(); nextTick(render) }
})

// 机台泳道任务条点击：直接派工/查看（不走拖拽逻辑）
function onLaneBarClick(_e: MouseEvent, bar: GanttRow) {
  if (bar.raw) emit('select', bar.raw)
}

// ---- 拖拽/拉伸（抽到 composable，保持组件 ≤300 行）----
const { constrained, onBarClick, onBarDown, onResizeDown } = useGanttDrag({
  readonly: props.readonly, colW, msPerUnit, rows,
  onSelect: (t) => emit('select', t),
  onBarMove: (t, s, e) => emit('barMove', t, s, e)
})

// ---- 交互 ----
function shiftRange(dir: number) {
  const totalDays = (range.e.getTime() - range.s.getTime()) / 86400000
  if (totalDays > 60) return
  const span = range.e.getTime() - range.s.getTime()
  range.s = new Date(range.s.getTime() + dir * span)
  range.e = new Date(range.e.getTime() + dir * span)
  render()
}
function zoom(d: number) { unitW.value = Math.max(20, Math.min(200, unitW.value + d)); render() }
let extendLock = false
function onScroll(e: Event) {
  if (extendLock) return
  const el = e.target as HTMLElement
  if (!el) return
  const span = range.e.getTime() - range.s.getTime()
  const totalDays = (range.e.getTime() - range.s.getTime()) / 86400000
  // 最大60天，防止无限延伸
  if (totalDays > 60) return
  if (el.scrollLeft + el.clientWidth >= el.scrollWidth - 10) {
    extendLock = true
    range.e = new Date(range.e.getTime() + span)
    render()
    setTimeout(() => extendLock = false, 500)
  }
  if (el.scrollLeft <= 10) {
    extendLock = true
    range.s = new Date(range.s.getTime() - span)
    render()
    setTimeout(() => extendLock = false, 500)
  }
}

/** 按任务时间范围自动定位视窗（供只读详情/进度弹窗使用） */
function fitToData() {
  let min = Infinity, max = -Infinity
  for (const p of props.tasks) {
    for (const c of p.children || []) {
      for (const v of [c.start, c.end, c.actualStartTime, c.actualEndTime]) {
        if (!v) continue
        const t = parseDate(v).getTime()
        if (!Number.isNaN(t)) { if (t < min) min = t; if (t > max) max = t }
      }
    }
  }
  if (!Number.isFinite(min)) return
  const HOUR = 3600000, DAY = 86400000
  let s = min - HOUR, e = max + HOUR
  const MAX = 60 * DAY
  if (e - s > MAX) e = s + MAX
  mode.value = e - s <= 2 * DAY ? 'day' : 'week'
  const grid = mode.value === 'day' ? HOUR : DAY
  s = Math.floor(s / grid) * grid
  range.s = new Date(s)
  range.e = new Date(e)
  render()
  nextTick(() => { if (rightRef.value) rightRef.value.scrollLeft = 0 })
}

defineExpose({ render, fitToData })
</script>

<style scoped lang="scss">
.gc-root { border:1px solid #e4e7ed; border-radius:4px; background:#fff; }
.gc-toolbar { display:flex; align-items:center; gap:8px; padding:6px 10px; border-bottom:1px solid #ebeef5; background:#fafafa; b { margin-right:auto; font-size:13px; } }
.gc-main { display:flex; }
.gc-left { flex-shrink:0; border-right:1px solid #e4e7ed; }
.gc-left-hd { height:32px; line-height:32px; padding:0 8px; font-weight:600; font-size:12px; background:#f5f7fa; border-bottom:1px solid #dcdfe6; }
.gc-left-cell { display:flex; align-items:center; padding:0 4px; font-size:12px; border-bottom:1px solid #f2f3f5; overflow:hidden; white-space:nowrap; &.proj { font-weight:600; background:#fafafa; } &.lane { font-weight:600; background:#f5f7fa; } &.lane-pending { color:#f56c6c; background:#fef0f0; } &.lane-vendor { color:#909399; background:#f4f4f5; } }
.gc-right { flex:1; overflow:auto; max-height:calc(100vh - 280px); }
.gc-time-hd { height:32px; position:sticky; top:0; z-index:2; background:#f5f7fa; border-bottom:1px solid #dcdfe6; }
.gc-time-cell { position:absolute; top:0; height:100%; line-height:32px; text-align:center; font-size:10px; border-right:1px solid #e4e7ed; color:#606266; &.we { background:#fef0f0; color:#f56c6c; } }
.gc-grid { position:relative; }
.gc-row { position:relative; border-bottom:1px solid #f2f3f5; }
.gc-cell { position:absolute; top:0; height:100%; border-right:1px solid #f8f8f8; &.we { background:#fefafa; } }
</style>
