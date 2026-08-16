/**
 * 销售出库模块纯函数 — 可在 uni-app 和测试环境中使用
 */

// 出库单状态 → uni-tag type 映射
export function salesStatusTagType(status) {
  const map = { DRAFT: 'default', PARTIAL_POSTED: 'warning', POSTED: 'primary', SHIPPED: 'success', CLOSED: 'info', CANCELED: 'danger' }
  return map[status] || 'default'
}

// 出库单状态 → 中文文本（与字典 mes_wm_sales_status 保持一致）
export function salesStatusText(status) {
  const map = { DRAFT: '草稿', PARTIAL_POSTED: '部分出库', POSTED: '已出库', SHIPPED: '已发运', CLOSED: '已关闭', CANCELED: '已作废' }
  return map[status] || status
}

// 发运状态 → 中文文本
export function shipStatusText(shipStatus) {
  const map = { UN_SHIPPED: '未发运', PARTIAL_SHIPPED: '部分发运', SHIPPED: '已发运', RECEIVED: '已签收' }
  return map[shipStatus] || shipStatus || '-'
}

// 校验订单状态是否允许出库（与后端 POSTABLE_STATUSES 一致）
export function canPost(status) {
  return status === 'DRAFT' || status === 'PARTIAL_POSTED'
}

// 计算出库行未出量
export function lineRemain(line) {
  return Math.max(0, Number(line.quantitySales || 0) - Number(line.quantityPosted || 0))
}
