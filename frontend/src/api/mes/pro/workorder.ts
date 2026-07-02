import request from '@/utils/request'

export function listWorkorder(query) { return request({ url: '/mes/pro/workorder/list', method: 'get', params: query }) }
export function listAllWorkorder() { return request({ url: '/mes/pro/workorder/listAll', method: 'get' }) }
export function getWorkorder(workorderId) { return request({ url: '/mes/pro/workorder/' + workorderId, method: 'get' }) }
// 获取工单详情（含BOM和工序参数，合并返回）
export function getWorkorderDetail(workorderId) { return request({ url: '/mes/pro/workorder/detail/' + workorderId, method: 'get' }) }
export function addWorkorder(data) { return request({ url: '/mes/pro/workorder', method: 'post', data }) }
// 新增工单（含BOM和工序参数）
export function createWorkorderWithBom(data) { return request({ url: '/mes/pro/workorder/createWithBom', method: 'post', data }) }
export function updateWorkorder(data) { return request({ url: '/mes/pro/workorder', method: 'put', data }) }
// 修改工单（含BOM和工序参数）
export function updateWorkorderWithBom(data) { return request({ url: '/mes/pro/workorder/updateWithBom', method: 'put', data }) }
export function delWorkorder(ids) { return request({ url: '/mes/pro/workorder/' + ids, method: 'delete' }) }
// 开工：PREPARE → PRODUCING（含物料齐套校验）
export function startWorkorder(workorderId) { return request({ url: '/mes/pro/workorder/start/' + workorderId, method: 'put' }) }
// 物料齐套检查（仅查询，不执行开工）
export function checkWorkorderMaterial(workorderId) { return request({ url: '/mes/pro/workorder/checkMaterial/' + workorderId, method: 'get' }) }
// 开工前检查 + 自动生成领料单 + 开工（四步流程）
export function startWorkorderWithCheck(workorderId) { return request({ url: '/mes/pro/workorder/startWithCheck/' + workorderId, method: 'put' }) }
// 偏离检测：比较提交的BOM/参数与路线标准，返回偏离明细（后端权威判定）
export function checkDeviation(data) { return request({ url: '/mes/pro/workorder/checkDeviation', method: 'post', data }) }
// 取消工单：PREPARE/PRODUCING → CANCEL
export function cancelWorkorder(workorderId) { return request({ url: '/mes/pro/workorder/cancel/' + workorderId, method: 'put' }) }
