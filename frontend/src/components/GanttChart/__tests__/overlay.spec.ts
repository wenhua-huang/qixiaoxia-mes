import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// Mock 请求（与 constraint.spec.ts 一致）
vi.mock('@/utils/request', () => ({
  default: vi.fn().mockResolvedValue({ data: [] })
}))

vi.mock('@element-plus/icons-vue', () => ({
  ZoomIn: { template: '<span />' },
  ZoomOut: { template: '<span />' },
  Folder: { template: '<span />' },
}))

import GanttChart from '@/components/GanttChart/index.vue'
import type { GanttTask } from '@/types/api/mes/pro/gantt'

function makeTask(child: Partial<GanttTask>): GanttTask[] {
  return [{
    id: 'WO-test', text: '测试工单', type: 'project',
    children: [{
      id: '1', text: '印刷', type: 'task',
      start: '2026-06-30T08:00:00', end: '2026-06-30T12:00:00',
      colorCode: '#409eff', processName: '印刷', status: 'NORMAL',
      quantity: 50, quantityProduced: 30, duration: 240,
      ...child
    } as GanttTask]
  }]
}

function mountGantt(tasks: GanttTask[], readonly = false) {
  return mount(GanttChart, {
    props: { tasks, readonly },
    global: {
      stubs: {
        'el-button': { template: '<button><slot /></button>' },
        'el-button-group': { template: '<div><slot /></div>' },
      },
    },
  })
}

async function renderChart(wrapper: any) {
  await nextTick()
  await wrapper.vm.render()
  await nextTick()
}

describe('GanttChart 实际进度叠加层', () => {
  beforeEach(() => { vi.clearAllMocks() })
  afterEach(() => {
    document.removeEventListener('mousemove', () => {})
    document.removeEventListener('mouseup', () => {})
  })

  it('有 actualStartTime/actualEndTime 时渲染 .gc-bar-actual，且 left 对应 posX(actualStart)', async () => {
    // 用空格分隔日期，验证 parseDate 的 replace 逻辑
    const tasks = makeTask({
      actualStartTime: '2026-06-30 09:00:00',
      actualEndTime: '2026-06-30 11:00:00',
      progressPercent: 50
    })
    const wrapper = mountGantt(tasks)
    await renderChart(wrapper)

    const actual = wrapper.find('.gc-bar-actual')
    expect(actual.exists()).toBe(true)

    const row = (wrapper.vm as any).rows.find((r: any) => r.id === '1')
    const posX = (wrapper.vm as any).posX
    const expectedLeft = posX(row.aS)
    expect(actual.element.getAttribute('style')).toContain(`left: ${expectedLeft}px`)
    expect(row.aS instanceof Date && !isNaN(row.aS.getTime())).toBe(true)
  })

  it('无 actual 字段时不渲染 .gc-bar-actual，进度填充落在计划条内', async () => {
    const wrapper = mountGantt(makeTask({ progressPercent: 30 }))
    await renderChart(wrapper)
    expect(wrapper.find('.gc-bar-actual').exists()).toBe(false)
    const planBar = wrapper.find('.gc-bar')
    expect(planBar.exists()).toBe(true)
    const progress = planBar.find('.gc-bar-progress')
    expect(progress.exists()).toBe(true)
    expect(progress.element.getAttribute('style')).toContain('width: 30%')
  })

  it('actual 时间非法(Invalid Date)时不渲染 .gc-bar-actual，避免 NaNpx', async () => {
    const wrapper = mountGantt(makeTask({
      actualStartTime: 'not-a-date',
      actualEndTime: 'also-bad'
    }))
    await renderChart(wrapper)
    expect(wrapper.find('.gc-bar-actual').exists()).toBe(false)
  })

  it('progressPercent 60 时 .gc-bar-progress 宽度含 60%', async () => {
    const wrapper = mountGantt(makeTask({
      actualStartTime: '2026-06-30T09:00:00',
      actualEndTime: '2026-06-30T11:00:00',
      progressPercent: 60
    }))
    await renderChart(wrapper)
    const progress = wrapper.find('.gc-bar-progress')
    expect(progress.exists()).toBe(true)
    expect(progress.element.getAttribute('style')).toContain('width: 60%')
  })

  it('progressPercent 越界时被夹到 0-100', async () => {
    const wrapper = mountGantt(makeTask({ progressPercent: 150 }))
    await renderChart(wrapper)
    const row = (wrapper.vm as any).rows.find((r: any) => r.id === '1')
    expect(row.progress).toBe(100)
  })

  it("delayLevel='DELAY' 时根条含 risk-delay class", async () => {
    const wrapper = mountGantt(makeTask({ delayLevel: 'DELAY' }))
    await renderChart(wrapper)
    expect(wrapper.find('.gc-bar').classes()).toContain('risk-delay')
  })

  it("delayLevel='FINISHED_DELAY' 也使用 risk-delay", async () => {
    const wrapper = mountGantt(makeTask({ delayLevel: 'FINISHED_DELAY' }))
    await renderChart(wrapper)
    expect(wrapper.find('.gc-bar').classes()).toContain('risk-delay')
  })

  it("delayLevel='WARNING' 时含 risk-warning", async () => {
    const wrapper = mountGantt(makeTask({ delayLevel: 'WARNING' }))
    await renderChart(wrapper)
    expect(wrapper.find('.gc-bar').classes()).toContain('risk-warning')
  })

  it("delayLevel='BEHIND' 时含 risk-behind", async () => {
    const wrapper = mountGantt(makeTask({ delayLevel: 'BEHIND' }))
    await renderChart(wrapper)
    expect(wrapper.find('.gc-bar').classes()).toContain('risk-behind')
  })

  it("delayLevel='NORMAL' 时无 risk-* class", async () => {
    const wrapper = mountGantt(makeTask({ delayLevel: 'NORMAL' }))
    await renderChart(wrapper)
    const classes = wrapper.find('.gc-bar').classes()
    expect(classes.some((c: string) => c.startsWith('risk-'))).toBe(false)
  })

  it('默认(非readonly) delayLevel 缺省为 NORMAL，无 risk class', async () => {
    const wrapper = mountGantt(makeTask({}))
    await renderChart(wrapper)
    const row = (wrapper.vm as any).rows.find((r: any) => r.id === '1')
    expect(row.delayLevel).toBe('NORMAL')
  })
})

