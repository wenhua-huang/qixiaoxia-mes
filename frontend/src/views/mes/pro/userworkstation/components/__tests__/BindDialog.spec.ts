import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { ElMessageBox } from 'element-plus'

const { mockOptions, mockBatch } = vi.hoisted(() => ({
  mockOptions: vi.fn().mockResolvedValue({ data: [
    { workstationId: 205, workstationCode: 'BAG-01', workstationName: '1号制袋机', enableFlag: '1' },
    { workstationId: 206, workstationCode: 'BAG-02', workstationName: '2号制袋机', enableFlag: '1' },
  ] }),
  mockBatch: vi.fn().mockResolvedValue({ data: { successCount: 1, reactivatedCount: 0, skipCount: 0, skips: [] } }),
}))

vi.mock('@/api/mes/pro/userworkstation', () => ({
  workstationOptions: mockOptions,
  batchBindUserWorkstation: mockBatch,
}))

vi.mock('@/components/UserSelect/multi.vue', () => ({
  default: { name: 'UserMultiSelect', template: '<div class="user-picker" />' },
}))

import BindDialog from '@/views/mes/pro/userworkstation/components/BindDialog.vue'

const msgError = vi.fn()
const msgSuccess = vi.fn()

function mountDialog(props = {}) {
  return mount(BindDialog, {
    props: { showFlag: false, ...props },
    global: {
      config: { globalProperties: { $modal: { msgError, msgSuccess } } } as any,
      stubs: {
        'el-dialog': { template: '<div class="mock-dialog"><slot /><slot name="footer" /></div>' },
        'el-form': { template: '<div><slot /></div>' },
        'el-form-item': { template: '<div><slot /></div>' },
        'el-input': { template: '<textarea />' },
        'el-tag': { template: '<span class="tag"><slot /></span>' },
        'el-option': { template: '<div class="mock-option" />' },
        'el-select': {
          props: { modelValue: { type: Array, default: () => [] } },
          emits: ['update:modelValue'],
          template: '<div class="mock-select"><button type="button" class="pick-station" @click="$emit(\'update:modelValue\', [205])">pick</button><slot /></div>',
        },
        'el-button': {
          props: { loading: Boolean, disabled: Boolean },
          template: '<button :disabled="loading || disabled"><slot /></button>',
        },
      },
    },
  })
}

const flush = () => Promise.all([nextTick(), new Promise(r => setTimeout(r, 0))])

function okButton(wrapper: ReturnType<typeof mountDialog>) {
  return wrapper.findAll('button').find(b => b.text().includes('确 定'))!
}

describe('BindDialog 批量绑定弹窗', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('弹窗未打开不拉选项；每次打开都重新拉取工位选项', async () => {
    const wrapper = mountDialog()
    await flush()
    expect(mockOptions).not.toHaveBeenCalled()

    await wrapper.setProps({ showFlag: true })
    await flush()
    expect(mockOptions).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ showFlag: false })
    await wrapper.setProps({ showFlag: true })
    await flush()
    expect(mockOptions).toHaveBeenCalledTimes(2)
  })

  it('未选人员点确定：提示且不发请求', async () => {
    const wrapper = mountDialog({ showFlag: true })
    await flush()
    await wrapper.find('.pick-station').trigger('click')
    await okButton(wrapper).trigger('click')
    await flush()
    expect(msgError).toHaveBeenCalledWith('请选择绑定人员')
    expect(mockBatch).not.toHaveBeenCalled()
  })

  it('人员+工位齐备：按 {userIds, workstationIds, remark:undefined} 提交并通知父组件成功关闭', async () => {
    const wrapper = mountDialog({ showFlag: true })
    await flush()
    wrapper.findComponent({ name: 'UserMultiSelect' }).vm.$emit(
      'onSelected',
      [{ userId: 1, userName: 'admin', nickName: '黄文华' }],
    )
    await wrapper.find('.pick-station').trigger('click')
    await okButton(wrapper).trigger('click')
    await flush()

    expect(mockBatch).toHaveBeenCalledWith({ userIds: [1], workstationIds: [205], remark: undefined })
    expect(wrapper.emitted('success')).toBeTruthy()
    expect(wrapper.emitted('update:showFlag')!.at(-1)).toEqual([false])
    expect(msgSuccess).toHaveBeenCalledWith(expect.stringContaining('新增 1 条'))
  })

  it('跳过明细 HTML 转义后再交给 ElMessageBox，原始尖括号不得出现', async () => {
    mockBatch.mockResolvedValueOnce({
      data: { successCount: 0, reactivatedCount: 0, skipCount: 1, skips: ['<img src=x> / 1号制袋机'] },
    })
    const alertSpy = vi.spyOn(ElMessageBox, 'alert').mockResolvedValue(undefined as any)
    const wrapper = mountDialog({ showFlag: true })
    await flush()
    wrapper.findComponent({ name: 'UserMultiSelect' }).vm.$emit(
      'onSelected',
      [{ userId: 1, userName: 'admin', nickName: '黄文华' }],
    )
    await wrapper.find('.pick-station').trigger('click')
    await okButton(wrapper).trigger('click')
    await flush()

    expect(alertSpy).toHaveBeenCalledTimes(1)
    const [html, title] = alertSpy.mock.calls[0]
    expect(html as string).toContain('&lt;img src=x&gt;')
    expect(html as string).not.toContain('<img')
    expect(title as string).toContain('跳过已绑定 1 条')
    alertSpy.mockRestore()
  })

  it('提交失败：弹窗保持打开、不发 success，且确定按钮恢复可用', async () => {
    mockBatch.mockRejectedValueOnce(new Error('boom'))
    const wrapper = mountDialog({ showFlag: true })
    await flush()
    wrapper.findComponent({ name: 'UserMultiSelect' }).vm.$emit(
      'onSelected',
      [{ userId: 1, userName: 'admin', nickName: '黄文华' }],
    )
    await wrapper.find('.pick-station').trigger('click')
    const btn = okButton(wrapper)
    await btn.trigger('click')
    await flush()

    expect(wrapper.emitted('success')).toBeFalsy()
    expect(wrapper.emitted('update:showFlag')).toBeFalsy()
    expect(okButton(wrapper).attributes('disabled')).toBeUndefined()
  })
})
