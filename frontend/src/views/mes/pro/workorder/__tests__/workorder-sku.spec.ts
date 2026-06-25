import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// ═══════════════════════════════════════
// Hoisted mock functions (Pattern B)
// ═══════════════════════════════════════
const { mockCreateWithBom, mockCheckDeviation } = vi.hoisted(() => ({
  mockCreateWithBom: vi.fn().mockResolvedValue({ code: 200, data: { workorderId: 999 } }),
  mockCheckDeviation: vi.fn(),
}))

// ═══════════════════════════════════════
// Mock API modules
// ═══════════════════════════════════════
vi.mock('@/api/mes/pro/workorder', () => ({
  listWorkorder: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getWorkorderDetail: vi.fn().mockResolvedValue({ data: {} }),
  delWorkorder: vi.fn().mockResolvedValue({}),
  createWorkorderWithBom: mockCreateWithBom,
  updateWorkorderWithBom: vi.fn().mockResolvedValue({ code: 200 }),
  startWorkorder: vi.fn().mockResolvedValue({}),
  checkWorkorderMaterial: vi.fn().mockResolvedValue({ data: [] }),
  startWorkorderWithCheck: vi.fn().mockResolvedValue({ data: [] }),
  checkDeviation: mockCheckDeviation,
}))

vi.mock('@/api/mes/pro/workorderbom', () => ({
  listBomByWorkorderId: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/pro/workorderparam', () => ({
  listParamByWorkorderId: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/pro/routeproduct', () => ({
  listRouteProduct: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  listRouteProductByRouteId: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/pro/routeprocess', () => ({
  listRouteProcessByRouteId: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/pro/proroute', () => ({
  listRoute: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
}))
vi.mock('@/api/mes/pro/process', () => ({
  listAllProcess: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/pro/routeproductbom', () => ({
  listRouteProductBomByRouteId: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/pro/routeprocessparam', () => ({
  listRouteProcessParamByRouteProductId: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/mes/pro/paramtemplate', () => ({
  listParamTemplate: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
}))
vi.mock('@/api/mes/sys/autocoderule', () => ({
  genSerialCode: vi.fn().mockResolvedValue({ data: 'WO-AUTO-001' }),
}))
vi.mock('@/api/mes/pro/task', () => ({
  listTask: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  addTask: vi.fn().mockResolvedValue({}),
  updateTask: vi.fn().mockResolvedValue({}),
  delTask: vi.fn().mockResolvedValue({}),
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
vi.mock('@/utils/dict', () => ({ useDict: () => ({}) }))
vi.mock('@/components/itemSelect/single.vue', () => ({ default: { template: '<div class="mock-item-select" />' } }))
vi.mock('@/components/workstationSelect/single.vue', () => ({ default: { template: '<div class="mock-workstation-select" />' } }))

// ═══════════════════════════════════════
// Import component
// ═══════════════════════════════════════
import Workorder from '@/views/mes/pro/workorder/index.vue'

function mountWorkorder() {
  return mount(Workorder, {
    global: {
      stubs: {
        'right-toolbar': { template: '<div class="mock-right-toolbar" />' },
        'pagination': { template: '<div class="mock-pagination" />' },
        'dict-tag': { template: '<span class="mock-dict-tag" />' },
        'el-dialog': { template: '<div class="mock-dialog"><slot /><slot name="footer" /></div>' },
        'el-drawer': { template: '<div />' },
        'el-popover': { template: '<div />' },
        'el-tooltip': { template: '<div />' },
        'el-steps': { template: '<div class="mock-steps" />' },
        'el-step': { template: '<div class="mock-step" />' },
        'el-collapse': { template: '<div class="mock-collapse"><slot /></div>' },
        'el-collapse-item': { template: '<div class="mock-collapse-item"><slot /></div>' },
        'el-empty': { template: '<div class="mock-empty" />' },
        'el-checkbox': { template: '<input type="checkbox" class="mock-checkbox" />' },
        'el-input-number': { template: '<input type="number" class="mock-input-number" />' },
        'el-link': { template: '<a class="mock-link"><slot /></a>' },
        'el-row': { template: '<div class="mock-row"><slot /></div>' },
        'el-col': { template: '<div class="mock-col"><slot /></div>' },
        'el-radio-group': { template: '<div class="mock-radio-group"><slot /></div>' },
        'el-radio': { template: '<div class="mock-radio"><slot /></div>' },
        'el-alert': { template: '<div class="mock-alert"><slot /></div>' },
        'el-tag': { template: '<span class="mock-tag"><slot /></span>' },
        'el-divider': { template: '<div class="mock-divider" />' },
        'el-descriptions': { template: '<div />' },
        'itemSelect': { template: '<div class="mock-item-select" />' },
        'ItemSelect': { template: '<div class="mock-item-select" />' },
        'WorkstationSelect': { template: '<div class="mock-workstation-select" />' },
      },
      directives: { hasPermi: () => {}, hasRole: () => {} },
      mocks: {
        $modal: { msgSuccess: vi.fn(), msgError: vi.fn(), msgWarning: vi.fn(), confirm: vi.fn().mockResolvedValue('confirm') },
        download: vi.fn(), resetForm: vi.fn(),
      },
    },
  })
}

describe('生产工单 — SKU变体创建', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // Helper to set up form state
  function setupAddMode(vm: any, overrides: any = {}) {
    vm.optType = 'add'
    vm.step = 2
    vm.prorouteId = overrides.prorouteId || 100
    vm.form = {
      workorderCode: overrides.code || 'WO-SKU-001', workorderName: '测试工单',
      productId: 1, productCode: 'ZD-01', productName: '奔趣纸袋',
      quantity: 100, factoryId: 1, ...overrides.form,
    }
    vm.bomList = overrides.bomList || [
      { itemId: 10, itemCode: 'INK-01', itemName: '水性油墨', quantity: 0.8, unitName: 'kg', _processId: 1 },
    ]
    vm.paramList = overrides.paramList || []
    vm.routeProcesses = overrides.routeProcesses || [{ processId: 1, processName: '印刷', orderNum: 1 }]
  }

  // ═══════════════════════════════════════
  // 1. 后端检测有偏离 → 打开弹窗
  // ═══════════════════════════════════════
  it('后端返回偏离 → 应打开 SKU 对话框', async () => {
    mockCheckDeviation.mockResolvedValue({
      code: 200, data: { hasDeviation: true, deviations: [
        { source: 'BOM', itemCode: 'INK-01', itemName: '水性油墨', standardVal: '0.5', actualVal: '0.8', diffLabel: '用量变更' }
      ]}
    })
    const wrapper = mountWorkorder()
    await nextTick(); await nextTick()
    const vm = wrapper.vm as any
    setupAddMode(vm)

    vm.submitForm()
    await nextTick(); await nextTick()

    expect(vm.skuDialogOpen).toBe(true)
    expect(vm.skuDeviationList[0].diffLabel).toBe('用量变更')
    expect(mockCreateWithBom).not.toHaveBeenCalled()
  })

  // ═══════════════════════════════════════
  // 2. 后端检测无偏离 → 直接提交
  // ═══════════════════════════════════════
  it('后端返回无偏离 → 直接提交，不弹窗', async () => {
    mockCheckDeviation.mockResolvedValue({
      code: 200, data: { hasDeviation: false, deviations: [] }
    })
    const wrapper = mountWorkorder()
    await nextTick(); await nextTick()
    const vm = wrapper.vm as any
    setupAddMode(vm)

    vm.submitForm()
    await nextTick(); await nextTick()

    expect(vm.skuDialogOpen).toBe(false)
    expect(mockCreateWithBom).toHaveBeenCalledTimes(1)
  })

  // ═══════════════════════════════════════
  // 3. 选"是" → payload 含变体字段
  // ═══════════════════════════════════════
  it('SKU对话框选"是" → payload 含 createSkuVariant=true', async () => {
    mockCheckDeviation.mockResolvedValue({
      code: 200, data: { hasDeviation: true, deviations: [
        { source: 'BOM', itemCode: 'INK-01', itemName: '水性油墨', standardVal: '0.5', actualVal: '0.8', diffLabel: '用量变更' }
      ]}
    })
    const wrapper = mountWorkorder()
    await nextTick(); await nextTick()
    const vm = wrapper.vm as any
    setupAddMode(vm, { code: 'WO-SKU-003', form: { productId: 201 } })

    vm.submitForm()
    await nextTick(); await nextTick()
    expect(vm.skuDialogOpen).toBe(true)

    vm.skuChoice = 'yes'
    vm.skuCode = 'ZD-01-V1'
    vm.skuName = '奔趣-特种油墨'
    vm.confirmSkuDialog()
    await nextTick()

    expect(mockCreateWithBom).toHaveBeenCalledTimes(1)
    const payload = mockCreateWithBom.mock.calls[0][0]
    expect(payload.workorder.createSkuVariant).toBe(true)
    expect(payload.workorder.skuCode).toBe('ZD-01-V1')
  })

  // ═══════════════════════════════════════
  // 4. 选"否" → payload 含 createSkuVariant=false
  // ═══════════════════════════════════════
  it('SKU对话框选"否" → payload 含 createSkuVariant=false', async () => {
    mockCheckDeviation.mockResolvedValue({
      code: 200, data: { hasDeviation: true, deviations: [
        { source: 'BOM', itemCode: 'INK-01', itemName: '水性油墨', standardVal: '0.5', actualVal: '0.8', diffLabel: '用量变更' }
      ]}
    })
    const wrapper = mountWorkorder()
    await nextTick(); await nextTick()
    const vm = wrapper.vm as any
    setupAddMode(vm, { code: 'WO-SKU-004' })

    vm.submitForm()
    await nextTick(); await nextTick()
    expect(vm.skuDialogOpen).toBe(true)

    vm.skuChoice = 'no'
    vm.confirmSkuDialog()
    await nextTick()

    expect(mockCreateWithBom).toHaveBeenCalledTimes(1)
    const payload = mockCreateWithBom.mock.calls[0][0]
    expect(payload.workorder.createSkuVariant).toBe(false)
  })

  // ═══════════════════════════════════════
  // 5. 取消SKU对话框 → 不提交
  // ═══════════════════════════════════════
  it('SKU对话框取消 → 不提交', async () => {
    mockCheckDeviation.mockResolvedValue({
      code: 200, data: { hasDeviation: true, deviations: [
        { source: 'BOM', itemCode: 'INK-01', itemName: '水性油墨', standardVal: '0.5', actualVal: '0.8', diffLabel: '用量变更' }
      ]}
    })
    const wrapper = mountWorkorder()
    await nextTick(); await nextTick()
    const vm = wrapper.vm as any
    setupAddMode(vm)

    vm.submitForm()
    await nextTick(); await nextTick()
    expect(vm.skuDialogOpen).toBe(true)

    vm.cancelSkuDialog()
    await nextTick()
    expect(vm.skuDialogOpen).toBe(false)
    expect(mockCreateWithBom).not.toHaveBeenCalled()
  })

  // ═══════════════════════════════════════
  // 6. 编辑模式也触发偏离检测
  // ═══════════════════════════════════════
  it('编辑模式 → 也调后端检测 → 有偏离则弹窗', async () => {
    mockCheckDeviation.mockResolvedValue({
      code: 200, data: { hasDeviation: true, deviations: [
        { source: 'BOM', itemCode: 'INK-01', itemName: '水性油墨', standardVal: '0.5', actualVal: '1.2', diffLabel: '用量变更' }
      ]}
    })
    const wrapper = mountWorkorder()
    await nextTick(); await nextTick()
    const vm = wrapper.vm as any
    vm.optType = 'edit'
    vm.step = 2
    vm.prorouteId = 200
    vm.form = { workorderId: 42, workorderCode: 'WO-EDIT', productId: 1, productCode: 'ZD-01', productName: '奔趣纸袋', quantity: 300, factoryId: 1 }
    vm.bomList = [{ itemId: 10, itemCode: 'INK-01', quantity: 1.2, _processId: 1 }]
    vm.paramList = []
    vm.routeProcesses = [{ processId: 1, processName: '印刷', orderNum: 1 }]

    vm.submitForm()
    await nextTick(); await nextTick()
    expect(vm.skuDialogOpen).toBe(true)
  })
})
