import request from '@/utils/request'

export function listRouteProcess(query) { return request({ url: '/mes/pro/routeprocess/list', method: 'get', params: query }) }
export function listRouteProcessByRouteId(routeId) { return request({ url: '/mes/pro/routeprocess/listByRouteId/' + routeId, method: 'get' }) }
export function getRouteProcess(recordId) { return request({ url: '/mes/pro/routeprocess/' + recordId, method: 'get' }) }
export function addRouteProcess(data) { return request({ url: '/mes/pro/routeprocess', method: 'post', data: data }) }
export function updateRouteProcess(data) { return request({ url: '/mes/pro/routeprocess', method: 'put', data: data }) }
export function delRouteProcess(recordId) { return request({ url: '/mes/pro/routeprocess/' + recordId, method: 'delete' }) }
