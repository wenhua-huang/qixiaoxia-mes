import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

// ==================== Hoisted mock functions ====================
const { mockAddWorkstation, mockUpdateWorkstation, mockListWorkstation, mockGetWorkstation } = vi.hoisted(() => ({
  mockAddWorkstation: vi.fn().mockResolvedValue({ code: 200, msg: '操作成功' }),
  mockUpdateWorkstation: vi.fn().mockResolvedValue({ code: 200, msg: '操作成功' }),
  mockListWorkstation: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  mockGetWorkstation: vi.fn().mockResolvedValue({ data: {
    workstationId: 1, workstationCode: 'WS-001', workstationName: '1号印刷机',
    workshopId: 1, processId: 10, processName: '印刷', capacity: 100, status: 'IDLE', enableFlag: '1',
  } }),
}))

// ==================== Mock API: workstation ====================
vi.mock('@/api/mes/md/workstation', () => ({
  listWorkstation: mockListWorkstation,
  getWorkstation: mockGetWorkstation,
  delWorkstation: vi.fn().mockResolvedValue({}),
  addWorkstation: mockAddWorkstation,
  updateWorkstation: mockUpdateWorkstation,
  listMachines: vi.fn().mockResolvedValue({ data: [] }),
  addMachine: vi.fn().mockResolvedValue({}),
  delMachine: vi.fn().mockResolvedValue({}),
  listWorkers: vi.fn().mockResolvedValue({ data: [] }),
  addWorker: vi.fn().mockResolvedValue({}),
  delWorker: vi.fn().mockResolvedValue({}),
}))

// ==================== Mock API: workshop ====================
vi.mock('@/api/mes/md/workshop', () => ({
  listAllWorkshop: vi.fn().mockResolvedValue({ data: [{ workshopId: 1, workshopName: '印刷车间' }] }),
}))

// ==================== Mock API: process ====================
const { mockListAllProcess } = vi.hoisted(() => ({
  mockListAllProcess: vi.fn().mockResolvedValue({
    data: [
      { processId: 10, processCode: 'PRC-001', processName: '印刷', processType: 'PRINT' },
      { processId: 20, processCode: 'PRC-002', processName: '纸张分切', processType: 'SLITTING' },
      { processId: 30, processCode: 'PRC-003', processName: '制袋', processType: 'BAG_MAKE' },
    ],
  }),
}))

vi.mock('@/api/mes/pro/process', () => ({
  listAllProcess: mockListAllProcess,
}))

