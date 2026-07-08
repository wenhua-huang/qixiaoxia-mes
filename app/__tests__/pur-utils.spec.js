import { describe, it, expect } from 'vitest'
import {
  purchaseTypeText, orderStatusTagType, orderStatusText,
  recptStatusTagType, recptStatusText,
  isValidReceiptQty, canReceive, genRecptCode
} from '../utils/pur.js'

// ============ 采购类型映射 ============
describe('purchaseTypeText', () => {
  it('PAPER → 纸张', () => { expect(purchaseTypeText('PAPER')).toBe('纸张') })
  it('AUX → 辅料', () => { expect(purchaseTypeText('AUX')).toBe('辅料') })
  it('PACK → 包材', () => { expect(purchaseTypeText('PACK')).toBe('包材') })
  it('OTHER → 其他', () => { expect(purchaseTypeText('OTHER')).toBe('其他') })
  it('未知类型返回原值', () => { expect(purchaseTypeText('UNKNOWN')).toBe('UNKNOWN') })
  it('null 返回 null', () => { expect(purchaseTypeText(null)).toBe(null) })
})

// ============ 订单状态 → tag type ============
describe('orderStatusTagType', () => {
  it('DRAFT → default', () => { expect(orderStatusTagType('DRAFT')).toBe('default') })
  it('APPROVED → primary', () => { expect(orderStatusTagType('APPROVED')).toBe('primary') })
  it('ORDERED → warning', () => { expect(orderStatusTagType('ORDERED')).toBe('warning') })
  it('RECEIVING → warning', () => { expect(orderStatusTagType('RECEIVING')).toBe('warning') })
  it('RECEIVED → success', () => { expect(orderStatusTagType('RECEIVED')).toBe('success') })
  it('CLOSED → info', () => { expect(orderStatusTagType('CLOSED')).toBe('info') })
  it('CANCEL → danger', () => { expect(orderStatusTagType('CANCEL')).toBe('danger') })
  it('未知状态 → default', () => { expect(orderStatusTagType('X')).toBe('default') })
})

// ============ 订单状态 → 中文 ============
describe('orderStatusText', () => {
  it('DRAFT → 草稿', () => { expect(orderStatusText('DRAFT')).toBe('草稿') })
  it('APPROVED → 已审批', () => { expect(orderStatusText('APPROVED')).toBe('已审批') })
  it('ORDERED → 已下单', () => { expect(orderStatusText('ORDERED')).toBe('已下单') })
  it('RECEIVING → 收货中', () => { expect(orderStatusText('RECEIVING')).toBe('收货中') })
  it('RECEIVED → 已收货', () => { expect(orderStatusText('RECEIVED')).toBe('已收货') })
  it('CLOSED → 已关闭', () => { expect(orderStatusText('CLOSED')).toBe('已关闭') })
  it('CANCEL → 已取消', () => { expect(orderStatusText('CANCEL')).toBe('已取消') })
  it('未知状态返回原值', () => { expect(orderStatusText('XYZ')).toBe('XYZ') })
})

// ============ 入库单状态 → tag type ============
describe('recptStatusTagType', () => {
  it('DRAFT → default', () => { expect(recptStatusTagType('DRAFT')).toBe('default') })
  it('CONFIRMED → primary', () => { expect(recptStatusTagType('CONFIRMED')).toBe('primary') })
  it('POSTED → success', () => { expect(recptStatusTagType('POSTED')).toBe('success') })
  it('CANCEL → danger', () => { expect(recptStatusTagType('CANCEL')).toBe('danger') })
  it('未知 → default', () => { expect(recptStatusTagType('?')).toBe('default') })
})

// ============ 入库单状态 → 中文 ============
describe('recptStatusText', () => {
  it('DRAFT → 草稿', () => { expect(recptStatusText('DRAFT')).toBe('草稿') })
  it('CONFIRMED → 已确认', () => { expect(recptStatusText('CONFIRMED')).toBe('已确认') })
  it('POSTED → 已过账', () => { expect(recptStatusText('POSTED')).toBe('已过账') })
  it('CANCEL → 已取消', () => { expect(recptStatusText('CANCEL')).toBe('已取消') })
})

// ============ 实收数量校验 ============
describe('isValidReceiptQty', () => {
  it('正数有效: 100 → true', () => { expect(isValidReceiptQty(100)).toBe(true) })
  it('正数有效: "10.5" → true', () => { expect(isValidReceiptQty('10.5')).toBe(true) })
  it('正数有效: 0.001 → true', () => { expect(isValidReceiptQty(0.001)).toBe(true) })
  it('零无效: 0 → false', () => { expect(isValidReceiptQty(0)).toBe(false) })
  it('零无效: "0" → false', () => { expect(isValidReceiptQty('0')).toBe(false) })
  it('负数无效: -1 → false', () => { expect(isValidReceiptQty(-1)).toBe(false) })
  it('空字符串无效: "" → false', () => { expect(isValidReceiptQty('')).toBe(false) })
  it('字符串无效: "abc" → false', () => { expect(isValidReceiptQty('abc')).toBe(false) })
  it('null无效 → false', () => { expect(isValidReceiptQty(null)).toBe(false) })
  it('undefined无效 → false', () => { expect(isValidReceiptQty(undefined)).toBe(false) })
  it('NaN无效 → false', () => { expect(isValidReceiptQty(NaN)).toBe(false) })
})

// ============ 订单状态是否允许收货 ============
describe('canReceive', () => {
  it('ORDERED → true', () => { expect(canReceive('ORDERED')).toBe(true) })
  it('RECEIVING → true', () => { expect(canReceive('RECEIVING')).toBe(true) })
  it('DRAFT → false', () => { expect(canReceive('DRAFT')).toBe(false) })
  it('APPROVED → false', () => { expect(canReceive('APPROVED')).toBe(false) })
  it('RECEIVED → false', () => { expect(canReceive('RECEIVED')).toBe(false) })
  it('CLOSED → false', () => { expect(canReceive('CLOSED')).toBe(false) })
  it('CANCEL → false', () => { expect(canReceive('CANCEL')).toBe(false) })
  it('空字符串 → false', () => { expect(canReceive('')).toBe(false) })
  it('null → false', () => { expect(canReceive(null)).toBe(false) })
})

// ============ 生成收货编码 ============
describe('genRecptCode', () => {
  it('以 RCP- 开头', () => {
    const code = genRecptCode()
    expect(code.startsWith('RCP-')).toBe(true)
  })
  it('长度 > 4', () => {
    const code = genRecptCode()
    expect(code.length).toBeGreaterThan(4)
  })
  it('两次调用生成不同编码（有时间差）', async () => {
    const c1 = genRecptCode()
    await new Promise(r => setTimeout(r, 5))
    const c2 = genRecptCode()
    expect(c1).not.toBe(c2)
  })
})
