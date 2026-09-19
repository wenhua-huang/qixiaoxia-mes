import request from '@/utils/request'

export function listRouteProduct(query) { return request({ url: '/mes/pro/routeproduct/list', method: 'get', params: query }) }
export function listRouteProductByRouteId(routeId) { return request({ url: '/mes/pro/routeproduct/listByRouteId/' + routeId, method: 'get' }) }
export function getRouteProduct(recordId) { return request({ url: '/mes/pro/routeproduct/' + recordId, method: 'get' }) }
export function addRouteProduct(data) { return request({ url: '/mes/pro/routeproduct', method: 'post', data: data }) }
export function updateRouteProduct(data) { return request({ url: '/mes/pro/routeproduct', method: 'put', data: data }) }
export function delRouteProduct(recordId) { return request({ url: '/mes/pro/routeproduct/' + recordId, method: 'delete' }) }

// 按 订单类型+是否外发+是否包装 预览默认路线（开单联动）
export function resolveRouteProduct(query: { itemId: number; orderType?: string; outsourceFlag?: string; packageFlag?: string }) {
  return request({ url: '/mes/pro/routeproduct/resolve', method: 'get', params: query })
}
// 头维度变更后批量重算
export function resolveRouteProductBatch(data: { itemIds: number[]; orderType?: string; outsourceFlag?: string; packageFlag?: string }) {
  return request({ url: '/mes/pro/routeproduct/resolveBatch', method: 'post', data })
}
