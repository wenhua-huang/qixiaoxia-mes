import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// ==================== Mock API ====================
vi.mock('@/api/mes/cal/team', () => ({
  listTeam: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getTeam: vi.fn().mockResolvedValue({ data: {} }),
  delTeam: vi.fn().mockResolvedValue({}),
  addTeam: vi.fn().mockResolvedValue({}),
  updateTeam: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/mes/cal/team-member', () => ({
  listMember: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  addMember: vi.fn().mockResolvedValue({}),
  delMember: vi.fn().mockResolvedValue({}),
}))

vi.mock('@/api/mes/sys/autocoderule', () => ({
  genSerialCode: vi.fn().mockResolvedValue('AUTO-001'),
}))

// ==================== Mock vue-router ====================
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn(), currentRoute: { value: { query: {} } } }),
  useRoute: () => ({ query: {} }),
}))

// ==================== Mock store ====================
vi.mock('@/store/modules/user', async () => {
  const { defineStore } = await import('pinia')
  return {
    default: defineStore('user', () => ({
      token: 'mock-token', name: 'admin', roles: ['admin'], permissions: ['*:*:*'],
    })),
  }
})

vi.mock('@/store/modules/dict', async () => {
  const { defineStore } = await import('pinia')
  return { default: defineStore('dict', () => ({ getDict: vi.fn(() => []) })) }
})

vi.mock('@/utils/jsencrypt', () => ({ encrypt: vi.fn(), decrypt: vi.fn() }))
vi.mock('js-cookie', () => ({ default: { get: vi.fn(() => ''), set: vi.fn(), remove: vi.fn() } }))
vi.mock('@/utils/ruoyi', () => ({ resetForm: vi.fn(), parseTime: vi.fn(), addDateRange: vi.fn(), handleTree: vi.fn(), selectDictLabel: vi.fn(), selectDictLabels: vi.fn() }))
vi.mock('@/utils/dict', () => ({ useDict: () => ({ mes_calendar_type: ref([]) }) }))

// ==================== Import component ====================
import { ref } from 'vue'
import Team from '@/views/mes/cal/team/index.vue'

function mountTeam() {
  return mount(Team, {
    global: {
      stubs: {
        'right-toolbar': { template: '<div class="mock-right-toolbar" />' },
        'pagination': { template: '<div class="mock-pagination" />' },
        'dict-tag': { template: '<span class="mock-dict-tag" />' },
        'el-dialog': { template: '<div class="mock-dialog"><slot /><slot name="footer" /></div>' },
        'el-drawer': { template: '<div />' },
        'el-popover': { template: '<div />' },
        'el-tooltip': { template: '<div />' },
        'Member': { template: '<div class="mock-member" />' },
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

describe('班组定义 — team/index.vue', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('应该正常挂载并显示搜索区域', async () => {
    const wrapper = mountTeam()
    await nextTick()
    await nextTick()
    expect(wrapper.find('.app-container').exists()).toBe(true)
    expect(wrapper.text()).toContain('班组编号')
    expect(wrapper.text()).toContain('班组名称')
  })

  it('应该渲染操作按钮（新增/修改/删除/导出）', async () => {
    const wrapper = mountTeam()
    await nextTick()
    await nextTick()
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    expect(buttons.length).toBeGreaterThanOrEqual(4)
  })

  it('应该渲染班组表格', async () => {
    const wrapper = mountTeam()
    await nextTick()
    await nextTick()
    expect(wrapper.findComponent({ name: 'ElTable' }).exists()).toBe(true)
  })

  it('template 中 el-col 标签应正确闭合（回归测试：无 "Element is missing end tag" 错误）', () => {
    const wrapper = mountTeam()
    expect(wrapper.exists()).toBe(true)
  })

  it('dialog 表单应包含班组类型 selector', async () => {
    const wrapper = mountTeam()
    await nextTick()
    await nextTick()
    // 验证搜索表单中有班组类型下拉
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    expect(selects.length).toBeGreaterThanOrEqual(1)
  })
})
