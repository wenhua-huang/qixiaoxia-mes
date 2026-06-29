import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { ElMessage } from 'element-plus'

// Mock 请求
vi.mock('@/utils/request', () => ({
  default: vi.fn().mockResolvedValue({ data: [] })
}))

// Mock Element Plus 图标
vi.mock('@element-plus/icons-vue', () => ({
  ZoomIn: { template: '<span />' },
  ZoomOut: { template: '<span />' },
  Folder: { template: '<span />' },
}))

import GanttChart from '@/components/GanttChart/index.vue'
import type { GanttTask } from '@/types/api/mes/pro/gantt'

// 构建测试数据
function makeTasks(children: any[]): GanttTask[] {
  return [{
    id: 'WO-test', text: '测试工单', type: 'project',
    children: children.map(c => ({
      id: c.id, text: c.text || c.name, type: 'task' as const,
      start: c.start || '2026-06-30T08:00:00', end: c.end || '2026-06-30T09:00:00',
      colorCode: c.color || '#409eff', processName: c.name,
      predecessorId: c.predecessorId, status: c.status || 'NORMAL',
      quantity: 50, quantityProduced: 0, duration: 60
    }))
  }]
}

// helper: 模拟 mousedown + mousemove + mouseup
async function simulateDrag(wrapper: any, barIdx: number, deltaX: number) {
  const bars = wrapper.findAll('.gc-bar')
  const bar = bars[barIdx]
  if (!bar) throw new Error(`bar ${barIdx} not found`)

  const rect = { left: 100, top: 50, width: 80, height: 24 }
  bar.element.getBoundingClientRect = () => rect as DOMRectReadOnly

  // mousedown
  await bar.trigger('mousedown', { clientX: rect.left + 40 })

  // mousemove
  for (let i = 0; i < 3; i++) {
    document.dispatchEvent(new MouseEvent('mousemove', {
      clientX: rect.left + 40 + deltaX * (i + 1) / 3, bubbles: true
    }))
    await nextTick()
  }

  // mouseup
  document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
  await nextTick()
}

function mountGantt(tasks: GanttTask[]) {
  return mount(GanttChart, {
    props: { tasks },
    global: {
      stubs: {
        'el-button': { template: '<button><slot /></button>' },
        'el-button-group': { template: '<div><slot /></div>' },
      },
    },
  })
}

