import request from '@/utils/request'

export function listWorkorderParam(query) { return request({ url: '/mes/pro/workorderparam/list', method: 'get', params: query }) }
export function listParamByWorkorderId(workorderId) { return request({ url: '/mes/pro/workorderparam/listByWorkorderId/' + workorderId, method: 'get' }) }
export function addWorkorderParam(data) { return request({ url: '/mes/pro/workorderparam', method: 'post', data }) }
export function updateWorkorderParam(data) { return request({ url: '/mes/pro/workorderparam', method: 'put', data }) }
export function delWorkorderParam(ids) { return request({ url: '/mes/pro/workorderparam/' + ids, method: 'delete' }) }