describe('GanttChart readonly', () => {
  beforeEach(() => { vi.clearAllMocks() })
  afterEach(() => {
    document.removeEventListener('mousemove', () => {})
    document.removeEventListener('mouseup', () => {})
  })

  it('readonly=true 时不渲染 resize 手柄', async () => {
    const wrapper = mountGantt(makeTask({}), true)
    await nextTick()
    await wrapper.vm.render()
    await nextTick()
    expect(wrapper.find('.gc-resize-l').exists()).toBe(false)
    expect(wrapper.find('.gc-resize-r').exists()).toBe(false)
  })

  it('readonly=false(默认) 时仍渲染 resize 手柄', async () => {
    const wrapper = mountGantt(makeTask({}))
    await nextTick()
    await wrapper.vm.render()
    await nextTick()
    expect(wrapper.find('.gc-resize-l').exists()).toBe(true)
    expect(wrapper.find('.gc-resize-r').exists()).toBe(true)
  })

  it('readonly=true 时 mousedown 不触发拖拽（无 barMove）', async () => {
    const wrapper = mountGantt(makeTask({}), true)
    await nextTick()
    await wrapper.vm.render()
    await nextTick()

    const bar = wrapper.find('.gc-bar')
    const rect = { left: 100, top: 50, width: 80, height: 24 }
    bar.element.getBoundingClientRect = () => rect as DOMRectReadOnly

    await bar.trigger('mousedown', { clientX: 140 })
    document.dispatchEvent(new MouseEvent('mousemove', { clientX: 300, bubbles: true }))
    await nextTick()
    document.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }))
    await nextTick()

    expect(wrapper.emitted('barMove')).toBeFalsy()
  })

  it('readonly=true 时点击(mouseup)仍 emit select', async () => {
    const wrapper = mountGantt(makeTask({}), true)
    await nextTick()
    await wrapper.vm.render()
    await nextTick()

    await wrapper.find('.gc-bar').trigger('mouseup')
    await nextTick()

    const selected = wrapper.emitted('select')
    expect(selected).toBeTruthy()
    expect((selected![0] as any[])[0].id).toBe('1')
  })

  it('readonly 计划条 cursor 为 pointer（含 readonly class）', async () => {
    const wrapper = mountGantt(makeTask({}), true)
    await nextTick()
    await wrapper.vm.render()
    await nextTick()
    expect(wrapper.find('.gc-bar').classes()).toContain('readonly')
  })
})
