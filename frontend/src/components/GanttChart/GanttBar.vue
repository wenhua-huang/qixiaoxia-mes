<script lang="ts">
// 扁平行数据（由父组件 buildRows 产出）
export interface GanttRow {
  id: string
  text: string
  t: 'p' | 't'
  s: Date | null
  e: Date | null
  c: string
  raw: any
  minStartMs?: number
  aS?: Date | null
  aE?: Date | null
  progress?: number
  delayLevel?: string
}
</script>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  row: GanttRow
  posX: (d: Date) => number
  readonly?: boolean
  constrained?: boolean
}>(), {
  readonly: false,
  constrained: false
})

const emit = defineEmits<{
  (e: 'barClick', ev: MouseEvent): void
  (e: 'barDown', ev: MouseEvent): void
  (e: 'resizeDown', ev: MouseEvent, side: 'left' | 'right'): void
}>()

const MIN_BAR_W = 4

function valid(d: Date | null | undefined): d is Date {
  return d instanceof Date && !isNaN(d.getTime())
}

const hasActual = computed(() => !!(valid(props.row.aS) && valid(props.row.aE)))

const planStyle = computed(() => {
  const r = props.row
  if (!valid(r.s) || !valid(r.e)) return {}
  return {
    left: props.posX(r.s) + 'px',
    width: Math.max(props.posX(r.e) - props.posX(r.s), MIN_BAR_W) + 'px',
    background: r.c
  }
})

const actualStyle = computed(() => {
  const r = props.row
  if (!valid(r.aS) || !valid(r.aE)) return {}
  return {
    left: props.posX(r.aS) + 'px',
    width: Math.max(props.posX(r.aE) - props.posX(r.aS), MIN_BAR_W) + 'px',
    background: r.c
  }
})

const progressWidth = computed(() => (props.row.progress || 0) + '%')

const riskClass = computed(() => {
  switch (props.row.delayLevel) {
    case 'WARNING': return 'risk-warning'
    case 'DELAY':
    case 'FINISHED_DELAY': return 'risk-delay'
    case 'BEHIND': return 'risk-behind'
    default: return ''
  }
})

function onBarClick(e: MouseEvent) { emit('barClick', e) }
function onBarDown(e: MouseEvent) {
  if (props.readonly) return
  emit('barDown', e)
}
function onResizeL(e: MouseEvent) {
  if (props.readonly) return
  emit('resizeDown', e, 'left')
}
function onResizeR(e: MouseEvent) {
  if (props.readonly) return
  emit('resizeDown', e, 'right')
}
</script>

<template>
  <div class="gc-bar-wrap">
    <!-- 计划条基底（浅色轨道） -->
    <div class="gc-bar"
      :class="[riskClass, { readonly: readonly, constrained: constrained }]"
      :style="planStyle"
      @mouseup="onBarClick"
      @mousedown="onBarDown">
      <div v-if="!readonly" class="gc-resize-l" @mousedown.stop="onResizeL" />
      <div v-if="!readonly" class="gc-resize-r" @mousedown.stop="onResizeR" />
      <div v-if="!hasActual" class="gc-bar-progress" :style="{ width: progressWidth }" />
    </div>
    <!-- 实际条（实色 + 进度填充） -->
    <div v-if="hasActual" class="gc-bar-actual" :style="actualStyle">
      <div class="gc-bar-progress" :style="{ width: progressWidth }" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.gc-bar-wrap { position:absolute; top:3px; left:0; right:0; height:24px; }
.gc-bar {
  position:absolute; top:0; height:100%; border-radius:4px; cursor:grab; opacity:.35; z-index:1;
  display:flex; align-items:center;
  &:active { cursor:grabbing; }
  &:hover { opacity:.6; box-shadow:0 2px 6px rgba(0,0,0,.2); }
  &.readonly { cursor:pointer; }
  &.constrained { animation: flash .3s ease-in-out 2; border:2px solid #f56c6c; }
  &.risk-warning { box-shadow: inset 3px 0 0 #e6a23c; }
  &.risk-delay { box-shadow: inset 3px 0 0 #f56c6c; }
  &.risk-behind { box-shadow: inset 3px 0 0 #e6a23c; outline:1px dashed #e6a23c; outline-offset:-1px; }
}
.gc-bar-actual {
  position:absolute; top:4px; height:16px; border-radius:3px; z-index:2; overflow:hidden;
  pointer-events:none;
}
.gc-bar-progress {
  position:absolute; left:0; top:0; height:100%; background:rgba(0,0,0,.25);
  pointer-events:none; border-radius:inherit;
}
.gc-resize-l, .gc-resize-r { width:6px; height:100%; position:absolute; top:0; cursor:ew-resize; z-index:3; }
.gc-resize-l { left:0; border-radius:4px 0 0 4px; }
.gc-resize-r { right:0; border-radius:0 4px 4px 0; }
@keyframes flash { 0%,100% { opacity:.35; } 50% { opacity:.15; border-color:#f56c6c; } }
</style>
