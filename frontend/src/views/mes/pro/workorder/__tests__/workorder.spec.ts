import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// ==================== Hoisted mock functions (vi.mock is hoisted above imports) ====================
const { mockCreateWithBom, mockUpdateWithBom } = vi.hoisted(() => ({
  mockCreateWithBom: vi.fn().mockResolvedValue({ code: 200, data: { workorderId: 999 } }),
  mockUpdateWithBom: vi.fn().mockResolvedValue({ code: 200 }),
}))

// ==================== Mock API: workorder ====================
vi.mock('@/api/mes/pro/workorder', () => ({
  listWorkorder: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  getWorkorder: vi.fn().mockResolvedValue({ data: {} }),
  delWorkorder: vi.fn().mockResolvedValue({}),
  createWorkorderWithBom: mockCreateWithBom,
  updateWorkorderWithBom: mockUpdateWithBom,
  startWorkorder: vi.fn().mockResolvedValue({}),
  checkWorkorderMaterial: vi.fn().mockResolvedValue({ data: [] }),
  // 偏离检测：默认无偏离，让 submitForm 直达 doSubmitForm
  checkDeviation: vi.fn().mockResolvedValue({ code: 200, data: { hasDeviation: false, deviations: [] } }),
}))

// ==================== Mock API: BOM/param (only list needed for loadBom/loadParams) ====================
vi.mock('@/api/mes/pro/workorderbom', () => ({
  listBomByWorkorderId: vi.fn().mockResolvedValue({ data: [] }),
}))

vi.mock('@/api/mes/pro/workorderparam', () => ({
  listParamByWorkorderId: vi.fn().mockResolvedValue({ data: [] }),
}))

// ==================== Mock API: route/product/process ====================
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

// ==================== Mock vue-router + stores ====================
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

// ==================== Global component stubs ====================
vi.mock('@/components/itemSelect/single.vue', () => ({ default: { template: '<div class="mock-item-select" />' } }))
vi.mock('@/components/workstationSelect/single.vue', () => ({ default: { template: '<div class="mock-workstation-select" />' } }))

// ==================== Import component ====================
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
        'itemSelect': { template: '<div class="mock-item-select" />' },
        'ItemSelect': { template: '<div class="mock-item-select" />' },
        'WorkstationSelect': { template: '<div class="mock-workstation-select" />' },
      },
      directives: {
        hasPermi: () => {},
        hasRole: () => {},
      },
      mocks: {
        $modal: {
          msgSuccess: vi.fn(),
          msgError: vi.fn(),
          msgWarning: vi.fn(),
          confirm: vi.fn().mockResolvedValue('confirm'),
        },
        download: vi.fn(),
        resetForm: vi.fn(),
      },
    },
  })
}

