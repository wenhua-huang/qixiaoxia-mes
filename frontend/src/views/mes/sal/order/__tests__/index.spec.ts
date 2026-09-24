import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import SalOrder from '../index.vue'

// ==================== Mock APIs ====================
const mockListOrder = vi.fn()
const mockResolveBatch = vi.fn()
const mockListRoute = vi.fn()
const mockAcceptOrder = vi.fn()
const mockModal = { confirm: vi.fn(), msgSuccess: vi.fn(), msgError: vi.fn(), msgWarning: vi.fn() }
vi.mock('@/api/mes/sal/order', () => ({
  listOrder: (...args: any[]) => mockListOrder(...args),
  listAllOrder: vi.fn().mockResolvedValue({ code: 200, data: [] }),
  getOrder: vi.fn(),
  getOrderDetail: vi.fn().mockResolvedValue({ code: 200, data: { lines: [] } }),
  createOrderWithLines: vi.fn().mockResolvedValue({ code: 200 }),
  updateOrderWithLines: vi.fn().mockResolvedValue({ code: 200 }),
  closeOrder: vi.fn().mockResolvedValue({ code: 200 }),
  cancelOrder: vi.fn().mockResolvedValue({ code: 200 }),
  acceptOrder: (...args: any[]) => mockAcceptOrder(...args),
  toWorkorder: vi.fn().mockResolvedValue({ code: 200, data: { workorderCode: 'WO001' } }),
  delOrder: vi.fn().mockResolvedValue({ code: 200 }),
}))
vi.mock('@/api/system/dict/data', () => ({
  getDicts: vi.fn().mockResolvedValue({ data: [] }),
}))
vi.mock('@/api/system/user', () => ({
  listUser: vi.fn().mockResolvedValue({ rows: [] }),
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
// LineEdit 真实加载(组件虽 stub,模块仍被 import 解析)，切断 md/item、md/attr -> request->Navbar 链
vi.mock('@/api/mes/md/item', () => ({ getItem: vi.fn().mockResolvedValue({ data: {} }) }))
vi.mock('@/api/mes/md/attr', () => ({ getEffAttrSchema: vi.fn().mockResolvedValue({ data: [] }) }))
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

  it('生产中订单显示进度百分比且不渲染审核按钮', async () => {
    mockListOrder.mockResolvedValue({ rows: [
      { orderId: 1, orderCode: 'SO001', orderName: 'x', clientName: 'c', status: 'PRODUCING', progressPercent: 40 }
    ], total: 1 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick(); await nextTick()
    const bar = wrapper.findComponent({ name: 'ElProgress' })
    expect(bar.exists()).toBe(true)
    expect(bar.props('percentage')).toBe(40)  // 真实进度绑定，非文本巧合
    expect(bar.props('status')).toBe('')
    const html = wrapper.html()
    expect(html).toMatch(/\b40%/)
    expect(html).not.toContain('提交审核')
    expect(html).not.toContain('审核通过')
    expect(html).toContain('生成工单')        // PRODUCING 可追加转单
  })

  it('已结单订单进度条走 exception 态且不渲染任何流转操作', async () => {
    mockListOrder.mockResolvedValue({ rows: [
      { orderId: 3, orderCode: 'SO003', orderName: 'x', clientName: 'c', status: 'CLOSED', progressPercent: 100 }
    ], total: 1 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick(); await nextTick()
    expect(wrapper.findComponent({ name: 'ElProgress' }).props('status')).toBe('exception')
    const actionText = wrapper.findAll('tbody tr')[0]?.text() || ''
    expect(actionText).not.toContain('生成工单')
    expect(actionText).not.toContain('结单')
    expect(actionText).not.toContain('取消')
    expect(actionText).toContain('查看')
  })

  it('CONFIRMED 但已派生工单（未开工）时隐藏改/删除，仍可追加转单', async () => {
    mockListOrder.mockResolvedValue({ rows: [
      { orderId: 4, orderCode: 'SO004', orderName: 'x', clientName: 'c', status: 'CONFIRMED', progressPercent: 0, workorderCount: 1 }
    ], total: 1 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick(); await nextTick()
    const row = wrapper.findAll('tbody tr')[0]!
    const btnTexts = row.findAll('button').map(b => b.text().trim())
    expect(btnTexts).toContain('查看')
    expect(btnTexts).toContain('生成工单')
    expect(btnTexts).not.toContain('改')
    expect(btnTexts).not.toContain('结单')
    // 删除图标按钮是行内唯一无文本按钮：不应出现
    expect(btnTexts.filter(t => t === '')).toHaveLength(0)
  })

  it('待接单订单：有接单/改/删/取消，无生成工单/结单', async () => {
    mockListOrder.mockResolvedValue({ rows: [
      { orderId: 5, orderCode: 'SO005', orderName: 'x', clientName: 'c', status: 'PENDING_ACCEPT', progressPercent: 0 }
    ], total: 1 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick(); await nextTick()
    const btnTexts = wrapper.findAll('tbody tr')[0]!.findAll('button').map(b => b.text().trim())
    expect(btnTexts).toContain('查看')
    expect(btnTexts).toContain('接单')
    expect(btnTexts).toContain('改')
    expect(btnTexts).toContain('取消')
    expect(btnTexts).not.toContain('生成工单')
    expect(btnTexts).not.toContain('结单')
    // 删除图标按钮是行内唯一无文本按钮：应出现
    expect(btnTexts.filter(t => t === '')).toHaveLength(1)
  })

  it('点接单确认后调 acceptOrder 并刷新列表提示成功', async () => {
    mockListOrder.mockResolvedValue({ rows: [
      { orderId: 5, orderCode: 'SO005', orderName: 'x', clientName: 'c', status: 'PENDING_ACCEPT' }
    ], total: 1 })
    mockAcceptOrder.mockResolvedValue({ code: 200 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick(); await nextTick()
    const callsBefore = mockListOrder.mock.calls.length
    ;(wrapper.vm as any).handleAccept({ orderId: 5, orderCode: 'SO005' })
    await new Promise(resolve => setTimeout(resolve, 10))  // handleAccept 不返回 promise，用宏任务等链落完
    expect(mockAcceptOrder).toHaveBeenCalledTimes(1)
    expect(mockAcceptOrder).toHaveBeenCalledWith(5)
    expect(mockListOrder.mock.calls.length).toBe(callsBefore + 1)
    expect(mockModal.msgSuccess).toHaveBeenCalledWith('接单成功')
  })

  it('已出货订单显示结单按钮，不显示取消', async () => {
    mockListOrder.mockResolvedValue({ rows: [
      { orderId: 2, orderCode: 'SO002', orderName: 'x', clientName: 'c', status: 'SHIPPED', progressPercent: 100 }
    ], total: 1 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick(); await nextTick()
    expect(wrapper.html()).toContain('结单')
    expect(wrapper.html()).not.toContain('取消')
  })

  it('getList 请求带 includeProgress=true', async () => {
    mockListOrder.mockResolvedValue({ rows: [], total: 0 })
    mount(SalOrder, { global: globalStubs })
    await nextTick()
    expect(mockListOrder.mock.calls[0][0]).toMatchObject({ includeProgress: true })
  })

  it('状态/订单类型由字典驱动, 业务线纯映射', async () => {
    mockListOrder.mockResolvedValue({ rows: [], total: 0 })
    const wrapper = mount(SalOrder, { global: globalStubs })
    await nextTick()
    const vm: any = wrapper.vm
    vm.salStatusOptions = [
      { dictValue: 'PRODUCING', dictLabel: '生产中', listClass: 'warning' },
      { dictValue: 'CONFIRMED', dictLabel: '已确认', listClass: 'success' },
      { dictValue: 'CANCEL', dictLabel: '已取消', listClass: 'danger' },
    ]
    expect(vm.statusMeta('PRODUCING')).toMatchObject({ text: '生产中', type: 'warning' })
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
