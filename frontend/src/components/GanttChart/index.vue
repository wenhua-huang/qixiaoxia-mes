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
        <div v-for="(r,i) in rows" :key="'l'+i" class="gc-left-cell" :class="{proj:r.t==='p'}"
          :style="{height:rowH+'px'}">
          <span v-if="r.t==='p' && (r.raw as any).materialStatus?.status==='shortage'"
            :title="'缺料：' + ((r.raw as any).materialStatus?.shortageNames || '')" style="cursor:help">🔴</span>
          <span v-else-if="r.t==='p' && (r.raw as any).materialStatus?.status==='ok'"
            :title="物料齐套" style="cursor:help">🟢</span>
          <span :style="{paddingLeft:r.t==='p'?'4px':'20px'}">{{ r.text }}</span>
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
            <div v-if="r.t==='t' && r.s && r.e" class="gc-bar"
              :class="{constrained: constrained}"
              :style="{left:posX(r.s)+'px',width:Math.max(posX(r.e)-posX(r.s),4)+'px',background:r.c}"
              @mouseup="onBarClick($event, r)"
              @mousedown="onBarDown($event, r)" >
              <div class="gc-resize-l" @mousedown.stop="onResizeDown($event, r, 'left')" />
              <div class="gc-resize-r" @mousedown.stop="onResizeDown($event, r, 'right')" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { GanttTask } from '@/types/api/mes/pro/gantt'
import request from '@/utils/request'

const props = defineProps<{ tasks: GanttTask[]; loading?: boolean }>()
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
interface Row { id: string; text: string; t: 'p'|'t'; s: Date|null; e: Date|null; c: string; raw: any; minStartMs?: number }
const rows = ref<Row[]>([])

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