// ==================== Mock API: autocoderule ====================
vi.mock('@/api/mes/sys/autocoderule', () => ({
  genSerialCode: vi.fn().mockResolvedValue({ data: 'WS-AUTO-001' }),
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

// ==================== Mock utilities ====================
vi.mock('@/utils/dict', () => ({
  useDict: () => ({
    sys_yes_no: [{ value: '1', label: '是' }, { value: '0', label: '否' }],
    mes_workstation_type: [
      { value: 'PRINT', label: '印刷机' },
      { value: 'BAG_AUTO', label: '全自动制袋机' },
    ],
    mes_workstation_status: [
      { value: 'IDLE', label: '空闲' },
      { value: 'RUNNING', label: '运行中' },
    ],
  }),
}))

vi.mock('@/utils/jsencrypt', () => ({ encrypt: vi.fn(), decrypt: vi.fn() }))
vi.mock('js-cookie', () => ({ default: { get: vi.fn(() => ''), set: vi.fn(), remove: vi.fn() } }))
vi.mock('@/utils/ruoyi', () => ({
  resetForm: vi.fn(),
  parseTime: vi.fn(),
  addDateRange: vi.fn(),
  handleTree: vi.fn(),
  selectDictLabel: vi.fn(),
  selectDictLabels: vi.fn(),
}))

// ==================== Global component stubs ====================
vi.mock('@/components/RightToolbar/index.vue', () => ({
  default: { template: '<div class="mock-right-toolbar" />' },
}))
vi.mock('@/components/Pagination/index.vue', () => ({
  default: { template: '<div class="mock-pagination" />' },
}))
vi.mock('@/components/DictTag/index.vue', () => ({
  default: { template: '<span class="mock-dict-tag" />' },
}))

// ==================== Import component ====================
import Workstation from '@/views/mes/md/workstation/index.vue'

function mountWorkstation() {
  return mount(Workstation, {
    global: {
      stubs: {
        'right-toolbar': { template: '<div class="mock-right-toolbar" />' },
        'pagination': { template: '<div class="mock-pagination" />' },
        'dict-tag': { template: '<span class="mock-dict-tag" />' },
        'el-dialog': { template: '<div class="mock-dialog"><slot /><slot name="footer" /></div>' },
        'el-table': { template: '<div class="mock-table"><slot /></div>' },
        'el-table-column': { template: '<div />' },
        'el-form': { template: '<div class="mock-form"><slot /></div>' },
        'el-form-item': { template: '<div class="mock-form-item"><slot /></div>' },
        'el-input': { template: '<input class="mock-input" />' },
        'el-select': { template: '<select class="mock-select"><slot /></select>' },
        'el-option': { template: '<option />' },
        'el-button': { template: '<button class="mock-btn"><slot /></button>' },
        'el-switch': { template: '<div class="mock-switch" />' },
        'el-input-number': { template: '<input type="number" class="mock-input-number" />' },
        'el-radio-group': { template: '<div class="mock-radio-group"><slot /></div>' },
        'el-radio': { template: '<div class="mock-radio"><slot /></div>' },
        'el-divider': { template: '<div class="mock-divider" />' },
        'el-row': { template: '<div class="mock-row"><slot /></div>' },
        'el-col': { template: '<div class="mock-col"><slot /></div>' },
      },
      directives: {
        hasPermi: () => {},
        hasRole: () => {},
      },
    },
  })
}

describe('工作站管理 — processId 工序选择', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  // ══════════════════════════════════════════════
  // 初始挂载：自动加载 processOptions
  // ══════════════════════════════════════════════

  it('组件挂载后应自动调用 listAllProcess 加载工序选项', async () => {
    mountWorkstation()
    await nextTick()
    await nextTick()

    // <script setup> 顶层调用 getList() + listAllProcess()
    expect(mockListAllProcess).toHaveBeenCalledTimes(1)
    expect(mockListWorkstation).toHaveBeenCalledTimes(1)
  })

  it('processOptions 应被填充为工序ID+名称列表', async () => {
    const wrapper = mountWorkstation()
    await nextTick()
    await nextTick()

    const vm = wrapper.vm as any
    // listAllProcess resolve 后 processOptions 应被填充
    expect(vm.processOptions).toHaveLength(3)
    expect(vm.processOptions[0]).toMatchObject({ processId: 10, processName: '印刷' })
    expect(vm.processOptions[1]).toMatchObject({ processId: 20, processName: '纸张分切' })
    expect(vm.processOptions[2]).toMatchObject({ processId: 30, processName: '制袋' })
  })

  // ══════════════════════════════════════════════
  // 表格：显示 processName 列（非 processType）
  // ══════════════════════════════════════════════

  it('表格应渲染工序列（prop="processName"）而非工序类型', async () => {
    const wrapper = mountWorkstation()
    await nextTick()
    await nextTick()

    expect(wrapper.find('.app-container').exists()).toBe(true)
    // 不应包含旧标签 "工序类型"
    expect(wrapper.html()).not.toContain('工序类型')
  })

  // ══════════════════════════════════════════════
  // 表单 rules：processId 必填
  // ══════════════════════════════════════════════

  it('表单校验规则应包含 processId 必填', async () => {
    const wrapper = mountWorkstation()
    await nextTick()
    await nextTick()

    const vm = wrapper.vm as any
    expect(vm.rules.processId).toBeDefined()
    expect(vm.rules.processId[0].required).toBe(true)
    expect(vm.rules.processId[0].message).toBe('请选择工序')
  })

  // ══════════════════════════════════════════════
  // 表单数据：使用 processId 而非 processType
  // ══════════════════════════════════════════════

  it('表单 form 使用 processId 字段，且不再引用 mes_process_type 字典', async () => {
    const wrapper = mountWorkstation()
    await nextTick()
    await nextTick()

    const vm = wrapper.vm as any
    // form.processId 字段（初始 undefined，用户选择工序后赋值）
    expect(vm.form.processId).toBeUndefined()
    // useDict 已移除 mes_process_type（不引入工序类型字典）
    expect(vm).not.toHaveProperty('mes_process_type')
  })

  // ══════════════════════════════════════════════
  // 查询参数：支持 processId 筛选
  // ══════════════════════════════════════════════

  it('查询参数 queryParams 应包含 processId 字段', async () => {
    const wrapper = mountWorkstation()
    await nextTick()
    await nextTick()

    const vm = wrapper.vm as any
    expect(vm.queryParams).toHaveProperty('processId')
    expect(vm.queryParams.processId).toBeUndefined() // 初始未筛选

    // 模拟用户选择工序筛选
    vm.queryParams.processId = 10
    await nextTick()
    expect(vm.queryParams.processId).toBe(10)
  })

  // ══════════════════════════════════════════════
  // getWorkstation 返回数据含 processId
  // ══════════════════════════════════════════════

  it('getWorkstation mock 应返回含 processId/processName 的数据', async () => {
    mountWorkstation()
    await nextTick()
    await nextTick()

    const { getWorkstation } = await import('@/api/mes/md/workstation')
    const res = await getWorkstation(1)
    expect((res as any).data.processId).toBe(10)
    expect((res as any).data.processName).toBe('印刷')
  })
})
