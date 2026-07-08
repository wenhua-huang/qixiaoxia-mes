import { describe, it, expect, vi } from 'vitest'

// Mock API
vi.mock('@/api/mes/wm/transaction', () => ({
  listWmTransaction: vi.fn(() => Promise.resolve({ rows: [], total: 0 })),
  getWmTransaction: vi.fn(() => Promise.resolve({
    data: {
      transactionId: 1,
      transactionType: 'ITEM_RECPT',
      sourceDocType: 'wm_item_recpt',
      sourceDocCode: 'IR-20260707-001',
      itemCode: 'MAT-001',
      itemName: '测试物料',
      specification: 'A4',
      unitName: '张',
      quantity: 100,
      unit2Name: '令',
      quantity2: 0.2,
      warehouseName: '原料仓',
      batchCode: 'B20260701',
      workorderCode: 'WO-001',
      createBy: 'admin',
      createTime: '2026-07-07 10:00:00',
      transactionTime: '2026-07-07 09:00:00',
    },
  })),
}))

// Mock useDict — dict values 与 TransactionTypeEnum + 实际枚举值对齐
const mockDictData = {
  mes_wm_transaction_type: { value: [
    { dictLabel: '物料入库',   dictValue: 'ITEM_RECPT',    listClass: 'success' },
    { dictLabel: '杂项入库',   dictValue: 'MISC_RECPT',    listClass: 'success' },
    { dictLabel: '杂项出库',   dictValue: 'MISC_ISSUE',    listClass: 'danger' },
    { dictLabel: '供应商退货', dictValue: 'ITEM_RTV',      listClass: 'danger' },
    { dictLabel: '销售出库',   dictValue: 'PRODUCT_SALES', listClass: 'danger' },
    { dictLabel: '销售退货',   dictValue: 'PRODUCT_RT',    listClass: 'success' },
    { dictLabel: '调拨出库',   dictValue: 'TRANS_OUT',     listClass: 'warning' },
    { dictLabel: '调拨入库',   dictValue: 'TRANS_IN',      listClass: 'warning' },
    { dictLabel: '分配',       dictValue: 'ALLOCATE',      listClass: 'info' },
    { dictLabel: '释放',       dictValue: 'RELEASE',       listClass: 'info' },
    { dictLabel: '领料出库',   dictValue: 'ISSUE_OUT',     listClass: 'danger' },
    { dictLabel: '退货入库',   dictValue: 'RETURN_IN',     listClass: 'success' },
  ]},
  mes_wm_source_doc_type: { value: [
    { dictLabel: '物料入库单', dictValue: 'wm_item_recpt', listClass: '' },
    { dictLabel: '领料出库单', dictValue: 'ISSUE',         listClass: '' },
    { dictLabel: '退料单',     dictValue: 'RTISSUE',       listClass: '' },
    { dictLabel: '调拨单',     dictValue: 'wm_transfer',   listClass: '' },
  ]},
}

vi.mock('@/utils/dict', () => ({
  useDict: vi.fn(() => mockDictData),
}))

// 与 TransactionTypeEnum 对齐的完整枚举值列表
const ALL_TX_TYPES = [
  'ITEM_RECPT', 'MISC_RECPT', 'MISC_ISSUE', 'ITEM_RTV',
  'PRODUCT_SALES', 'PRODUCT_RT', 'TRANS_OUT', 'TRANS_IN',
  'ALLOCATE', 'RELEASE', 'ISSUE_OUT', 'RETURN_IN',
]

