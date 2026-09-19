import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import SalOrder from '../index.vue'

// ==================== Mock APIs ====================
const mockListOrder = vi.fn()
const mockResolveBatch = vi.fn()
const mockListRoute = vi.fn()
const mockModal = { confirm: vi.fn(), msgSuccess: vi.fn(), msgError: vi.fn(), msgWarning: vi.fn() }
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
vi.mock('@/api/mes/pro/routeproduct', () => ({
  listRouteProduct: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
  resolveRouteProductBatch: (...args: any[]) => mockResolveBatch(...args),
}))
vi.mock('@/api/mes/pro/proroute', () => ({ listRoute: (...args: any[]) => mockListRoute(...args) }))
vi.mock('@/api/mes/pro/routeprocess', () => ({ listRouteProcessByRouteId: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/api/mes/pro/routeproductbom', () => ({ listRouteProductBomByRouteId: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/api/mes/pro/routeprocessparam', () => ({ listRouteProcessParamByRouteProductId: vi.fn().mockResolvedValue({ data: [] }) }))
vi.mock('@/api/mes/pro/paramtemplate', () => ({ listParamTemplate: vi.fn().mockResolvedValue({ rows: [] }) }))
vi.mock('@/api/mes/pro/workorder', () => ({ checkDeviation: vi.fn().mockResolvedValue({ data: { hasDeviation: false, deviations: [] } }) }))

const globalStubs = {
  stubs: { ClientSelect: true, LineEdit: true, ToWorkorderDialog: true, 'right-toolbar': true, pagination: true },
  mocks: { parseTime: (t: any) => (t ? String(t).slice(0, 10) : ''), $modal: mockModal },
}

describe('SalOrder index.vue', () => {
  beforeEach(() => { vi.clearAllMocks(); mockModal.confirm.mockResolvedValue(true) })

  /** 挂载并取出 vm; listOrder 默认空列表 */
  async function mountPage(listRows: any[] = []) {
    mockListOrder.mockResolvedValue({ rows: listRows, total: listRows.length })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick(); await nextTick()
    return wrapper
  }

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

  it('状态/订单类型由字典驱动, 业务线纯映射', async () => {
    mockListOrder.mockResolvedValue({ rows: [], total: 0 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick()
    const vm: any = wrapper.vm
    vm.salStatusOptions = [
      { dictValue: 'PREPARE', dictLabel: '待提交', listClass: 'info' },
      { dictValue: 'CONFIRMED', dictLabel: '已确认', listClass: 'success' },
      { dictValue: 'CANCEL', dictLabel: '已取消', listClass: 'danger' },
    ]
    expect(vm.statusMeta('PREPARE')).toMatchObject({ text: '待提交', type: 'info' })
    expect(vm.statusMeta('CONFIRMED')).toMatchObject({ text: '已确认', type: 'success' })
    expect(vm.statusMeta('CANCEL')).toMatchObject({ text: '已取消', type: 'danger' })
    vm.salOrderTypeOptions = [
      { dictValue: 'STANDARD', dictLabel: '标品', listClass: 'primary' },
      { dictValue: 'PLATE', dictLabel: '制版', listClass: 'success' },
    ]
    expect(vm.orderTypeMeta('PLATE')).toMatchObject({ text: '制版', type: 'success' })
    expect(vm.businessLineText('FOREIGN')).toBe('外贸')
    expect(vm.businessLineText('SPOT')).toBe('现货')
  })

  it('空列表不崩溃', async () => {
    const wrapper = await mountPage()
    expect(wrapper.exists()).toBe(true)
  })

  it('头维度变更确认后: 按新维度批量重算并覆盖命中行的路线', async () => {
    const wrapper = await mountPage()
    const vm: any = wrapper.vm
    vm.form.orderType = 'PLATE'; vm.form.outsourceFlag = 'N'; vm.form.packageFlag = 'N'
    vm.lineList = [{ lineId: 1, productId: 227, productName: '产品A', routeProductId: 1, routeName: '旧路线' }]
    mockResolveBatch.mockResolvedValue({ data: { 227: { matched: true, routeProductId: 404, routeId: 51 } } })
    mockListRoute.mockResolvedValue({ rows: [{ routeId: 51, routeCode: 'RT-PLATE', routeName: '制版路线' }] })

    await vm.onHeadDimensionChange()

    expect(mockResolveBatch).toHaveBeenCalledTimes(1)
    expect(mockResolveBatch.mock.calls[0][0]).toMatchObject({ itemIds: [227], orderType: 'PLATE', outsourceFlag: 'N', packageFlag: 'N' })
    expect(vm.lineList[0].routeProductId).toBe(404)
    expect(vm.lineList[0].routeName).toBe('制版路线')
    expect(mockModal.msgSuccess).toHaveBeenCalled()
  })

  it('头维度变更取消确认: 不调重算接口, 行路线保持不变', async () => {
    const wrapper = await mountPage()
    const vm: any = wrapper.vm
    vm.lineList = [{ lineId: 1, productId: 227, productName: '产品A', routeName: '旧路线' }]
    mockModal.confirm.mockRejectedValue(new Error('cancel'))

    await vm.onHeadDimensionChange()

    expect(mockResolveBatch).not.toHaveBeenCalled()
    expect(vm.lineList[0].routeName).toBe('旧路线')
  })

  it('批量重算遇到外发硬阻断: 提示阻断产品且不覆盖该行', async () => {
    const wrapper = await mountPage()
    const vm: any = wrapper.vm
    vm.lineList = [{ lineId: 1, productId: 227, productName: '产品A', routeProductId: 1, routeName: '旧路线' }]
    mockResolveBatch.mockResolvedValue({ data: { 227: { hardBlocked: true, message: '无外发路线' } } })
    mockListRoute.mockResolvedValue({ rows: [] })

    await vm.onHeadDimensionChange()

    expect(vm.lineList[0].routeProductId).toBe(1)
    expect(mockModal.msgError.mock.calls[0][0]).toContain('产品A')
  })

  it('连续切换头维度: 先返回的过期响应被丢弃, 不污染明细行', async () => {
    const wrapper = await mountPage()
    const vm: any = wrapper.vm
    vm.form.orderType = 'STANDARD'
    vm.lineList = [{ lineId: 1, productId: 227, productName: '产品A' }]
    let resolveFirst!: (v: any) => void
    const first = new Promise(r => { resolveFirst = r })
    let resolveSecond!: (v: any) => void
    const second = new Promise(r => { resolveSecond = r })
    mockResolveBatch.mockReturnValueOnce(first).mockReturnValueOnce(second)
    mockListRoute.mockResolvedValue({ rows: [] })

    vm.onHeadDimensionChange()
    await nextTick()
    vm.onHeadDimensionChange()
    await nextTick()
    resolveSecond({ data: { 227: { matched: true, routeProductId: 405, routeId: 52 } } })
    await Promise.resolve(); await nextTick()
    resolveFirst({ data: { 227: { matched: true, routeProductId: 404, routeId: 51 } } })
    await Promise.resolve(); await nextTick()

    expect(mockResolveBatch).toHaveBeenCalledTimes(2)
    expect(vm.lineList[0].routeProductId).toBe(405)
  })
})
