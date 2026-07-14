/**
 * 产品入库模块纯函数 — 可在 uni-app 和测试环境中使用
 */

// 入库单状态 → uni-tag type 映射
export function recptStatusTagType(status) {
  const map = { DRAFT: 'default', CONFIRMED: 'primary', POSTED: 'success', CANCEL: 'danger' }
  return map[status] || 'default'
}

// 入库单状态 → 中文文本
export function recptStatusText(status) {
  const map = { DRAFT: '草稿', CONFIRMED: '已确认', POSTED: '已过账', CANCEL: '已取消' }
  return map[status] || status
}

// 校验实收数量是否有效
export function isValidRecptQty(qty) {
  const num = parseFloat(qty)
  return !isNaN(num) && num > 0
}
