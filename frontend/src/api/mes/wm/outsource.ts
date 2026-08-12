import request from '@/utils/request'

// 外协单列表
export function listOutsource(query: any) {
  return request({ url: '/mes/wm/outsource/list', method: 'get', params: query })
}

// 外协单详情（含发料行+收货行）
export function getOutsource(orderId: number) {
  return request({ url: '/mes/wm/outsource/' + orderId, method: 'get' })
}

// 创建外协发货单（发料扣库存+流转卡OUTSOURCING）
export function createOutsource(data: any) {
  return request({ url: '/mes/wm/outsource/create', method: 'post', data })
}

// 删除草稿外协单（仅 DRAFT 状态，未扣库存，级联删发料行）
export function delOutsource(orderId: number) {
  return request({ url: `/mes/wm/outsource/${orderId}`, method: 'delete' })
}

// 执行发料（草稿 DRAFT → 已发料 ISSUED，扣库存）
export function executeOutsource(orderId: number) {
  return request({ url: `/mes/wm/outsource/${orderId}/execute`, method: 'post' })
}

// 批量执行发料（逐张草稿单扣库存发料，单条失败不影响其他单）
export function batchExecuteOutsource(orderIds: number[]) {
  return request({ url: '/mes/wm/outsource/batchExecute', method: 'post', data: orderIds })
}

// 厂商录加工结果
export function recordOutsourceResult(orderId: number, data: any[]) {
  return request({ url: `/mes/wm/outsource/${orderId}/result`, method: 'post', data })
}

// 我方收货（入库+建报工+推进流转卡）
export function receiveOutsource(orderId: number) {
  return request({ url: `/mes/wm/outsource/${orderId}/receive`, method: 'post' })
}

// 批量收货（逐单入库收货，单条失败不影响其他单）
export function batchReceiveOutsource(orderIds: number[]) {
  return request({ url: '/mes/wm/outsource/batchReceive', method: 'post', data: orderIds })
}