// ---- 构建 ----
function buildRows() {
  const rr: Row[] = []
  for (const p of props.tasks) {
    rr.push({ id: p.id, text: p.text, t: 'p', s: null, e: null, c: '', raw: p })
    if (p.children) {
      let prevEndMs = 0
      let isFirst = true
      for (const c of p.children) {
        const row: Row = {
          id: c.id, text: c.processName || c.text, t: 't',
          s: c.start ? new Date(c.start) : null,
          e: c.end ? new Date(c.end) : null,
          c: c.colorCode || '#409eff', raw: c,
          // 首工序无约束，后续工序不能早于前道结束时间
          minStartMs: isFirst ? 0 : prevEndMs
        }
        if (c.end) prevEndMs = new Date(c.end).getTime()
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

// ---- init ----
onMounted(render)
watch(() => props.tasks, () => nextTick(render), { deep: true })

// ---- 拖拽/拉伸 ----
let dragRow: Row|null = null
let dragType: 'move'|'resize-l'|'resize-r' = 'move'
let dragStartX = 0
let dragOrigStart = 0
let dragOrigEnd = 0
let dragNewStart = 0
let dragNewEnd = 0
let dragMoved = false
let constraintShown = false
const constrained = ref(false)  // 越界闪烁

function onBarClick(_e: MouseEvent, row: Row) {
  if (!dragMoved) emit('select', row.raw)
}
function onBarDown(e: MouseEvent, row: Row) { startDrag(e, row, 'move') }
function onResizeDown(e: MouseEvent, row: Row, side: 'left'|'right') { startDrag(e, row, side==='left'?'resize-l':'resize-r') }

function startDrag(e: MouseEvent, row: Row, type: 'move'|'resize-l'|'resize-r') {
  if (!row.s || !row.e || !row.raw) return
  dragType = type; dragRow = row; dragMoved = false
  dragStartX = e.clientX
  dragOrigStart = dragNewStart = row.s.getTime()
  dragOrigEnd = dragNewEnd = row.e.getTime()
  document.addEventListener('mousemove', onBarMove)
  document.addEventListener('mouseup', onBarUp)
  e.preventDefault()
}

function onBarMove(e: MouseEvent) {
  if (!dragRow) return
  dragMoved = true
  const msDelta = (e.clientX - dragStartX) / colW.value * msPerUnit.value
  if (dragType === 'resize-l') {
    dragNewStart = Math.min(dragOrigStart + msDelta, dragNewEnd - 60000)
  } else if (dragType === 'resize-r') {
    dragNewEnd = Math.max(dragOrigEnd + msDelta, dragNewStart + 60000)
  } else {
    dragNewStart = dragOrigStart + msDelta
    dragNewEnd = dragOrigEnd + msDelta
  }
  // 前置约束：不能越过前道工序结束时间
  if (dragRow.minStartMs && dragRow.minStartMs > 0 && dragNewStart < dragRow.minStartMs) {
    dragNewStart = dragRow.minStartMs
    dragNewEnd = dragRow.minStartMs + (dragOrigEnd - dragOrigStart)
    constrained.value = true
    setTimeout(() => constrained.value = false, 800)
    if (!constraintShown) { constraintShown = true; setTimeout(() => constraintShown = false, 2000)
      ElMessage({ message: '受前置工序约束，不能往前移动', type: 'warning', duration: 1500 }) }
  }
  dragRow.s = new Date(dragNewStart)
  dragRow.e = new Date(dragNewEnd)
  rows.value = [...rows.value]
}

function onBarUp() {
  document.removeEventListener('mousemove', onBarMove)
  document.removeEventListener('mouseup', onBarUp)
  if (!dragRow?.raw) { dragRow = null; return }
  if (Math.abs(dragOrigStart - dragNewStart) < 300000 && Math.abs(dragOrigEnd - dragNewEnd) < 300000) {
    dragRow = null; return
  }
  const localStr = (ms: number) => {
    const d = new Date(ms)
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`
  }
  emit('barMove', dragRow.raw, localStr(dragNewStart), localStr(dragNewEnd))
  dragRow = null
}

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

defineExpose({ render })
</script>

<style scoped lang="scss">
.gc-root { border:1px solid #e4e7ed; border-radius:4px; background:#fff; }
.gc-toolbar { display:flex; align-items:center; gap:8px; padding:6px 10px; border-bottom:1px solid #ebeef5; background:#fafafa; b { margin-right:auto; font-size:13px; } }
.gc-main { display:flex; }
.gc-left { flex-shrink:0; border-right:1px solid #e4e7ed; }
.gc-left-hd { height:32px; line-height:32px; padding:0 8px; font-weight:600; font-size:12px; background:#f5f7fa; border-bottom:1px solid #dcdfe6; }
.gc-left-cell { display:flex; align-items:center; padding:0 4px; font-size:12px; border-bottom:1px solid #f2f3f5; overflow:hidden; white-space:nowrap; &.proj { font-weight:600; background:#fafafa; } }
.gc-right { flex:1; overflow:auto; max-height:calc(100vh - 280px); }
.gc-time-hd { height:32px; position:sticky; top:0; z-index:2; background:#f5f7fa; border-bottom:1px solid #dcdfe6; }
.gc-time-cell { position:absolute; top:0; height:100%; line-height:32px; text-align:center; font-size:10px; border-right:1px solid #e4e7ed; color:#606266; &.we { background:#fef0f0; color:#f56c6c; } }
.gc-grid { position:relative; }
.gc-row { position:relative; border-bottom:1px solid #f2f3f5; }
.gc-cell { position:absolute; top:0; height:100%; border-right:1px solid #f8f8f8; &.we { background:#fefafa; } }
.gc-bar { position:absolute; top:3px; height:24px; border-radius:4px; cursor:grab; opacity:.9; z-index:1; display:flex; align-items:center; &:active { cursor:grabbing; } &:hover { opacity:1; box-shadow:0 2px 6px rgba(0,0,0,.2); } &.constrained { animation: flash .3s ease-in-out 2; border:2px solid #f56c6c; } }
@keyframes flash { 0%,100% { opacity:.9; } 50% { opacity:.4; border-color:#f56c6c; } }
.gc-resize-l, .gc-resize-r { width:6px; height:100%; position:absolute; top:0; cursor:ew-resize; }
.gc-resize-l { left:0; border-radius:4px 0 0 4px; }
.gc-resize-r { right:0; border-radius:0 4px 4px 0; }
</style>
