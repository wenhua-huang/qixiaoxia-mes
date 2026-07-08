import request from '@/utils/request'

// 领料单列表（支持按状态过滤）
export function listIssue(query) {
  return request({ url: '/mes/wm/issueheader/list', method: 'get', params: query })
}

// 领料单详情（头 + 行）
export function getIssueDetail(issueId) {
  return request({ url: '/mes/wm/issueheader/detail/' + issueId, method: 'get' })
}

// 领料单明细行
export function listIssueLine(issueId) {
  return request({ url: '/mes/wm/issueline/list', method: 'get', params: { issueId } })
}

// 发料出库（分批）— details 为发料明细数组
export function issueOut(issueId, details) {
  return request({ url: '/mes/wm/issueheader/issueOut/' + issueId, method: 'put', data: details })
}

// 预占库存（APPROVED → ALLOCATED）
export function confirmIssue(issueId) {
  return request({ url: '/mes/wm/issueheader/confirm/' + issueId, method: 'put' })
}

// 关闭领料单（ISSUED → CLOSED）
export function closeIssue(issueId) {
  return request({ url: '/mes/wm/issueheader/close/' + issueId, method: 'put' })
}

// 物料库存查询（扫码后根据 itemCode 查可用库存）
export function getItemStock(itemCode, warehouseId) {
  return request({ url: '/mes/wm/materialstock/list', method: 'get', params: { itemCode, warehouseId } })
}
