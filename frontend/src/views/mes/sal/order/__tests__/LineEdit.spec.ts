import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import LineEdit from '../LineEdit.vue'

const mockResolve = vi.fn()
const mockListBindings = vi.fn()
const mockListRoute = vi.fn()
vi.mock('@/api/mes/pro/routeproduct', () => ({
  listRouteProduct: (...a: any[]) => mockListBindings(...a),
  resolveRouteProduct: (...a: any[]) => mockResolve(...a),
}))
vi.mock('@/api/mes/pro/proroute', () => ({ listRoute: (...a: any[]) => mockListRoute(...a) }))
vi.mock('@/api/mes/md/item', () => ({ getItem: vi.fn().mockResolvedValue({ data: {} }) }))
vi.mock('@/api/mes/md/attr', () => ({ getEffAttrSchema: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/components/itemSelect/single.vue', () => ({ default: { name: 'ItemSelect', template: '<div />' } }))

/** 可控 Promise, 用于构造响应乱序 */
function deferred<T = any>() {
  let resolve!: (v: T) => void
  const promise = new Promise<T>(r => { resolve = r })
  return { promise, resolve }
}

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
  beforeEach(() => {
    vi.clearAllMocks()
    mockListRoute.mockResolvedValue({ rows: [] })
  })

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

  it('改选产品时立即清空旧产品路线快照, 无跨产品路线时间窗', async () => {
    mockListBindings.mockReturnValue(new Promise(() => {})) // 候选永不返回, 模拟加载中
    mockResolve.mockReturnValue(new Promise(() => {}))
    const wrapper = await mountLine({ lineId: 5, productId: 100, productName: '旧产品', quantity: 1, routeProductId: 9, routeName: '旧路线' })
    ;(wrapper.vm as any).onProductSelected({ itemId: 200, itemCode: 'P2', itemName: '新产品', unitOfMeasure: 'PCS', unitName: '个' })
    expect((wrapper.vm as any).form.routeProductId).toBeNull()
    expect((wrapper.vm as any).form.routeName).toBeNull()
  })

  it('快速 A→B 切换产品: A 的晚到候选/解析结果被丢弃, 不落 B 行', async () => {
    const bindA = deferred(); const bindB = deferred()
    const resolveA = deferred(); const resolveB = deferred()
    mockListBindings.mockImplementation((q: any) => q.itemId === 100 ? bindA.promise : bindB.promise)
    mockResolve.mockImplementation((q: any) => q.itemId === 100 ? resolveA.promise : resolveB.promise)
    const wrapper = await mountLine()
    const vm = wrapper.vm as any

    vm.onProductSelected({ itemId: 100, itemCode: 'P1', itemName: '产品A', unitOfMeasure: 'PCS', unitName: '个' })
    await nextTick()
    vm.onProductSelected({ itemId: 200, itemCode: 'P2', itemName: '产品B', unitOfMeasure: 'PCS', unitName: '个' })
    await nextTick()

    // B 候选先返回并完成解析
    bindB.resolve({ rows: [{ recordId: 3, routeId: 30, itemId: 200, itemName: '产品B', _routeName: 'B路线' }] })
    await Promise.resolve(); await nextTick()
    resolveB.resolve({ data: { matched: true, routeProductId: 3, routeId: 30 } })
    await Promise.resolve(); await nextTick()
    expect(vm.form.productId).toBe(200)
    expect(vm.form.routeProductId).toBe(3)

    // A 的候选/解析更晚返回: 守卫应直接丢弃, 不会再发起 A 的 resolve, B 行不被污染
    bindA.resolve({ rows: [bindingRow(2, 20, 'A路线', 'Y')] })
    await Promise.resolve(); await nextTick()
    expect(mockResolve).toHaveBeenCalledTimes(1)
    expect(vm.form.routeProductId).toBe(3)
    // A 的 resolve 即便外部意外返回也不得落状态
    resolveA.resolve({ data: { matched: true, routeProductId: 2, routeId: 20 } })
    await Promise.resolve(); await nextTick()
    expect(vm.form.routeProductId).toBe(3)
  })
})