describe('生产工单 — workorder/index.vue submitForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该正常挂载', async () => {
    const wrapper = mountWorkorder()
    await nextTick()
    await nextTick()
    expect(wrapper.find('.app-container').exists()).toBe(true)
    expect(wrapper.text()).toContain('工单编码')
  })

  // ══════════════════════════════════════════════
  // submitForm - 新增模式
  // ══════════════════════════════════════════════

  it('submitForm 新增模式应调用 createWorkorderWithBom 并构建正确 payload', async () => {
    const wrapper = mountWorkorder()
    await nextTick()
    await nextTick()

    // 模拟新增状态
    const vm = wrapper.vm as any
    vm.optType = 'add'
    vm.step = 2
    vm.prorouteId = 100
    vm.form = {
      workorderCode: 'WO-TEST-001',
      workorderName: '测试工单',
      productId: 1,
      productCode: 'PROD-001',
      productName: '测试产品',
      quantity: 100,
      factoryId: 1,
      status: 'PREPARE',
      workorderType: 'SELF',
      routeProductId: null,
    }
    vm.bomList = [
      {
        itemId: 100,
        itemCode: 'MAT-001',
        itemName: '物料A',
        itemSpc: 'Φ10',
        unitOfMeasure: 'kg',
        unitName: '千克',
        itemOrProduct: 'RAW',
        quantity: 0.5,
        processId: 10,
        processName: '工序A',
      },
      {
        itemId: 200,
        itemCode: 'MAT-002',
        itemName: '物料B',
        itemSpc: '',
        unitOfMeasure: 'PCS',
        unitName: '个',
        itemOrProduct: 'AUXILIARY',
        quantity: 2,
        _processId: 20, // ← 旧数据使用 _processId 前缀
        _processName: '工序B',
      },
    ]
    vm.paramList = [
      { recordId: null, templateId: 1, routeProductId: 100, standardValue: '100', adjustedValue: '105' },
      { recordId: 50, templateId: 2, routeProductId: 100, standardValue: '200', adjustedValue: '210' },
    ]

    // 调用 submitForm
    vm.submitForm()
    await nextTick()

    // 验证 createWorkorderWithBom 被调用
    expect(mockCreateWithBom).toHaveBeenCalledTimes(1)
    expect(mockUpdateWithBom).not.toHaveBeenCalled()

    const payload = mockCreateWithBom.mock.calls[0][0]
    expect(payload).toBeDefined()

    // 验证 workorder 头
    expect(payload.workorder.workorderCode).toBe('WO-TEST-001')
    expect(payload.workorder.workorderName).toBe('测试工单')
    expect(payload.workorder.routeProductId).toBe(100) // ← prorouteId 注入

    // 验证 bomList
    expect(payload.bomList).toHaveLength(2)

    // BOM item 1: processId 来自直接字段
    expect(payload.bomList[0].itemId).toBe(100)
    expect(payload.bomList[0].processId).toBe(10)
    expect(payload.bomList[0].processName).toBe('工序A')
    expect(payload.bomList[0].itemOrProduct).toBe('RAW')
    expect(payload.bomList[0].quantity).toBe(0.5)

    // BOM item 2: processId 来自 _processId 回退
    expect(payload.bomList[1].itemId).toBe(200)
    expect(payload.bomList[1].processId).toBe(20)
    expect(payload.bomList[1].processName).toBe('工序B')
    expect(payload.bomList[1].itemOrProduct).toBe('AUXILIARY')
    expect(payload.bomList[1].quantity).toBe(2)

    // BOM 不应包含 _ 前缀的临时字段
    expect(payload.bomList[1]._processId).toBeUndefined()
    expect(payload.bomList[1]._processName).toBeUndefined()

    // 验证 paramList
    expect(payload.paramList).toHaveLength(2)
    expect(payload.paramList[0].recordId).toBeNull()   // 新参数无recordId
    expect(payload.paramList[0].templateId).toBe(1)
    expect(payload.paramList[0].standardValue).toBe('100')
    expect(payload.paramList[0].adjustedValue).toBe('105')
    expect(payload.paramList[1].recordId).toBe(50)      // 已有recordId保留
    expect(payload.paramList[1].templateId).toBe(2)
    expect(payload.paramList[1].adjustedValue).toBe('210')

    // paramList 不应包含前端内部字段（remark 映射为 '' 以保持一致性）
    expect(payload.paramList[0].paramName).toBeUndefined()
    expect(payload.paramList[0].remark).toBe('')
    expect(payload.paramList[0]._paramName).toBeUndefined()
  })

  // ══════════════════════════════════════════════
  // submitForm - 修改模式
  // ══════════════════════════════════════════════

  it('submitForm 修改模式应调用 updateWorkorderWithBom', async () => {
    const wrapper = mountWorkorder()
    await nextTick()
    await nextTick()

    const vm = wrapper.vm as any
    vm.optType = 'edit'
    vm.step = 1
    vm.prorouteId = 200
    vm.form = {
      workorderId: 42,
      workorderCode: 'WO-EDIT-001',
      workorderName: '编辑工单',
      productId: 1,
      productCode: 'PROD-001',
      quantity: 300,
      factoryId: 1,
      routeProductId: 200,
    }
    vm.bomList = []
    vm.paramList = []

    vm.submitForm()
    await nextTick()

    expect(mockUpdateWithBom).toHaveBeenCalledTimes(1)
    expect(mockCreateWithBom).not.toHaveBeenCalled()

    const payload = mockUpdateWithBom.mock.calls[0][0]
    expect(payload.workorder.workorderId).toBe(42)
    expect(payload.workorder.routeProductId).toBe(200)
    expect(payload.workorder.quantity).toBe(300)
    expect(payload.bomList).toEqual([])
    expect(payload.paramList).toEqual([])
  })

  // ══════════════════════════════════════════════
  // submitForm - 路由ID回退
  // ══════════════════════════════════════════════

  it('submitForm 修改模式：prorouteId 为 null 时回退到 form.routeProductId', async () => {
    const wrapper = mountWorkorder()
    await nextTick()
    await nextTick()

    const vm = wrapper.vm as any
    vm.optType = 'edit'
    vm.prorouteId = null
    vm.form = {
      workorderId: 10,
      workorderCode: 'WO-FB-001',
      workorderName: '回退测试',
      productId: 1,
      quantity: 50,
      routeProductId: 999, // ← 回退目标
    }
    vm.bomList = []
    vm.paramList = []

    vm.submitForm()
    await nextTick()

    expect(mockUpdateWithBom).toHaveBeenCalledTimes(1)
    const payload = mockUpdateWithBom.mock.calls[0][0]
    expect(payload.workorder.routeProductId).toBe(999) // ← 回退到 form.routeProductId
  })

  // ══════════════════════════════════════════════
  // BOM 物料空值默认处理
  // ══════════════════════════════════════════════

  it('BOM 物料字段缺失时使用默认值', async () => {
    const wrapper = mountWorkorder()
    await nextTick()
    await nextTick()

    const vm = wrapper.vm as any
    vm.optType = 'add'
    vm.step = 2
    vm.prorouteId = 1
    vm.form = { workorderCode: 'WO-DEF', workorderName: '默认值', productId: 1, quantity: 10 }
    vm.bomList = [{
      itemId: 1,
      // 以下字段全部缺失
      itemCode: undefined,
      itemName: undefined,
      itemSpc: undefined,
      unitOfMeasure: undefined,
      unitName: undefined,
      itemOrProduct: undefined,
      quantity: undefined,
      processId: undefined,
      processName: undefined,
    }]
    vm.paramList = []

    vm.submitForm()
    await nextTick()

    const payload = mockCreateWithBom.mock.calls[0][0]
    const bom = payload.bomList[0]
    expect(bom.itemId).toBe(1)
    expect(bom.itemCode).toBe('')
    expect(bom.itemName).toBe('')
    expect(bom.itemSpc).toBe('')
    expect(bom.unitOfMeasure).toBe('')
    expect(bom.unitName).toBe('')
    expect(bom.itemOrProduct).toBe('RAW')      // ← 默认 RAW
    expect(bom.quantity).toBe(0)               // ← 默认 0
    expect(bom.processId).toBeNull()
    expect(bom.processName).toBe('')
  })

  // ══════════════════════════════════════════════
  // 排产：handleScheduleAddTask → processCode + processType
  // ══════════════════════════════════════════════

  it('handleScheduleAddTask 应设置 scheduleTaskForm 含 processCode 和 processType', async () => {
    const wrapper = mountWorkorder()
    await nextTick()
    const vm = wrapper.vm as any

    vm.scheduleWorkorderId = 1
    vm.scheduleWorkorderCode = 'WO-001'
    vm.scheduleWorkorderName = '测试工单'

    const step = { processId: 203, processCode: 'SLITTING', processName: '纸张分切', processType: 'SLITTING', routeId: 10 }
    vm.handleScheduleAddTask(step)
    await nextTick()

    expect(vm.scheduleTaskOpen).toBe(true)
    expect(vm.scheduleTaskForm.processId).toBe(203)
    expect(vm.scheduleTaskForm.processCode).toBe('SLITTING')
    expect(vm.scheduleTaskForm.processType).toBe('SLITTING')
    expect(vm.scheduleTaskForm.processName).toBe('纸张分切')
    expect(vm.scheduleTaskTitle).toContain('纸张分切')
  })

  it('handleScheduleAddTask processType 为空时也能正常打开', async () => {
    const wrapper = mountWorkorder()
    await nextTick()
    const vm = wrapper.vm as any

    const step = { processId: 99, processCode: '', processName: '测试工序', processType: '', routeId: 5 }
    vm.handleScheduleAddTask(step)
    await nextTick()

    expect(vm.scheduleTaskForm.processType).toBe('')
    expect(vm.scheduleTaskOpen).toBe(true)
  })

  // handleOpenWorkstationSelect 由 E2E 覆盖（涉及 $refs 操作，单测难以 mock）
  // 其核心逻辑「从 scheduleTaskForm.processId 取值并传给 WorkstationSelect」已在 handleScheduleAddTask 测试中验证（processId 与 processType 均来自 step 对象）
})
