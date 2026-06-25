import request from '@/utils/request'

export function listRouteProduct(query) { return request({ url: '/mes/pro/routeproduct/list', method: 'get', params: query }) }
export function listRouteProductByRouteId(routeId) { return request({ url: '/mes/pro/routeproduct/listByRouteId/' + routeId, method: 'get' }) }
export function getRouteProduct(recordId) { return request({ url: '/mes/pro/routeproduct/' + recordId, method: 'get' }) }
export function addRouteProduct(data) { return request({ url: '/mes/pro/routeproduct', method: 'post', data: data }) }
export function updateRouteProduct(data) { return request({ url: '/mes/pro/routeproduct', method: 'put', data: data }) }
export function delRouteProduct(recordId) { return request({ url: '/mes/pro/routeproduct/' + recordId, method: 'delete' }) }
