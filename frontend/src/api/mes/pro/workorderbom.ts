import request from '@/utils/request'

export function listWorkorderBom(query) { return request({ url: '/mes/pro/workorderbom/list', method: 'get', params: query }) }
export function listBomByWorkorderId(workorderId) { return request({ url: '/mes/pro/workorderbom/listByWorkorderId/' + workorderId, method: 'get' }) }
export function addWorkorderBom(data) { return request({ url: '/mes/pro/workorderbom', method: 'post', data }) }
export function updateWorkorderBom(data) { return request({ url: '/mes/pro/workorderbom', method: 'put', data }) }
export function delWorkorderBom(ids) { return request({ url: '/mes/pro/workorderbom/' + ids, method: 'delete' }) }
