import request from '@/utils/request'

// 外协单列表（厂商账号后端自动按 vendorId 过滤）
export function listOutsource(query) {
  return request({ url: '/mes/wm/outsource/list', method: 'get', params: query })
}

// 外协单详情（含发料行+收货行）
export function getOutsource(orderId) {
  return request({ url: '/mes/wm/outsource/' + orderId, method: 'get' })
}

// 厂商录加工结果（请求体是数组，后端自动继承发料物料）
export function recordOutsourceResult(orderId, data) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/result', method: 'post', data: data })
}

// 我方收货（无请求体）
export function receiveOutsource(orderId) {
  return request({ url: '/mes/wm/outsource/' + orderId + '/receive', method: 'post' })
}
