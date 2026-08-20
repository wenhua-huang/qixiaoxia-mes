/**
 * 采购模块纯函数 — 可在 uni-app 和测试环境中使用
 */

// 采购类型映射
const purchaseTypeMap = { PAPER: '纸张', AUX: '辅料', PACK: '包材', OTHER: '其他' }
export function purchaseTypeText(type) {
  return purchaseTypeMap[type] || type
}

// 订单状态 → uni-tag type 映射
export function orderStatusTagType(status) {
  const map = { DRAFT: 'default', APPROVED: 'primary', ORDERED: 'warning', RECEIVING: 'warning', RECEIVED: 'success', CLOSED: 'info', CANCEL: 'danger' }
  return map[status] || 'default'
}

// 订单状态 → 中文文本
export function orderStatusText(status) {
  const map = { DRAFT: '草稿', APPROVED: '已审批', ORDERED: '已下单', RECEIVING: '收货中', RECEIVED: '已收货', CLOSED: '已关闭', CANCEL: '已取消' }
  return map[status] || status
}

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

// IQC 检验状态（列表 qcStatus 计算列）→ uni-tag type
export function qcStatusTagType(status) {
  const map = { PENDING: 'warning', INSPECTING: 'warning', PASSED: 'success', CONCESSION: 'warning', FAILED: 'danger', NONE: 'default' }
  return map[status] || 'default'
}

// IQC 检验状态 → 中文文本
export function qcStatusText(status) {
  const map = { PENDING: '待检', INSPECTING: '检验中', PASSED: '合格', CONCESSION: '让步接收', FAILED: '不合格', NONE: '免检' }
  return map[status] || '免检'
}

// 校验实收数量是否有效
export function isValidReceiptQty(qty) {
  const num = parseFloat(qty)
  return !isNaN(num) && num > 0
}

// 校验订单状态是否允许收货
export function canReceive(status) {
  return status === 'ORDERED' || status === 'RECEIVING'
}

// 生成收货编码
export function genRecptCode() {
  return 'RCP-' + Date.now().toString(36).toUpperCase()
}
