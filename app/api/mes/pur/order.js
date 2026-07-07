import request from '@/utils/request'

// 查询采购订单头列表（支持按编码模糊搜索）
export function listOrder(query) {
  return request({
    url: '/mes/pur/order/list',
    method: 'get',
    params: query
  })
}

// 查询采购订单头详情
export function getOrder(orderId) {
  return request({
    url: '/mes/pur/order/' + orderId,
    method: 'get'
  })
}

// 查询采购订单行列表
export function listOrderLine(query) {
  return request({
    url: '/mes/pur/order-line/list',
    method: 'get',
    params: query
  })
}

// 审批采购订单（DRAFT → APPROVED）
export function approveOrder(orderId) {
  return request({
    url: '/mes/pur/order/' + orderId + '/approve',
    method: 'post'
  })
}

// 下达采购订单（APPROVED → ORDERED）
export function orderOrder(orderId) {
  return request({
    url: '/mes/pur/order/' + orderId + '/order',
    method: 'post'
  })
}

// 关闭采购订单（RECEIVED → CLOSED）
export function closeOrder(orderId) {
  return request({
    url: '/mes/pur/order/' + orderId + '/close',
    method: 'post'
  })
}

// 查询入库单列表（收货历史用）
export function listItemRecpt(query) {
  return request({
    url: '/mes/wm/item_recpt/list',
    method: 'get',
    params: query
  })
}

// 确认收货（入库单 DRAFT → CONFIRMED）
export function confirmItemRecpt(recptId) {
  return request({
    url: '/mes/wm/item_recpt/confirm/' + recptId,
    method: 'put'
  })
}

// 过账入库（入库单 CONFIRMED → POSTED）
export function postItemRecpt(recptId) {
  return request({
    url: '/mes/wm/item_recpt/post/' + recptId,
    method: 'put'
  })
}

// 查询入库单详情（单条）
export function getItemRecpt(recptId) {
  return request({
    url: '/mes/wm/item_recpt/' + recptId,
    method: 'get'
  })
}

// 查询入库单行列表
export function listItemRecptLines(query) {
  return request({
    url: '/mes/wm/item_recpt_line/list',
    method: 'get',
    params: query
  })
}

// 一键收货：入库单头+行+确认，单接口原子完成
export function receiveItemRecpt(data) {
  return request({
    url: '/mes/wm/item_recpt/receive',
    method: 'post',
    data: data
  })
}

// 查询仓库列表（用于下拉选择）
export function listWarehouseAll() {
  return request({
    url: '/mes/wm/warehouse/listAll',
    method: 'get'
  })
}
