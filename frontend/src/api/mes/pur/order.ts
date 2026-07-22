import request from '@/utils/request'

// 查询采购订单头列表
export function listOrder(query) {
  return request({
    url: '/mes/pur/order/list',
    method: 'get',
    params: query
  })
}

// 查询采购订单头详细
export function getOrder(orderId) {
  return request({
    url: '/mes/pur/order/' + orderId,
    method: 'get'
  })
}

// 新增采购订单头
export function addOrder(data) {
  return request({
    url: '/mes/pur/order',
    method: 'post',
    data: data
  })
}

// 修改采购订单头
export function updateOrder(data) {
  return request({
    url: '/mes/pur/order',
    method: 'put',
    data: data
  })
}

// 删除采购订单头
export function delOrder(orderId) {
  return request({
    url: '/mes/pur/order/' + orderId,
    method: 'delete'
  })
}

// 审批采购订单（DRAFT -> APPROVED）
export function approveOrder(orderId) {
  return request({
    url: '/mes/pur/order/' + orderId + '/approve',
    method: 'post'
  })
}

// 下达采购订单（APPROVED -> ORDERED）
export function orderOrder(orderId) {
  return request({
    url: '/mes/pur/order/' + orderId + '/order',
    method: 'post'
  })
}

// 关闭采购订单（RECEIVED -> CLOSED 正常，RECEIVING -> CLOSED 强制关闭）
export function closeOrder(orderId, closeReason) {
  return request({
    url: '/mes/pur/order/' + orderId + '/close',
    method: 'post',
    params: { closeReason: closeReason }
  })
}

// 取消采购订单（DRAFT/APPROVED/ORDERED -> CANCEL）
export function cancelOrder(orderId, cancelReason) {
  return request({
    url: '/mes/pur/order/' + orderId + '/cancel',
    method: 'post',
    params: { cancelReason: cancelReason }
  })
}

// 取消采购订单行（ORDERED/RECEIVING -> CANCEL）
export function cancelLine(lineId, cancelReason) {
  return request({
    url: '/mes/pur/order/line/' + lineId + '/cancel',
    method: 'post',
    params: { cancelReason: cancelReason }
  })
}

// 终止收货采购订单行（RECEIVING -> CLOSED）
export function terminateLine(lineId, closeReason) {
  return request({
    url: '/mes/pur/order/line/' + lineId + '/terminate',
    method: 'post',
    params: { closeReason: closeReason }
  })
}
