import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// Mock API（不发起真实请求）
vi.mock('@/api/mes/md/workstation', () => ({
  listWorkstation: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
}))
vi.mock('@/api/mes/pro/process', () => ({
  listAllProcess: vi.fn().mockResolvedValue({ data: [
    { processId: 1, processCode: 'PRC-001', processName: '印刷', processType: 'PRINT' },
    { processId: 2, processCode: 'PRC-002', processName: '纸张分切', processType: 'SLITTING' },
    { processId: 3, processCode: 'PRC-003', processName: '制袋', processType: 'BAG_MAKE' },
    { processId: 4, processCode: 'PRC-004', processName: '检验', processType: 'INSPECT' },
  ] }),
}))
vi.mock('@/api/mes/md/workshop', () => ({
  listAllWorkshop: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/components/Pagination/index.vue', () => ({
  default: { template: '<div class="mock-pagination" />' },
}))

import WorkstationSelect from '@/components/workstationSelect/single.vue'

function mountWs(props?: { processId?: number }) {
  return mount(WorkstationSelect, {
    props: props || {},
    global: {
      stubs: {
        'el-dialog': { template: '<div class="mock-dialog"><slot /><slot name="footer" /></div>' },
        'el-table': { template: '<div class="mock-table"><slot /></div>' },
        'el-table-column': { template: '<div />' },
        'el-form': { template: '<div><slot /></div>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<input />' },
        'el-select': { template: '<select><slot /></select>' },
        'el-option': { template: '<option />' },
        'el-button': { template: '<button><slot /></button>' },
        'el-radio': { template: '<input type="radio" />' },
        'pagination': { template: '<div class="mock-pagination" />' },
      },
    },
  })
}

describe('工作站选择器 — 工序筛选逻辑', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ═══════════════════════════════════
  // queryParams 状态管理
  // ═══════════════════════════════════

  it('open() 应重置搜索条件并设置 processId', async () => {
    const wrapper = mountWs()
    await nextTick()
    const vm = wrapper.vm as any

    // 模拟脏数据
    vm.queryParams.workstationCode = 'ABC'
    vm.queryParams.workstationName = '测试'
    vm.queryParams.pageNum = 3

    vm.open(2)
    await nextTick()

    expect(vm.queryParams.workstationCode).toBeUndefined()
    expect(vm.queryParams.workstationName).toBeUndefined()
    expect(vm.queryParams.pageNum).toBe(1)
    expect(vm.queryParams.pageSize).toBe(10)
    expect(vm.queryParams.processId).toBe(2)
  })

  it('open() 不传参时从 prop 读取 processId', async () => {
    const wrapper = mountWs({ processId: 1 })
    await nextTick()
    const vm = wrapper.vm as any

    vm.open()
    await nextTick()

    expect(vm.queryParams.processId).toBe(1)
  })

  it('open() 显式传参优先于 prop', async () => {
    const wrapper = mountWs({ processId: 1 })
    await nextTick()
    const vm = wrapper.vm as any

    vm.open(2)
    await nextTick()

    expect(vm.queryParams.processId).toBe(2)
  })

  it('open() 传 null/undefined 时回退到 prop', async () => {
    const wrapper = mountWs({ processId: 1 })
    await nextTick()
    const vm = wrapper.vm as any

    vm.open(null)
    await nextTick()
    expect(vm.queryParams.processId).toBe(1)

    vm.open(undefined)
    await nextTick()
    expect(vm.queryParams.processId).toBe(1)
  })

  it('open() 传了不在下拉选项中的 processId 时不设筛选', async () => {
    const wrapper = mountWs({ processId: 3 })
    await nextTick()
    const vm = wrapper.vm as any

    // 999 不在 mock 的 processOptions 中 → 不筛选
    vm.open(999)
    await nextTick()

    expect(vm.queryParams.processId).toBeUndefined()
  })

  // ═══════════════════════════════════
  // resetQuery
  // ═══════════════════════════════════

  it('resetQuery 应保留 prop processId，清除用户输入的搜索条件', async () => {
    const wrapper = mountWs({ processId: 2 })
    await nextTick()
    const vm = wrapper.vm as any

    vm.queryParams.workstationCode = 'USER_INPUT'
    vm.queryParams.workstationName = '用户搜索'
    vm.queryParams.pageNum = 5
    vm.queryParams.processId = 1 // 用户改过

    vm.resetQuery()
    await nextTick()

    expect(vm.queryParams.workstationCode).toBeUndefined()
    expect(vm.queryParams.workstationName).toBeUndefined()
    expect(vm.queryParams.pageNum).toBe(1)
    expect(vm.queryParams.processId).toBe(2) // 回到 prop
  })
})
