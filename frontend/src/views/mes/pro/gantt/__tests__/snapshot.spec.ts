import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'

// ── Mock 外部依赖 ──
vi.mock('@/utils/request', () => ({
  default: vi.fn().mockResolvedValue({ data: {} })
}))

vi.mock('@/utils/auth', () => ({ getToken: () => 'mock-token' }))

vi.mock('@element-plus/icons-vue', () => ({
  Plus: { template: '<span />' }
}))

import SnapShotPanel from '@/views/mes/pro/gantt/SnapShotPanel.vue'
import request from '@/utils/request'

/** 创建快照的 POST 请求匹配器（区分于 load() 的 GET /snapshot/list） */
const POST_SNAPSHOT = expect.objectContaining({ url: '/mes/pro/gantt/snapshot', method: 'post' })

function mountPanel(props: any = {}) {
  return mount(SnapShotPanel, {
    props: { workorderId: 1, tasks: [], ...props },
    global: {
      stubs: {
        'el-card': { template: '<div class="el-card"><slot /></div>' },
        'el-button': { template: '<button class="mock-btn" @click="$emit(\'click\')"><slot /></button>' },
        'el-table': { template: '<table><slot /></table>' },
        'el-table-column': { template: '<td />' },
        'el-tag': { template: '<span><slot /></span>' }
      }
    }
  })
}

describe('排产快照 - 保存前工作站校验', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 恢复 confirm 默认行为（onMounted 的 load 不触发 confirm，保持干净）
    vi.spyOn(ElMessageBox, 'confirm').mockRestore()
  })

  it('所有任务都已选工作站时直接保存（不弹确认）', async () => {
    const confirmSpy = vi.spyOn(ElMessageBox, 'confirm')
    const wrapper = mountPanel({
      workorderId: 1,
      tasks: [{
        id: 'WO-1', children: [
          { id: '1', workstationId: 10, processName: '印刷' },
          { id: '2', workstationId: 20, processName: '分切' }
        ]
      }]
    })

    await wrapper.find('.mock-btn').trigger('click')
    await flushPromises()

    expect(confirmSpy).not.toHaveBeenCalled()
    expect(request).toHaveBeenCalledWith(POST_SNAPSHOT)
  })

  it('存在未选工作站的任务时弹出确认并显示数量', async () => {
    const confirmSpy = vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as any)
    const wrapper = mountPanel({
      workorderId: 1,
      tasks: [{
        id: 'WO-1', children: [
          { id: '1', workstationId: 10, processName: '印刷' },
          { id: '2', workstationId: null, processName: '分切' },  // 未选
          { id: '3', workstationId: 0, processName: '包装' }       // 自动分配占位，视为未选
        ]
      }]
    })

    await wrapper.find('.mock-btn').trigger('click')
    await flushPromises()

    expect(confirmSpy).toHaveBeenCalledWith(
      expect.stringContaining('2 个任务未选择工作站'),
      '提示',
      expect.objectContaining({ type: 'warning' })
    )
    // 用户确认后继续保存
    expect(request).toHaveBeenCalledWith(POST_SNAPSHOT)
  })

  it('用户取消确认时不保存快照', async () => {
    vi.spyOn(ElMessageBox, 'confirm').mockRejectedValue('cancel' as any)
    const wrapper = mountPanel({
      workorderId: 1,
      tasks: [{ id: 'WO-1', children: [{ id: '1', workstationId: null }] }]
    })

    await wrapper.find('.mock-btn').trigger('click')
    await flushPromises()

    expect(request).not.toHaveBeenCalledWith(POST_SNAPSHOT)
  })

  it('未选择工单时警告且不保存', async () => {
    const warnSpy = vi.spyOn(ElMessage, 'warning')
    const wrapper = mountPanel({ workorderId: null, tasks: [] })

    await wrapper.find('.mock-btn').trigger('click')
    await flushPromises()

    expect(warnSpy).toHaveBeenCalledWith('请先选择工单再保存快照')
    expect(request).not.toHaveBeenCalledWith(POST_SNAPSHOT)
  })
})
