import request from '@/utils/request'

// 外协单列表（厂商账号后端自动按 vendorId 过滤）
export function listOutsource(query) {
  return request({ url: '/mes/wm/outsource/list', method: 'get', params: query })
}

// 外协单详情（含发料行+收货行）
export function getOutsource(orderId) {
  return request({ url: '/mes/wm/outsource/' + orderId, method: 'get' })
}

// 创建外协发料单（App 手工发料：直接扣料并置为 ISSUED）
export function createOutsource(data) {
  return request({ url: '/mes/wm/outsource/create', method: 'post', data: data })
}

// 厂商签收（ISSUED → VENDOR_RCVD，厂商操作，无请求体）
export function vendorReceiveOutsource(orderId) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/vendorReceive', method: 'post' })
}

// 厂商录加工结果（请求体是数组，后端自动继承发料物料；支持分批补录；录满自动完成）
export function recordOutsourceResult(orderId, data) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/result', method: 'post', data: data })
}

// 厂商手动完成（PROCESSING → FINISHED，允许短交，无请求体）
export function completeOutsource(orderId) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/complete', method: 'post' })
}

// 厂商发货（FINISHED → SHIPPED，无请求体）
export function shipOutsource(orderId) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/ship', method: 'post' })
}

// 我方收货（无请求体）
export function receiveOutsource(orderId) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/receive', method: 'post' })
}

// 修改草稿单发料行（仓库/批次/数量，仅 DRAFT 状态）
export function updateIssueLines(orderId, data) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/issueLines', method: 'put', data: data })
}

// 执行发料（草稿 DRAFT → 已发料 ISSUED，扣库存）
export function executeOutsource(orderId) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/execute', method: 'post' })
}
