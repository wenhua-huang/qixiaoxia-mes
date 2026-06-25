import request from '@/utils/request'

export function listRouteProductBom(query) { return request({ url: '/mes/pro/routeproductbom/list', method: 'get', params: query }) }
export function listRouteProductBomByRouteId(routeId) { return request({ url: '/mes/pro/routeproductbom/listByRouteId/' + routeId, method: 'get' }) }
export function getRouteProductBom(recordId) { return request({ url: '/mes/pro/routeproductbom/' + recordId, method: 'get' }) }
export function addRouteProductBom(data) { return request({ url: '/mes/pro/routeproductbom', method: 'post', data: data }) }
export function updateRouteProductBom(data) { return request({ url: '/mes/pro/routeproductbom', method: 'put', data: data }) }
export function delRouteProductBom(recordId) { return request({ url: '/mes/pro/routeproductbom/' + recordId, method: 'delete' }) }
