import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ItemVendorList from '../ItemVendorList.vue'

// ==================== Mock APIs ====================
const mockListByVendorId = vi.fn()
vi.mock('@/api/mes/md/itemvendor', () => ({
  listByVendorId: (...args: any[]) => mockListByVendorId(...args),
  addItemVendor: vi.fn().mockResolvedValue({ code: 200 }),
  updateItemVendor: vi.fn().mockResolvedValue({ code: 200 }),
  delItemVendor: vi.fn().mockResolvedValue({ code: 200 }),
}))
vi.mock('@/api/mes/md/item', () => ({
  listItem: vi.fn().mockResolvedValue({ rows: [], total: 0 }),
}))

describe('ItemVendorList.vue', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('应在挂载时加载供应物料列表', async () => {
    mockListByVendorId.mockResolvedValue({ code: 200, data: [
      { recordId: 1, itemId: 10, itemCode: 'MAT-001', itemName: '白牛皮', vendorId: 200, vendorName: '圣享纸业', isPreferred: 'Y', minOrderQty: 100, leadTimeDays: 7 }
    ]})

    const wrapper = mount(ItemVendorList, { props: { vendorId: 200 } })
    await nextTick(); await nextTick()

    expect(mockListByVendorId).toHaveBeenCalledWith(200)
    expect(wrapper.html()).toContain('MAT-001')
    expect(wrapper.html()).toContain('白牛皮')
  })

  it('应显示添加物料按钮', async () => {
    mockListByVendorId.mockResolvedValue({ code: 200, data: [] })
    const wrapper = mount(ItemVendorList, { props: { vendorId: 200 } })
    await nextTick()
    expect(wrapper.html()).toContain('添加物料')
  })

  it('切换首选应调用 updateItemVendor', async () => {
    mockListByVendorId.mockResolvedValue({ code: 200, data: [
      { recordId: 1, itemId: 10, itemCode: 'MAT-001', itemName: '白牛皮', vendorId: 200, isPreferred: 'N', minOrderQty: 100, leadTimeDays: 7 }
    ]})
    const wrapper = mount(ItemVendorList, { props: { vendorId: 200 } })
    await nextTick(); await nextTick()
    expect(wrapper.html()).toContain('MAT-001')
  })
})
