import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import LineEdit from '../LineEdit.vue'

const mockResolve = vi.fn()
const mockListBindings = vi.fn()
vi.mock('@/api/mes/pro/routeproduct', () => ({
  listRouteProduct: (...a: any[]) => mockListBindings(...a),
  resolveRouteProduct: (...a: any[]) => mockResolve(...a),
}))
vi.mock('@/api/mes/pro/proroute', () => ({ listRoute: vi.fn().mockResolvedValue({ rows: [] }) }))
vi.mock('@/api/mes/md/item', () => ({ getItem: vi.fn().mockResolvedValue({ data: {} }) }))
vi.mock('@/api/mes/md/attr', () => ({ getEffAttrSchema: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/components/itemSelect/single.vue', () => ({ default: { name: 'ItemSelect', template: '<div />' } }))

async function mountLine(line: any = null, extraProps: any = {}) {
  const wrapper = mount(LineEdit, {
    props: { modelValue: false, line, orderType: 'STANDARD', outsourceFlag: 'N', packageFlag: 'N', ...extraProps },
    global: {
      stubs: { ExtAttrForm: true },
      mocks: { parseTime: (t: any) => (t ? String(t) : '') },
      config: { globalProperties: {} } as any,
    },
  })
  // 真实用法是 false→true 翻转触发 initForm(watch 无 immediate)
  await wrapper.setProps({ modelValue: true })
  await nextTick()
  return wrapper
}

function bindingRow(id: number, routeId: number, name: string, isDefault = 'N') {
  return { recordId: id, routeId, itemId: 100, itemCode: 'P1', itemName: '演示产品', _routeName: name, isDefault }
}

describe('SalOrder LineEdit.vue 工艺路线带出', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('选产品后按头维度解析并默认选中命中路线', async () => {
    mockListBindings.mockResolvedValue({ rows: [bindingRow(2, 20, '标准路线', 'Y')] })
    mockResolve.mockResolvedValue({ data: { matched: true, hardBlocked: false, routeProductId: 2, routeId: 20 } })
    const wrapper = await mountLine()
    ;(wrapper.vm as any).onProductSelected({ itemId: 100, itemCode: 'P1', itemName: '演示产品', unitOfMeasure: 'PCS', unitName: '个' })
    await nextTick(); await nextTick(); await new Promise(r => setTimeout(r, 0))
    expect((wrapper.vm as any).form.routeProductId).toBe(2)
    expect((wrapper.vm as any).routeBlocked).toBe(false)
  })

  it('外发硬阻断时置阻断态且 confirm 不提交', async () => {
    mockListBindings.mockResolvedValue({ rows: [] })
    mockResolve.mockResolvedValue({ data: { matched: false, hardBlocked: true, message: '未配置外发路线' } })
    const wrapper = await mountLine(null, { outsourceFlag: 'Y' })
    ;(wrapper.vm as any).onProductSelected({ itemId: 100, itemCode: 'P1', itemName: '演示产品', unitOfMeasure: 'PCS', unitName: '个' })
    await nextTick(); await new Promise(r => setTimeout(r, 0))
    expect((wrapper.vm as any).routeBlocked).toBe(true)
    const before = wrapper.emitted('confirm')?.length || 0
    ;(wrapper.vm as any).confirm()
    await nextTick()
    expect((wrapper.emitted('confirm')?.length || 0)).toBe(before)
  })

  it('编辑已有行保留手选路线, 不重新解析', async () => {
    mockListBindings.mockResolvedValue({ rows: [bindingRow(9, 90, '手选路线')] })
    const wrapper = await mountLine({ lineId: 5, productId: 100, productCode: 'P1', productName: '演示产品', quantity: 1, routeProductId: 9, routeName: '手选路线' })
    await nextTick(); await new Promise(r => setTimeout(r, 0))
    expect(mockResolve).not.toHaveBeenCalled()
    expect((wrapper.vm as any).form.routeProductId).toBe(9)
  })
})
