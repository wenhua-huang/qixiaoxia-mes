import request from '@/utils/request'

// 查询工单变更列表
export function listWorkorderChange(query) { return request({ url: '/mes/pro/workorderchange/list', method: 'get', params: query }) }
// 按工单ID查询变更记录
export function listChangeByWorkorderId(workorderId) { return request({ url: '/mes/pro/workorderchange/listByWorkorderId/' + workorderId, method: 'get' }) }
// 查询变更详情
export function getWorkorderChange(changeId) { return request({ url: '/mes/pro/workorderchange/' + changeId, method: 'get' }) }
// 新增变更申请
export function addWorkorderChange(data) { return request({ url: '/mes/pro/workorderchange', method: 'post', data }) }
// 修改变更申请
export function updateWorkorderChange(data) { return request({ url: '/mes/pro/workorderchange', method: 'put', data }) }
// 审批变更
export function approveWorkorderChange(changeId) { return request({ url: '/mes/pro/workorderchange/approve/' + changeId, method: 'put' }) }
// 删除变更记录
export function delWorkorderChange(ids) { return request({ url: '/mes/pro/workorderchange/' + ids, method: 'delete' }) }
