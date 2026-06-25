import request from '@/utils/request'

export function listRouteProcessParam(query) { return request({ url: '/mes/pro/routeprocessparam/list', method: 'get', params: query }) }
export function listRouteProcessParamByRouteProductId(routeProductId) { return request({ url: '/mes/pro/routeprocessparam/listByRouteProductId/' + routeProductId, method: 'get' }) }
export function getRouteProcessParam(recordId) { return request({ url: '/mes/pro/routeprocessparam/' + recordId, method: 'get' }) }
export function addRouteProcessParam(data) { return request({ url: '/mes/pro/routeprocessparam', method: 'post', data: data }) }
export function updateRouteProcessParam(data) { return request({ url: '/mes/pro/routeprocessparam', method: 'put', data: data }) }
export function delRouteProcessParam(recordId) { return request({ url: '/mes/pro/routeprocessparam/' + recordId, method: 'delete' }) }
export function batchInitFromTemplate(routeProductId, processId) { return request({ url: '/mes/pro/routeprocessparam/batchInitFromTemplate', method: 'post', params: { routeProductId, processId } }) }
export function batchUpdateRouteProcessParam(data) { return request({ url: '/mes/pro/routeprocessparam/batchUpdate', method: 'put', data }) }