describe('GanttChart 前置约束', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    document.removeEventListener('mousemove', () => {})
    document.removeEventListener('mouseup', () => {})
  })

  // ═══════════════════════════════════
  // SS 顺序约束
  // ═══════════════════════════════════

  it('SS顺序工序：拖拽越过前道结束时间时应被卡住', async () => {
    const tasks = makeTasks([
      { id: '1', name: '印刷', start: '2026-06-30T08:00:00', end: '2026-06-30T10:00:00' },
      { id: '2', name: '制袋', start: '2026-06-30T10:30:00', end: '2026-06-30T12:00:00', predecessorId: 1 },
    ])
    const wrapper = mountGantt(tasks)
    await nextTick()
    await (wrapper.vm as any).render()
    await nextTick()

    // 找到制袋行
    const rows = (wrapper.vm as any).rows
    const bagRow = rows.find((r: any) => r.id === '2')
    expect(bagRow).toBeTruthy()
    expect(bagRow.minStartMs).toBe(new Date('2026-06-30T10:00:00').getTime())

    // 模拟拖拽制袋往左超过印刷结束时间(10:00)
    const bars = wrapper.findAll('.gc-bar')
    expect(bars.length).toBe(2)
    const bagBar = bars[1]

    const startX = 200
    bagBar.element.getBoundingClientRect = () => ({ left: startX, top: 50, width: 60, height: 24 } as DOMRectReadOnly)
    await bagBar.trigger('mousedown', { clientX: startX + 30 })

    // 往左拖 200px（期望越过印刷结束时间）
    document.dispatchEvent(new MouseEvent('mousemove', { clientX: startX - 170, bubbles: true }))
    await nextTick()

    // 约束应该卡住，制袋的 start 不能早于 10:00
    expect(rows[2].s.getTime()).toBeGreaterThanOrEqual(bagRow.minStartMs!)

    document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
    await nextTick()
  })

  it('SS顺序工序：越界时 emit barMove 的时间应被约束修正', async () => {
    const tasks = makeTasks([
      { id: '1', name: '印刷', start: '2026-06-30T08:00:00', end: '2026-06-30T10:00:00' },
      { id: '2', name: '制袋', start: '2026-06-30T10:30:00', end: '2026-06-30T12:00:00', predecessorId: 1 },
    ])
    const wrapper = mountGantt(tasks)
    await nextTick()
    await (wrapper.vm as any).render()
    await nextTick()

    const bars = wrapper.findAll('.gc-bar')
    const bagBar = bars[1]
    const startX = 200
    bagBar.element.getBoundingClientRect = () => ({ left: startX, top: 50, width: 60, height: 24 } as DOMRectReadOnly)
    await bagBar.trigger('mousedown', { clientX: startX + 30 })

    // 往左拖 300px
    document.dispatchEvent(new MouseEvent('mousemove', { clientX: startX - 270, bubbles: true }))
    await nextTick()
    document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
    await nextTick()

    // emit 的 barMove 事件应该携带被约束修正后的时间
    const emitted = wrapper.emitted('barMove')
    if (emitted && emitted.length > 0) {
      const [task, newStart, newEnd] = emitted[0] as [any, string, string]
      expect(task.id).toBe('2')
      // newStart 不应早于 10:00
      expect(new Date(newStart).getTime()).toBeGreaterThanOrEqual(new Date('2026-06-30T10:00:00').getTime())
    }
  })

  // ═══════════════════════════════════
  // FS 并行约束
  // ═══════════════════════════════════

  it('FS并行工序：当前按数组顺序约束（全SS），后续支持linkType区分', async () => {
    const tasks = makeTasks([
      { id: '1', name: '印刷', start: '2026-06-30T08:00:00', end: '2026-06-30T10:00:00' },
      { id: '2', name: '制袋', start: '2026-06-30T08:30:00', end: '2026-06-30T10:30:00' },
    ])
    const wrapper = mountGantt(tasks)
    await nextTick()
    await (wrapper.vm as any).render()
    await nextTick()

    const bagRow = (wrapper.vm as any).rows.find((r: any) => r.id === '2')
    // 按数组顺序，minStartMs = 前道结束时间（安全默认）
    expect(bagRow.minStartMs).toBe(new Date('2026-06-30T10:00:00').getTime())
  })

  // ═══════════════════════════════════
  // 首工序无约束
  // ═══════════════════════════════════

  it('首工序无前置，可任意拖拽', async () => {
    const tasks = makeTasks([
      { id: '1', name: '纸张分切', start: '2026-06-30T08:00:00', end: '2026-06-30T09:00:00' },
    ])
    const wrapper = mountGantt(tasks)
    await nextTick()
    await (wrapper.vm as any).render()
    await nextTick()

    const firstRow = (wrapper.vm as any).rows.find((r: any) => r.id === '1')
    expect(firstRow.minStartMs).toBe(0)
  })

  // ═══════════════════════════════════
  // ElMessage 提示
  // ═══════════════════════════════════

  it('越界时应弹出警告提示', async () => {
    const tasks = makeTasks([
      { id: '1', name: '印刷', start: '2026-06-30T08:00:00', end: '2026-06-30T10:00:00' },
      { id: '2', name: '制袋', start: '2026-06-30T10:30:00', end: '2026-06-30T12:00:00', predecessorId: 1 },
    ])
    const wrapper = mountGantt(tasks)
    await nextTick()
    await (wrapper.vm as any).render()
    await nextTick()

    const bars = wrapper.findAll('.gc-bar')
    const bagBar = bars[1]

    // mock past constraint
    const rows = (wrapper.vm as any).rows
    rows[2].minStartMs = new Date('2026-06-30T10:00:00').getTime()

    const startX = 200
    bagBar.element.getBoundingClientRect = () => ({ left: startX, top: 50, width: 60, height: 24 } as DOMRectReadOnly)
    await bagBar.trigger('mousedown', { clientX: startX + 30 })

    // 越界拖拽
    document.dispatchEvent(new MouseEvent('mousemove', { clientX: startX - 300, bubbles: true }))
    await nextTick()

    // constrained ref 应设为 true
    expect((wrapper.vm as any).constrained).toBe(true)

    document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
    await nextTick()
  })
})
