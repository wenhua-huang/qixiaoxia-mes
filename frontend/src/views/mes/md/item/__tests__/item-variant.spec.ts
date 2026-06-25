import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// ═══════════════════════════════════════
// Hoisted mock functions
// ═══════════════════════════════════════
const { mockListItem, mockGetItem } = vi.hoisted(() => ({
  mockListItem: vi.fn(),
  mockGetItem: vi.fn(),
}))

// ═══════════════════════════════════════
// Mock API modules
// ═══════════════════════════════════════
vi.mock('@/api/mes/md/item', () => ({
  listItem: mockListItem,
  getItem: mockGetItem,
  addItem: vi.fn().mockResolvedValue({ code: 200 }),
  updateItem: vi.fn().mockResolvedValue({ code: 200 }),
  delItem: vi.fn().mockResolvedValue({}),
}))
vi.mock('@/api/mes/md/itemtype', () => ({
  treeselect: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/md/unitmeasure', () => ({
  listAllUnitmeasure: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/sys/autocoderule', () => ({
  genSerialCode: vi.fn().mockResolvedValue({ data: 'ITEM-AUTO-001' }),
}))
vi.mock('@/api/mes/md/itembom', () => ({
  listBomByItemId: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/md/itembatchconfig', () => ({
  getBatchConfigByItemId: vi.fn().mockResolvedValue({ data: null }),
  saveBatchConfig: vi.fn().mockResolvedValue({}),
}))

// ═══════════════════════════════════════
// Mock vue-router + stores
// ═══════════════════════════════════════
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
vi.mock('@/utils/dict', () => ({ useDict: () => ({ sys_yes_no: [] }) }))

// ═══════════════════════════════════════
// Import component
// ═══════════════════════════════════════
import Item from '@/views/mes/md/item/index.vue'

function mountItem() {
  return mount(Item, {
    global: {
      stubs: {
        'right-toolbar': { template: '<div class="mock-right-toolbar" />' },
        'pagination': { template: '<div class="mock-pagination" />' },
        'dict-tag': { template: '<span class="mock-dict-tag" />' },
        'el-dialog': { template: '<div class="mock-dialog"><slot /><slot name="footer" /></div>' },
        'el-drawer': { template: '<div />' },
        'el-popover': { template: '<div />' },
        'el-tooltip': { template: '<div />' },
        'el-tree': { template: '<div class="mock-tree" />' },
        'el-tree-select': { template: '<select class="mock-tree-select" />' },
        'el-tabs': { template: '<div class="mock-tabs"><slot /></div>' },
        'el-tab-pane': { template: '<div class="mock-tab-pane"><slot /></div>' },
        'el-input-number': { template: '<input type="number" class="mock-input-number" />' },
        'el-link': { template: '<a class="mock-link"><slot /></a>' },
        'el-row': { template: '<div class="mock-row"><slot /></div>' },
        'el-col': { template: '<div class="mock-col"><slot /></div>' },
        'el-tag': { template: '<span class="mock-tag"><slot /></span>' },
        'el-switch': { template: '<div class="mock-switch" />' },
        'el-select': { template: '<div class="mock-select"><slot /></div>' },
        'el-option': { template: '<div class="mock-option" />' },
        'el-radio-group': { template: '<div class="mock-radio-group" />' },
        'el-radio': { template: '<div class="mock-radio" />' },
        'el-date-picker': { template: '<input class="mock-date-picker" />' },
        'BatchConfig': { template: '<div class="mock-batch-config" />' },
        'ItemBom': { template: '<div class="mock-item-bom" />' },
      },
      directives: { hasPermi: () => {}, hasRole: () => {} },
      mocks: {
        $modal: { msgSuccess: vi.fn(), msgError: vi.fn(), msgWarning: vi.fn(), confirm: vi.fn().mockResolvedValue('confirm') },
        download: vi.fn(), resetForm: vi.fn(),
      },
    },
  })
}

