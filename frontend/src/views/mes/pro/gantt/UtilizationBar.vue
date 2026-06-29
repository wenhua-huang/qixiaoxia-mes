<template>
  <div class="util-root" v-if="bars.length">
    <div class="util-title">产能利用率</div>
    <div v-for="b in bars" :key="b.name" class="util-row">
      <span class="util-label">{{ b.name }}</span>
      <div class="util-track">
        <div class="util-fill" :class="b.level" :style="{width:b.pct+'%'}">
          <span v-if="b.pct > 20">{{ b.pct }}%</span>
        </div>
      </div>
      <span class="util-info">{{ b.taskMins }}分 / {{ b.capacity }}个/h</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { GanttTask } from '@/types/api/mes/pro/gantt'

const props = defineProps<{ tasks: GanttTask[] }>()

const bars = computed(() => {
  const map = new Map<string, { name: string; taskMins: number; capacity: number }>()
  for (const p of props.tasks) {
    for (const c of p.children || []) {
      const ws = (c as any).workstationId ? c.text?.split(' → ')[1] : null
      if (!ws || !c.duration) continue
      const key = ws + '|' + ((c as any).workstationId || '')
      if (!map.has(key)) {
        map.set(key, { name: ws, taskMins: 0, capacity: (c as any).capacity || 1 })
      }
      map.get(key)!.taskMins += c.duration
    }
  }
  const WORK_MINUTES = 480 // 8h = 480min per day
  return Array.from(map.values()).map(b => ({
    ...b,
    pct: Math.round(b.taskMins / WORK_MINUTES * 100),
    level: b.taskMins / WORK_MINUTES > 1 ? 'over' : b.taskMins / WORK_MINUTES > 0.8 ? 'warn' : 'ok'
  })).sort((a, b) => b.pct - a.pct)
})
</script>

<style scoped>
.util-root { margin:8px 0; padding:8px 12px; border:1px solid #e4e7ed; border-radius:4px; background:#fff; }
.util-title { font-size:12px; font-weight:600; margin-bottom:6px; }
.util-row { display:flex; align-items:center; gap:8px; margin:3px 0; }
.util-label { width:120px; font-size:11px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.util-track { flex:1; height:16px; background:#f5f7fa; border-radius:3px; overflow:hidden; }
.util-fill { height:100%; border-radius:3px; display:flex; align-items:center; justify-content:center; font-size:10px; color:#fff; transition:width .3s; min-width:2px; &.ok { background:#67c23a; } &.warn { background:#e6a23c; } &.over { background:#f56c6c; } }
.util-info { width:120px; font-size:10px; color:#909399; text-align:right; }
</style>
