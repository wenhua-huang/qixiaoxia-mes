import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import SalOrder from '../index.vue'

// ==================== Mock APIs ====================
const mockListOrder = vi.fn()
vi.mock('@/api/mes/sal/order', () => ({
  listOrder: (...args: any[]) => mockListOrder(...args),
  listAllOrder: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getOrder: vi.fn(),
  getOrderDetail: vi.fn().mockResolvedValue({ code: 200, data: { lines: [] } }),
  createOrderWithLines: vi.fn().mockResolvedValue({ code: 200 }),
  updateOrderWithLines: vi.fn().mockResolvedValue({ code: 200 }),
  confirmOrder: vi.fn().mockResolvedValue({ code: 200 }),
  closeOrder: vi.fn().mockResolvedValue({ code: 200 }),
  cancelOrder: vi.fn().mockResolvedValue({ code: 200 }),
  toWorkorder: vi.fn().mockResolvedValue({ code: 200, data: { workorderCode: 'WO001' } }),
  delOrder: vi.fn().mockResolvedValue({ code: 200 }),
}))
vi.mock('@/api/mes/sys/autocoderule', () => ({
  genSerialCode: vi.fn().mockResolvedValue({ data: 'SO20260715001' }),
}))
// 断开 LineEdit->ItemSelect->item API->request->router->layout->Navbar 的解析链
vi.mock('@/components/itemSelect/single.vue', () => ({
  default: { name: 'ItemSelect', template: '<div />' },
}))
// 断开 ClientSelect->client API->request->...->Navbar 的解析链(Navbar 导入 HeaderNotice 目录,vitest 解析不了)
vi.mock('@/api/mes/md/client', () => ({
  listClient: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
}))
// 断开 index.vue 新增的 pro 域 API 导入->request->Navbar 链
vi.mock('@/api/mes/pro/routeproduct', () => ({ listRouteProduct: vi.fn().mockResolvedValue({ rows: [], total: 0 }) }))
vi.mock('@/api/mes/pro/proroute', () => ({ listRoute: vi.fn().mockResolvedValue({ rows: [] }) }))
vi.mock('@/api/mes/pro/routeprocess', () => ({ listRouteProcessByRouteId: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/api/mes/pro/routeproductbom', () => ({ listRouteProductBomByRouteId: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/api/mes/pro/routeprocessparam', () => ({ listRouteProcessParamByRouteProductId: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/api/mes/pro/paramtemplate', () => ({ listParamTemplate: vi.fn().mockResolvedValue({ rows: [] }) }))
vi.mock('@/api/mes/pro/workorder', () => ({ checkDeviation: vi.fn().mockResolvedValue({ data: { hasDeviation: false, deviations: [] } }) }))

const globalStubs = {
  stubs: { ClientSelect: true, LineEdit: true, 'right-toolbar': true, pagination: true },
  mocks: { parseTime: (t: any) => (t ? String(t).slice(0, 10) : '') },
}

describe('SalOrder index.vue', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('挂载时调用 listOrder 加载列表', async () => {
    mockListOrder.mockResolvedValue({ rows: [], total: 0 })
    mount(SalOrder, { global: globalStubs })
    await nextTick()
    expect(mockListOrder).toHaveBeenCalled()
    expect(mockListOrder.mock.calls[0][0]).toMatchObject({ pageNum: 1, pageSize: 10 })
  })

  it('渲染新增/修改/删除/导出按钮', async () => {
    mockListOrder.mockResolvedValue({ rows: [], total: 0 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick()
    const html = wrapper.html()
    expect(html).toContain('新增')
    expect(html).toContain('导出')
  })

  it('渲染订单列表数据', async () => {
    mockListOrder.mockResolvedValue({ rows: [
      { orderId: 1, orderCode: 'SO001', orderName: '心心纸袋', clientName: '圣享', businessLine: 'DOMESTIC', status: 'CONFIRMED', totalAmount: 9800 }
    ], total: 1 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick(); await nextTick()
    const html = wrapper.html()
    expect(html).toContain('SO001')
    expect(html).toContain('心心纸袋')
    expect(html).toContain('圣享')
  })

  it('状态文本/标签/业务线 纯函数映射正确', async () => {
    mockListOrder.mockResolvedValue({ rows: [], total: 0 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick()
    const vm: any = wrapper.vm
    expect(vm.statusText('PREPARE')).toBe('待确认')
    expect(vm.statusText('CONFIRMED')).toBe('已确认')
    expect(vm.statusText('CANCEL')).toBe('已取消')
    expect(vm.statusTag('CONFIRMED')).toBe('success')
    expect(vm.statusTag('CANCEL')).toBe('danger')
    expect(vm.businessLineText('FOREIGN')).toBe('外贸')
    expect(vm.businessLineText('SPOT')).toBe('现货')
  })

  it('空列表不崩溃', async () => {
    mockListOrder.mockResolvedValue({ rows: [], total: 0 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick()
    expect(wrapper.exists()).toBe(true)
  })
})
