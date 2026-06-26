import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// ═══════════ Mock APIs ═══════════
const { mockListWorkshop } = vi.hoisted(() => ({
  mockListWorkshop: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
}))

vi.mock('@/api/mes/md/workshop', () => ({
  listWorkshop: mockListWorkshop,
  getWorkshop: vi.fn().mockResolvedValue({ data: {} }),
  delWorkshop: vi.fn().mockResolvedValue({}),
  addWorkshop: vi.fn().mockResolvedValue({ code: 200 }),
  updateWorkshop: vi.fn().mockResolvedValue({ code: 200 }),
}))

vi.mock('@/api/mes/sys/autocoderule', () => ({
  genSerialCode: vi.fn().mockResolvedValue({ data: 'WS-AUTO-001' }),
}))

vi.mock('@/utils/dict', () => ({
  useDict: () => ({ sys_yes_no: [] }),
}))

vi.mock('@/utils/ruoyi', () => ({
  resetForm: vi.fn(),
}))

// ═══════════ Import component ═══════════
import Workshop from '@/views/mes/md/workshop/index.vue'

describe('车间管理 — 无删除按钮', () => {
  it('工具栏不含删除按钮', async () => {
    const wrapper = mount(Workshop, {
      global: {
        stubs: {
          'right-toolbar': { template: '<div class="mock-right-toolbar" />' },
          'pagination': { template: '<div class="mock-pagination" />' },
          'dict-tag': { template: '<span class="mock-dict-tag" />' },
          'el-dialog': { template: '<div class="mock-dialog"><slot /><slot name="footer" /></div>' },
          'el-row': { template: '<div class="mock-row"><slot /></div>' },
          'el-col': { template: '<div class="mock-col"><slot /></div>' },
          'el-table': { template: '<div class="mock-table"><slot /></div>' },
          'el-table-column': { template: '<div class="mock-table-col" />' },
          'el-form': { template: '<div class="mock-form"><slot /></div>' },
          'el-form-item': { template: '<div class="mock-form-item"><slot /></div>' },
          'el-input': { template: '<input class="mock-input" />' },
          'el-button': { template: '<button class="mock-btn"><slot /></button>' },
          'el-switch': { template: '<div class="mock-switch" />' },
          'el-radio-group': { template: '<div class="mock-radio-group" />' },
          'el-radio': { template: '<div class="mock-radio" />' },
          'el-select': { template: '<div class="mock-select" />' },
          'el-option': { template: '<div class="mock-option" />' },
          'el-tooltip': { template: '<div class="mock-tooltip" />' },
          'el-link': { template: '<a class="mock-link"><slot /></a>' },
        },
        directives: { hasPermi: () => {} },
        mocks: {
          $modal: { msgSuccess: vi.fn(), msgError: vi.fn(), confirm: vi.fn().mockResolvedValue('confirm') },
          resetForm: vi.fn(),
        },
      },
    })
    await nextTick()

    // HTML 注释提示存在，但页面不含可交互的删除按钮
    const html = wrapper.html()
    // 注释标记存在（说明删除按钮已被替换）
    expect(html).toContain('启停用')
    // 按钮文本不直接含"删除"
    const btns = wrapper.findAll('.mock-btn')
    const btnTexts = btns.map(b => b.text()).join('|')
    expect(btnTexts).not.toContain('删除')
  })

  it('注释提示存在 — 用启停用替代物理删除', async () => {
    const wrapper = mount(Workshop, {
      global: {
        stubs: {
          'right-toolbar': { template: '<div />' },
          'pagination': { template: '<div />' },
          'dict-tag': { template: '<span />' },
          'el-dialog': { template: '<div />' },
          'el-row': { template: '<div><slot /></div>' },
          'el-col': { template: '<div><slot /></div>' },
          'el-table': { template: '<div />' },
          'el-table-column': { template: '<div />' },
          'el-form': { template: '<div><slot /></div>' },
          'el-form-item': { template: '<div><slot /></div>' },
          'el-input': { template: '<input />' },
          'el-button': { template: '<button><slot /></button>' },
          'el-switch': { template: '<div />' },
          'el-radio-group': { template: '<div />' },
          'el-radio': { template: '<div />' },
          'el-select': { template: '<div />' },
          'el-option': { template: '<div />' },
          'el-tooltip': { template: '<div />' },
          'el-link': { template: '<a><slot /></a>' },
        },
        directives: { hasPermi: () => {} },
        mocks: {
          $modal: { msgSuccess: vi.fn(), msgError: vi.fn(), confirm: vi.fn() },
          resetForm: vi.fn(),
        },
      },
    })
    await nextTick()

    // 注释内容应存在
    const html = wrapper.html()
    expect(html).toContain('启停用')
  })
})
