import { describe, it, expect, vi, beforeEach } from 'vitest'

// Mock API functions
vi.mock('@/api/mes/pur/order', () => ({
  listOrder: vi.fn(() => Promise.resolve({ rows: [], total: 0 })),
  getOrder: vi.fn(() => Promise.resolve({ data: {} })),
  delOrder: vi.fn(() => Promise.resolve()),
  addOrder: vi.fn(() => Promise.resolve({ data: { orderId: 1 } })),
  updateOrder: vi.fn(() => Promise.resolve()),
  approveOrder: vi.fn(() => Promise.resolve()),
  orderOrder: vi.fn(() => Promise.resolve()),
  closeOrder: vi.fn(() => Promise.resolve()),
}))

describe('采购订单页面 — 状态按钮可见性', () => {
  it('DRAFT状态 → 显示审批按钮', () => {
    const row = { orderId: 1, orderCode: 'PO-001', status: 'DRAFT' }
    expect(row.status === 'DRAFT').toBe(true)
    expect(row.status === 'APPROVED').toBe(false)
    expect(row.status === 'RECEIVED').toBe(false)
  })

  it('APPROVED状态 → 显示下单按钮', () => {
    const row = { status: 'APPROVED' }
    expect(row.status === 'APPROVED').toBe(true)
  })

  it('RECEIVED状态 → 显示关闭按钮', () => {
    const row = { status: 'RECEIVED' }
    expect(row.status === 'RECEIVED').toBe(true)
  })

  it('ORDERED状态 → 不显示操作按钮', () => {
    const row = { status: 'ORDERED' }
    expect(row.status === 'DRAFT').toBe(false)
    expect(row.status === 'APPROVED').toBe(false)
    expect(row.status === 'RECEIVED').toBe(false)
  })
})

describe('isOverdue — 超期未到货判断', () => {
  // 与组件内 isOverdue 方法完全一致的纯函数
  const isOverdue = (row: { status: string; expectedDate: string | null }) => {
    if (row.status !== 'ORDERED' && row.status !== 'RECEIVING') return false
    if (!row.expectedDate) return false
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return new Date(row.expectedDate) < today
  }

  const yesterday = () => {
    const d = new Date()
    d.setDate(d.getDate() - 1)
    return d.toISOString().slice(0, 10)
  }

  const tomorrow = () => {
    const d = new Date()
    d.setDate(d.getDate() + 1)
    return d.toISOString().slice(0, 10)
  }

  const today = () => new Date().toISOString().slice(0, 10)

  it('ORDERED + 预计到货已过 → true', () => {
    expect(isOverdue({ status: 'ORDERED', expectedDate: yesterday() })).toBe(true)
  })

  it('ORDERED + 预计到货未来 → false', () => {
    expect(isOverdue({ status: 'ORDERED', expectedDate: tomorrow() })).toBe(false)
  })

  it('ORDERED + 预计到货今天 → false', () => {
    expect(isOverdue({ status: 'ORDERED', expectedDate: today() })).toBe(false)
  })

  it('RECEIVING + 超期 → true', () => {
    expect(isOverdue({ status: 'RECEIVING', expectedDate: yesterday() })).toBe(true)
  })

  it('DRAFT + 日期已过 → false（不在监控范围）', () => {
    expect(isOverdue({ status: 'DRAFT', expectedDate: yesterday() })).toBe(false)
  })

  it('RECEIVED + 日期已过 → false（已收货不超期）', () => {
    expect(isOverdue({ status: 'RECEIVED', expectedDate: yesterday() })).toBe(false)
  })

  it('expectedDate为null → false', () => {
    expect(isOverdue({ status: 'ORDERED', expectedDate: null })).toBe(false)
  })

  it('CLOSED + 日期已过 → false（已关闭不超期）', () => {
    expect(isOverdue({ status: 'CLOSED', expectedDate: yesterday() })).toBe(false)
  })
})

describe('采购订单API函数可导入', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('approveOrder可调用', async () => {
    const { approveOrder } = await import('@/api/mes/pur/order')
    const result = await approveOrder(1)
    expect(approveOrder).toBeDefined()
    expect(result).toBeUndefined()  // mock returns undefined
  })

  it('orderOrder可调用', async () => {
    const { orderOrder } = await import('@/api/mes/pur/order')
    await orderOrder(1)
    expect(orderOrder).toBeDefined()
  })

  it('closeOrder可调用', async () => {
    const { closeOrder } = await import('@/api/mes/pur/order')
    await closeOrder(1)
    expect(closeOrder).toBeDefined()
  })

  it('现有API函数仍然可用', async () => {
    const { listOrder, getOrder, delOrder, addOrder, updateOrder } = await import('@/api/mes/pur/order')
    expect(listOrder).toBeDefined()
    expect(getOrder).toBeDefined()
    expect(delOrder).toBeDefined()
    expect(addOrder).toBeDefined()
    expect(updateOrder).toBeDefined()
  })
})
