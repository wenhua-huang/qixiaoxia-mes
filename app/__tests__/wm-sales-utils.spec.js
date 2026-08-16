import { describe, it, expect } from 'vitest'
import {
  salesStatusText, salesStatusTagType, shipStatusText, canPost, lineRemain
} from '../utils/wm-sales.js'

// ============ 出库单状态 → 中文文本 ============
describe('salesStatusText', () => {
  it('DRAFT → 草稿', () => { expect(salesStatusText('DRAFT')).toBe('草稿') })
  it('PARTIAL_POSTED → 部分出库', () => { expect(salesStatusText('PARTIAL_POSTED')).toBe('部分出库') })
  it('POSTED → 已出库', () => { expect(salesStatusText('POSTED')).toBe('已出库') })
  it('SHIPPED → 已发运', () => { expect(salesStatusText('SHIPPED')).toBe('已发运') })
  it('CLOSED → 已关闭', () => { expect(salesStatusText('CLOSED')).toBe('已关闭') })
  it('CANCELED → 已作废', () => { expect(salesStatusText('CANCELED')).toBe('已作废') })
  it('未知状态返回原值', () => { expect(salesStatusText('XX')).toBe('XX') })
})

// ============ 出库单状态 → uni-tag type ============
describe('salesStatusTagType', () => {
  it('DRAFT → default', () => { expect(salesStatusTagType('DRAFT')).toBe('default') })
  it('PARTIAL_POSTED → warning', () => { expect(salesStatusTagType('PARTIAL_POSTED')).toBe('warning') })
  it('POSTED → primary', () => { expect(salesStatusTagType('POSTED')).toBe('primary') })
  it('SHIPPED → success', () => { expect(salesStatusTagType('SHIPPED')).toBe('success') })
  it('CANCELED → danger', () => { expect(salesStatusTagType('CANCELED')).toBe('danger') })
  it('未知状态返回 default', () => { expect(salesStatusTagType('XX')).toBe('default') })
})

// ============ 发运状态 → 中文文本 ============
describe('shipStatusText', () => {
  it('UN_SHIPPED → 未发运', () => { expect(shipStatusText('UN_SHIPPED')).toBe('未发运') })
  it('PARTIAL_SHIPPED → 部分发运', () => { expect(shipStatusText('PARTIAL_SHIPPED')).toBe('部分发运') })
  it('SHIPPED → 已发运', () => { expect(shipStatusText('SHIPPED')).toBe('已发运') })
  it('RECEIVED → 已签收', () => { expect(shipStatusText('RECEIVED')).toBe('已签收') })
  it('空值返回 -', () => { expect(shipStatusText(null)).toBe('-') })
})

// ============ 可出库状态判断（与后端 POSTABLE_STATUSES 一致） ============
describe('canPost', () => {
  it('DRAFT 可出库', () => { expect(canPost('DRAFT')).toBe(true) })
  it('PARTIAL_POSTED 可出库', () => { expect(canPost('PARTIAL_POSTED')).toBe(true) })
  it('POSTED 不可出库', () => { expect(canPost('POSTED')).toBe(false) })
  it('SHIPPED 不可出库', () => { expect(canPost('SHIPPED')).toBe(false) })
  it('CLOSED 不可出库', () => { expect(canPost('CLOSED')).toBe(false) })
})

// ============ 出库行未出量计算 ============
describe('lineRemain', () => {
  it('正常计算：67-8=59', () => {
    expect(lineRemain({ quantitySales: 67, quantityPosted: 8 })).toBe(59)
  })
  it('全部出完返回 0', () => {
    expect(lineRemain({ quantitySales: 10, quantityPosted: 10 })).toBe(0)
  })
  it('posted 为空按 0 算', () => {
    expect(lineRemain({ quantitySales: 5 })).toBe(5)
  })
  it('字段都为空返回 0', () => {
    expect(lineRemain({})).toBe(0)
  })
  it('字符串数字可计算', () => {
    expect(lineRemain({ quantitySales: '10', quantityPosted: '3' })).toBe(7)
  })
})