describe('物料管理 — 变体功能', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ═══════════════════════════════════════
  // 1. 变体物料显示 [变体] 标签
  // ═══════════════════════════════════════
  it('parentId>0 → 物料编码旁显示 [变体] 标签', async () => {
    mockListItem.mockResolvedValue({
      rows: [
        { itemId: 301, itemCode: 'ZD-01-V1', itemName: '奔趣-彩印', parentId: 201, itemTypeName: '纸袋成品', unitName: '个', enableFlag: '1' },
      ],
      total: 1,
    })
    mockGetItem.mockResolvedValue({ data: {} })

    const wrapper = mountItem()
    await nextTick(); await nextTick()

    // 变体标签应渲染
    const tags = wrapper.findAll('.variant-tag')
    expect(tags.length).toBeGreaterThan(0)
  })

  // ═══════════════════════════════════════
  // 2. SPU 不显示变体标签
  // ═══════════════════════════════════════
  it('parentId=0 → 不显示变体标签', async () => {
    mockListItem.mockResolvedValue({
      rows: [
        { itemId: 201, itemCode: 'ZD-01', itemName: '奔趣纸袋', parentId: 0, itemTypeName: '纸袋成品', unitName: '个', enableFlag: '1' },
      ],
      total: 1,
    })

    const wrapper = mountItem()
    await nextTick(); await nextTick()

    const tags = wrapper.findAll('.variant-tag')
    expect(tags.length).toBe(0)
  })

  // ═══════════════════════════════════════
  // 3. SPU/变体筛选 — 选"变体"
  // ═══════════════════════════════════════
  it('筛选选"变体" → queryParams.parentIdFilter 为 variant', async () => {
    mockListItem.mockResolvedValue({ rows: [], total: 0 })

    const wrapper = mountItem()
    await nextTick(); await nextTick()

    const vm = wrapper.vm as any
    // 模拟选择"变体"
    vm.queryParams.parentIdFilter = 'variant'
    vm.handleQuery()
    await nextTick()

    // 验证 listItem 被调用时携带 parentIdFilter
    expect(mockListItem).toHaveBeenCalled()
    const callArg = mockListItem.mock.calls[mockListItem.mock.calls.length - 1][0]
    expect(callArg.parentIdFilter).toBe('variant')
  })

  // ═══════════════════════════════════════
  // 4. SPU/变体筛选 — 选"SPU"
  // ═══════════════════════════════════════
  it('筛选选"SPU" → queryParams.parentIdFilter 为 spu', async () => {
    mockListItem.mockResolvedValue({ rows: [], total: 0 })

    const wrapper = mountItem()
    await nextTick(); await nextTick()

    const vm = wrapper.vm as any
    vm.queryParams.parentIdFilter = 'spu'
    vm.handleQuery()
    await nextTick()

    const callArg = mockListItem.mock.calls[mockListItem.mock.calls.length - 1][0]
    expect(callArg.parentIdFilter).toBe('spu')
  })

  // ═══════════════════════════════════════
  // 5. parentItemDisplay 计算属性 — 变体
  // ═══════════════════════════════════════
  it('parentItemDisplay: parentId>0 → 显示父产品编码和名称', async () => {
    mockListItem.mockResolvedValue({ rows: [], total: 0 })

    const wrapper = mountItem()
    await nextTick(); await nextTick()

    const vm = wrapper.vm as any
    // 直接设置 form 数据（绕过 handleUpdate 中的 getCurrentInstance 限制）
    vm.form.parentId = 201
    vm.form.parentItemCode = 'ZD-01'
    vm.form.parentItemName = '奔趣纸袋'
    await nextTick()

    expect(vm.parentItemDisplay).toContain('ZD-01')
    expect(vm.parentItemDisplay).toContain('奔趣纸袋')
  })

  // ═══════════════════════════════════════
  // 6. parentItemDisplay 计算属性 — SPU
  // ═══════════════════════════════════════
  it('parentItemDisplay: parentId=0 → 返回空字符串', async () => {
    mockListItem.mockResolvedValue({ rows: [], total: 0 })

    const wrapper = mountItem()
    await nextTick(); await nextTick()

    const vm = wrapper.vm as any
    vm.form.parentId = 0
    vm.form.parentItemCode = 'ZD-01'
    vm.form.parentItemName = '奔趣纸袋'
    await nextTick()

    expect(vm.parentItemDisplay).toBe('')
  })
})
