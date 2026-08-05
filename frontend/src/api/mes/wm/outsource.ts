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

// 厂商录加工结果
export function recordOutsourceResult(orderId: number, data: any[]) {
  return request({ url: `/mes/wm/outsource/${orderId}/result`, method: 'post', data })
}

// 我方收货（入库+建报工+推进流转卡）
export function receiveOutsource(orderId: number) {
  return request({ url: `/mes/wm/outsource/${orderId}/receive`, method: 'post' })
}
