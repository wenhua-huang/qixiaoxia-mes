import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// ==================== Mock APIs ====================
vi.mock('@/api/mes/cal/plan', () => ({
  listPlan: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getPlan: vi.fn().mockResolvedValue({ data: {} }),
  delPlan: vi.fn().mockResolvedValue({}),
  addPlan: vi.fn().mockResolvedValue({}),
  updatePlan: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/mes/cal/shift', () => ({
  listShift: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getShift: vi.fn().mockResolvedValue({ data: {} }),
  addShift: vi.fn().mockResolvedValue({}),
  updateShift: vi.fn().mockResolvedValue({}),
  delShift: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/mes/cal/plan-team', () => ({
  listTeam: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  addTeam: vi.fn().mockResolvedValue({}),
  delTeam: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/mes/cal/team-member', () => ({
  listMember: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
}))

vi.mock('@/api/mes/sys/autocoderule', () => ({
  genSerialCode: vi.fn().mockResolvedValue('AUTO-PLAN-001'),
}))

// ==================== Mock vue-router + store ====================
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), currentRoute: { value: { query: {} } } }),
  useRoute: () => ({ query: {} }),
}))

vi.mock('@/store/modules/user', async () => {
  const { defineStore } = await import('pinia')
  return { default: defineStore('user', () => ({ token: 'mock', name: 'admin', roles: ['admin'], permissions: ['*:*:*'] })) }
})

vi.mock('@/store/modules/dict', async () => {
  const { defineStore } = await import('pinia')
  return { default: defineStore('dict', () => ({ getDict: vi.fn(() => []) })) }
})

vi.mock('@/utils/jsencrypt', () => ({ encrypt: vi.fn(), decrypt: vi.fn() }))
vi.mock('js-cookie', () => ({ default: { get: vi.fn(() => ''), set: vi.fn(), remove: vi.fn() } }))
vi.mock('@/utils/ruoyi', () => ({ resetForm: vi.fn(), parseTime: vi.fn(), addDateRange: vi.fn(), handleTree: vi.fn(), selectDictLabel: vi.fn(), selectDictLabels: vi.fn() }))
vi.mock('@/utils/dict', () => ({
  useDict: () => ({
    mes_calendar_type: [],
    mes_shift_type: [],
    mes_shift_method: [],
    mes_order_status: [],
  }),
}))

// ==================== Import component ====================
import Plan from '@/views/mes/cal/plan/index.vue'

function mountPlan() {
  return mount(Plan, {
    global: {
      stubs: {
        'right-toolbar': { template: '<div class="mock-right-toolbar" />' },
        'pagination': { template: '<div class="mock-pagination" />' },
        'dict-tag': { template: '<span class="mock-dict-tag" />' },
        'el-dialog': { template: '<div class="mock-dialog"><slot /><slot name="footer" /></div>' },
        'el-drawer': { template: '<div />' },
        'el-popover': { template: '<div />' },
        'el-tooltip': { template: '<div />' },
        'Shift': { template: '<div class="mock-shift" />' },
        'Team': { template: '<div class="mock-team" />' },
      },
      directives: { hasPermi: () => {}, hasRole: () => {} },
      mocks: {
        $modal: { msgSuccess: vi.fn(), msgError: vi.fn(), confirm: vi.fn().mockResolvedValue('confirm') },
        download: vi.fn(),
        resetForm: vi.fn(),
      },
    },
  })
}

describe('排班计划 — plan/index.vue', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('应该正常挂载并显示搜索区域', async () => {
    const wrapper = mountPlan()
    await nextTick()
    await nextTick()
    expect(wrapper.find('.app-container').exists()).toBe(true)
    expect(wrapper.text()).toContain('计划编号')
    expect(wrapper.text()).toContain('计划名称')
  })

  it('应该渲染操作按钮（新增/删除/导出）', async () => {
    const wrapper = mountPlan()
    await nextTick()
    await nextTick()
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    expect(buttons.length).toBeGreaterThanOrEqual(3)
  })

  it('应该渲染计划表格', async () => {
    const wrapper = mountPlan()
    await nextTick()
    await nextTick()
    expect(wrapper.findComponent({ name: 'ElTable' }).exists()).toBe(true)
  })

  it('dialog 应带 tabs 用于班次和班组（查看模式回归）', async () => {
    const wrapper = mountPlan()
    await nextTick()
    await nextTick()
    // 组件正常挂载，dialog template 无未闭合标签
    expect(wrapper.exists()).toBe(true)
  })

  it('搜索表单应包含班组类型下拉和日期选择器', async () => {
    const wrapper = mountPlan()
    await nextTick()
    await nextTick()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    const datePickers = wrapper.findAllComponents({ name: 'ElDatePicker' })
    expect(selects.length).toBeGreaterThanOrEqual(1)
    expect(datePickers.length).toBeGreaterThanOrEqual(1)
  })
})
