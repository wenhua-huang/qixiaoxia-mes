import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'

const { ROW, mockList, mockUpdate } = vi.hoisted(() => ({
  ROW: {
    recordId: 212,
    userName: 'admin',
    nickName: '黄文华',
    workstationCode: 'BAG-01',
    workstationName: '1号制袋机',
    enableFlag: '1',
    operationTime: null,
  },
  mockList: vi.fn(),
  mockUpdate: vi.fn(),
}))

vi.mock('@/api/mes/pro/userworkstation', () => ({
  listUserWorkstation: mockList,
  updateUserWorkstation: mockUpdate,
}))

vi.mock('../components/BindDialog.vue', () => ({
  default: { name: 'BindDialog', template: '<div class="bind-dialog" />' },
}))

import UserWorkstationPage from '@/views/mes/pro/userworkstation/index.vue'

const flush = () => Promise.all([nextTick(), new Promise(r => setTimeout(r, 0))])

// el-table-column 桩把固定行作为 scope 传给默认插槽；el-switch 桩模拟「先翻 v-model 再触发 change」
let scopeRow: any
function mountPage(confirmImpl: () => Promise<void>) {
  scopeRow = { ...ROW }
  return mount(UserWorkstationPage, {
    global: {
      config: {
        globalProperties: {
          parseTime: () => '2026-09-25 02:23:39',
          $modal: {
            confirm: vi.fn(confirmImpl),
            msgSuccess: vi.fn(),
            msgError: vi.fn(),
            resetForm: vi.fn(),
            download: vi.fn(),
          },
        },
      } as any,
      directives: { hasPermi: () => {} },
      stubs: {
        'right-toolbar': { template: '<div class="right-toolbar" />' },
        'pagination': { template: '<div class="pagination" />' },
        'el-form': { template: '<div><slot /></div>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<input />' },
        'el-select': { template: '<div><slot /></div>' },
        'el-option': { template: '<div />' },
        'el-row': { template: '<div><slot /></div>' },
        'el-col': { template: '<div><slot /></div>' },
        'el-table': { template: '<div class="table"><slot /></div>' },
        'el-table-column': {
          data: () => ({ scopeRow }),
          template: '<div class="col"><slot :row="scopeRow" /></div>',
        },
        'el-button': { template: '<button><slot /></button>' },
        'el-switch': {
          props: { modelValue: String },
          emits: ['update:modelValue', 'change'],
          template: '<button type="button" class="switch" @click="flip">{{ modelValue }}</button>',
          methods: {
            flip() {
              this.$emit('update:modelValue', '0')
              this.$emit('change', scopeRow)
            },
          },
        },
      },
    },
  })
}

describe('用户工作站列表页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockList.mockResolvedValue({ rows: [ROW], total: 1 })
    mockUpdate.mockResolvedValue({ code: 200, msg: '操作成功' })
  })

  it('挂载自动查列表，返回 1 行', async () => {
    const wrapper = mountPage(() => Promise.resolve())
    await flush()
    expect(mockList).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.switch').exists()).toBe(true)
  })

  it('开关切停用：取消确认则不发更新，确认文案含人员与工位名', async () => {
    const wrapper = mountPage(() => Promise.reject(new Error('cancel')))
    await flush()
    await wrapper.find('.switch').trigger('click')
    await flush()
    const modal = wrapper.vm.$.appContext.config.globalProperties.$modal
    expect(modal.confirm).toHaveBeenCalledWith('确认要停用"黄文华"的"1号制袋机"工位绑定吗？')
    expect(mockUpdate).not.toHaveBeenCalled()
    // 取消后还原并重查
    expect(mockList).toHaveBeenCalledTimes(2)
  })

  it('开关切停用：确认则按 recordId+enableFlag 调更新并提示成功', async () => {
    const wrapper = mountPage(() => Promise.resolve())
    await flush()
    await wrapper.find('.switch').trigger('click')
    await flush()
    const modal = wrapper.vm.$.appContext.config.globalProperties.$modal
    expect(mockUpdate).toHaveBeenCalledWith({ recordId: 212, enableFlag: '0' })
    expect(modal.msgSuccess).toHaveBeenCalledWith('停用成功')
  })
})
