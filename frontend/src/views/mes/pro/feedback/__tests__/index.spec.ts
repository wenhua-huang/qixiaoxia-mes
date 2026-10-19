import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick, ref } from 'vue'

const { ROW, mockApi } = vi.hoisted(() => ({
  ROW: {
    recordId: 507,
    feedbackCode: '000FB20260920004',
    feedbackType: 'INTERNAL',
    taskId: 99079,
    workorderId: 99034,
    processId: 203,
    routeId: 200,
    workstationId: 219,
    itemId: 211,
    quantityFeedback: 1,
    quantityQualified: 1,
    status: 'AUDITED',
    remark: '原备注',
    feedbackTime: '2026-09-20 11:36:32',
  },
  mockApi: {
    listFeedback: vi.fn(),
    getFeedback: vi.fn(),
    addFeedback: vi.fn(),
    updateFeedback: vi.fn(),
    genSerialCode: vi.fn(),
  },
}))

vi.mock('@/api/mes/pro/feedback', () => ({
  listFeedback: mockApi.listFeedback,
  getFeedback: mockApi.getFeedback,
  addFeedback: mockApi.addFeedback,
  updateFeedback: mockApi.updateFeedback,
  delFeedback: vi.fn(),
  confirmFeedback: vi.fn(),
  auditFeedback: vi.fn(),
  batchConfirmFeedback: vi.fn(),
  batchAuditFeedback: vi.fn(),
  getConsumeDefaults: vi.fn(),
  getFeedbackChanges: vi.fn(),
  getInputDefault: vi.fn(),
}))
vi.mock('@/api/mes/pro/task', () => ({ getTask: vi.fn() }))
vi.mock('@/api/mes/pro/paramtemplate', () => ({ listParamTemplateByProcessId: vi.fn() }))
vi.mock('@/api/mes/sys/autocoderule', () => ({ genSerialCode: mockApi.genSerialCode }))
vi.mock('@/api/mes/pro/procard', () => ({ listProcard: vi.fn() }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('@/components/workstationSelect/single.vue', () => ({ default: { name: 'WorkstationSelect', template: '<div />' } }))
vi.mock('@/components/taskSelect/single.vue', () => ({ default: { name: 'taskSelect', template: '<div />' } }))
vi.mock('@/components/workorderSelect/single.vue', () => ({ default: { name: 'workorderSelect', template: '<div />' } }))
vi.mock('@element-plus/icons-vue', () => ({ Warning: { name: 'Warning', template: '<i />' } }))

import FeedbackPage from '@/views/mes/pro/feedback/index.vue'

const flush = () => Promise.all([nextTick(), new Promise(r => setTimeout(r, 0))])

let scopeRow: any
function mountPage() {
  scopeRow = { ...ROW }
  return mount(FeedbackPage, {
    global: {
      config: {
        globalProperties: {
          parseTime: (t: any) => t,
          useDict: (...keys: string[]) => Object.fromEntries(keys.map(k => [k, ref([])])),
          resetForm: vi.fn(),
          download: vi.fn(),
          $modal: {
            confirm: vi.fn(() => Promise.resolve()),
            msgSuccess: vi.fn(),
            msgError: vi.fn(),
            msgWarning: vi.fn(),
            alertError: vi.fn(),
          },
        },
      } as any,
      directives: { hasPermi: () => {} },
      stubs: {
        'right-toolbar': { template: '<div />' },
        'pagination': { template: '<div />' },
        'el-form': {
          template: '<form><slot /></form>',
          methods: {
            validate(cb: any) { cb(true) },
            clearValidate() {},
          },
        },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<input />' },
        'el-input-number': { template: '<input />' },
        'el-select': { template: '<div><slot /></div>' },
        'el-option': { template: '<div />' },
        'el-date-picker': { template: '<input />' },
        'el-row': { template: '<div><slot /></div>' },
        'el-col': { template: '<div><slot /></div>' },
        'el-icon': { template: '<i><slot /></i>' },
        'el-tag': { template: '<span><slot /></span>' },
        'el-empty': { template: '<div />' },
        'el-tabs': { template: '<div><slot /></div>' },
        'el-tab-pane': { template: '<div><slot /></div>' },
        'el-table': { template: '<div class="table"><slot /></div>' },
        'el-table-column': {
          data: () => ({ scopeRow }),
          template: '<div class="col"><slot :row="scopeRow" /></div>',
        },
        'el-dialog': {
          props: ['modelValue'],
          template: '<div v-if="modelValue" class="dlg"><slot /><slot name="footer" /></div>',
        },
        'el-drawer': { props: ['modelValue'], template: '<div v-if="modelValue"><slot /></div>' },
        'el-button': { template: '<button type="button"><slot /></button>' },
      },
    },
  })
}

function findBtn(wrapper: any, text: string, scope?: any) {
  const root = scope || wrapper
  const btn = root.findAll('button').find((b: any) => b.text().replace(/\s/g, '') === text)
  if (!btn) throw new Error(`button "${text}" not found`)
  return btn
}

describe('生产报工列表页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockApi.listFeedback.mockResolvedValue({ rows: [ROW], total: 1 })
    mockApi.getFeedback.mockResolvedValue({ data: { ...ROW } })
    mockApi.addFeedback.mockResolvedValue({ code: 200, msg: '操作成功' })
    mockApi.updateFeedback.mockResolvedValue({ code: 200, msg: '操作成功' })
    mockApi.genSerialCode.mockResolvedValue({ data: 'FB-NEW' })
  })

  it('编辑已审核单后不刷新直接新增：提交 payload 不携带残留 status', async () => {
    const wrapper = mountPage()
    await flush()
    // 点行内「修改」→ Object.assign(form, data) 把 status: 'AUDITED' 注入表单
    await findBtn(wrapper.find('.table'), '修改').trigger('click')
    await flush()
    // 不刷新页面，直接点「新增报工」→ reset() 必须删掉注入的 status
    await findBtn(wrapper, '新增报工').trigger('click')
    await flush()
    await findBtn(wrapper.find('.dlg'), '确定').trigger('click')
    await flush()
    expect(mockApi.addFeedback).toHaveBeenCalledTimes(1)
    const payload = mockApi.addFeedback.mock.calls[0][0]
    expect(payload).not.toHaveProperty('status')
  })

  it('对照：直接新增，提交 payload 同样不含 status', async () => {
    const wrapper = mountPage()
    await flush()
    await findBtn(wrapper, '新增报工').trigger('click')
    await flush()
    await findBtn(wrapper.find('.dlg'), '确定').trigger('click')
    await flush()
    expect(mockApi.addFeedback).toHaveBeenCalledTimes(1)
    expect(mockApi.addFeedback.mock.calls[0][0]).not.toHaveProperty('status')
  })
})