describe('库存事务 — 字典映射完整性', () => {
  it('12 种事务类型全部有字典映射', () => {
    const txDict = mockDictData.mes_wm_transaction_type.value
    const values = txDict.map((d: any) => d.dictValue)
    for (const t of ALL_TX_TYPES) {
      expect(values).toContain(t)
    }
    expect(txDict.length).toBe(12)
  })

  it('入库类事务类型 tag 颜色为 success', () => {
    const inbound = ['ITEM_RECPT', 'MISC_RECPT', 'PRODUCT_RT', 'RETURN_IN']
    for (const entry of mockDictData.mes_wm_transaction_type.value) {
      if (inbound.includes(entry.dictValue)) {
        expect(entry.listClass).toBe('success')
      }
    }
  })

  it('出库类事务类型 tag 颜色为 danger', () => {
    const outbound = ['MISC_ISSUE', 'ITEM_RTV', 'PRODUCT_SALES', 'ISSUE_OUT']
    for (const entry of mockDictData.mes_wm_transaction_type.value) {
      if (outbound.includes(entry.dictValue)) {
        expect(entry.listClass).toBe('danger')
      }
    }
  })

  it('调拨类事务类型 tag 颜色为 warning', () => {
    for (const entry of mockDictData.mes_wm_transaction_type.value) {
      if (entry.dictValue === 'TRANS_OUT' || entry.dictValue === 'TRANS_IN') {
        expect(entry.listClass).toBe('warning')
      }
    }
  })

  it('分配/释放 tag 颜色为 info', () => {
    for (const entry of mockDictData.mes_wm_transaction_type.value) {
      if (entry.dictValue === 'ALLOCATE' || entry.dictValue === 'RELEASE') {
        expect(entry.listClass).toBe('info')
      }
    }
  })

  it('4 种来源单据类型全部有字典映射', () => {
    const sdtDict = mockDictData.mes_wm_source_doc_type.value
    const values = sdtDict.map((d: any) => d.dictValue)
    expect(values).toContain('wm_item_recpt')
    expect(values).toContain('ISSUE')
    expect(values).toContain('RTISSUE')
    expect(values).toContain('wm_transfer')
    expect(sdtDict.length).toBe(4)
  })
})

describe('变动数量 — 显示格式化', () => {
  const formatQuantity = (q: number) => {
    const sign = q > 0 ? '+' : ''
    const color = q > 0 ? '#67c23a' : q < 0 ? '#f56c6c' : '#909399'
    return { text: `${sign}${q}`, color }
  }

  it('正数 → 绿色，带 + 号', () => {
    expect(formatQuantity(100)).toEqual({ text: '+100', color: '#67c23a' })
  })

  it('负数 → 红色，不带 + 号', () => {
    expect(formatQuantity(-50)).toEqual({ text: '-50', color: '#f56c6c' })
  })

  it('零 → 灰色，不带符号', () => {
    expect(formatQuantity(0)).toEqual({ text: '0', color: '#909399' })
  })

  it('小数正确格式化', () => {
    expect(formatQuantity(3.5)).toEqual({ text: '+3.5', color: '#67c23a' })
  })
})

describe('API 数据流', () => {
  it('getWmTransaction 返回完整业务数据', async () => {
    const { getWmTransaction } = await import('@/api/mes/wm/transaction')
    const r = await getWmTransaction(1)
    const form = r.data!
    expect(form.transactionType).toBe('ITEM_RECPT')
    expect(form.sourceDocType).toBe('wm_item_recpt')
    expect(form.itemCode).toBe('MAT-001')
    expect(form.itemName).toBe('测试物料')
    expect(form.quantity).toBe(100)
    expect(form.warehouseName).toBe('原料仓')
  })

  it('返回数据包含操作人和操作时间', async () => {
    const { getWmTransaction } = await import('@/api/mes/wm/transaction')
    const r = await getWmTransaction(1)
    expect(r.data!.createBy).toBe('admin')
    expect(r.data!.createTime).toBeTruthy()
    expect(r.data!.transactionTime).toBeTruthy()
  })

  it('返回数据不包含 factoryId（详情弹窗已隐藏）', async () => {
    const { getWmTransaction } = await import('@/api/mes/wm/transaction')
    const r = await getWmTransaction(1)
    expect(r.data!.factoryId).toBeUndefined()
  })
})
