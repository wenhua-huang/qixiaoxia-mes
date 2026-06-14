import request from '@/utils/request'

// 查询采购订单行列表
export function listLine(query) {
  return request({
    url: '/mes/pur/order-line/list',
    method: 'get',
    params: query
  })
}

// 查询采购订单行详细
export function getLine(lineId) {
  return request({
    url: '/mes/pur/order-line/' + lineId,
    method: 'get'
  })
}

// 新增采购订单行
export function addLine(data) {
  return request({
    url: '/mes/pur/order-line',
    method: 'post',
    data: data
  })
}

// 修改采购订单行
export function updateLine(data) {
  return request({
    url: '/mes/pur/order-line',
    method: 'put',
    data: data
  })
}

// 删除采购订单行
export function delLine(lineId) {
  return request({
    url: '/mes/pur/order-line/' + lineId,
    method: 'delete'
  })
}
