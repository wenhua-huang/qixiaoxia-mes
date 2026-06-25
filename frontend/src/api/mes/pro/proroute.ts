import request from '@/utils/request'

export function listRoute(query) { return request({ url: '/mes/pro/proroute/list', method: 'get', params: query }) }
export function getRoute(routeId) { return request({ url: '/mes/pro/proroute/' + routeId, method: 'get' }) }
export function addRoute(data) { return request({ url: '/mes/pro/proroute', method: 'post', data: data }) }
export function updateRoute(data) { return request({ url: '/mes/pro/proroute', method: 'put', data: data }) }
export function delRoute(routeId) { return request({ url: '/mes/pro/proroute/' + routeId, method: 'delete' }) }
