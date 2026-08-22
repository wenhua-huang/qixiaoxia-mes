import { ref, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { GanttRow } from './GanttBar.vue'
import type { GanttTask } from '@/types/api/mes/pro/gantt'

interface DragDeps {
  readonly: boolean
  colW: Ref<number>
  msPerUnit: Ref<number>
  rows: Ref<GanttRow[]>
  onSelect: (t: GanttTask) => void
  onBarMove: (t: GanttTask, newStart: string, newEnd: string) => void
}

/** 甘特图任务条拖拽/拉伸逻辑（move / resize-l / resize-r），含前置工序约束校验。 */
export function useGanttDrag(deps: DragDeps) {
  const { colW, msPerUnit, rows } = deps
  const constrained = ref(false)

  let dragRow: GanttRow | null = null
  let dragType: 'move' | 'resize-l' | 'resize-r' = 'move'
  let dragStartX = 0
  let dragOrigStart = 0
  let dragOrigEnd = 0
  let dragNewStart = 0
  let dragNewEnd = 0
  let dragMoved = false
  let constraintShown = false

  function onBarClick(_e: MouseEvent, row: GanttRow) {
    if (!dragMoved) deps.onSelect(row.raw)
  }

  function onBarDown(e: MouseEvent, row: GanttRow) {
    if (deps.readonly) return
    startDrag(e, row, 'move')
  }

  function onResizeDown(e: MouseEvent, row: GanttRow, side: 'left' | 'right') {
    if (deps.readonly) return
    startDrag(e, row, side === 'left' ? 'resize-l' : 'resize-r')
  }

  function startDrag(e: MouseEvent, row: GanttRow, type: 'move' | 'resize-l' | 'resize-r') {
    if (deps.readonly) return
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
      setTimeout(() => (constrained.value = false), 800)
      if (!constraintShown) {
        constraintShown = true
        setTimeout(() => (constraintShown = false), 2000)
        ElMessage({ message: '受前置工序约束，不能往前移动', type: 'warning', duration: 1500 })
      }
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
    deps.onBarMove(dragRow.raw, localStr(dragNewStart), localStr(dragNewEnd))
    dragRow = null
  }

  return { constrained, onBarClick, onBarDown, onResizeDown }
}

function localStr(ms: number): string {
  const d = new Date(ms)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
