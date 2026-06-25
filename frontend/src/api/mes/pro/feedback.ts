import request from '@/utils/request'

// 查询列表
export function listFeedback(query: any) {
  return request({ url: '/mes/pro/feedback/list', method: 'get', params: query })
}
// 查询详细
export function getFeedback(recordId: number) { return request({ url: '/mes/pro/feedback/' + recordId, method: 'get' }) }
// 新增
export function addFeedback(data: any) { return request({ url: '/mes/pro/feedback', method: 'post', data }) }
// 修改
export function updateFeedback(data: any) { return request({ url: '/mes/pro/feedback', method: 'put', data }) }
// 删除
export function delFeedback(ids: any) { return request({ url: '/mes/pro/feedback/' + ids, method: 'delete' }) }
// 根据任务ID查询反馈列表
export function listFeedbackByTaskId(taskId: number) { return request({ url: '/mes/pro/feedback/listByTaskId/' + taskId, method: 'get' }) }
// 确认报工：PREPARE → CONFIRMED
export function confirmFeedback(recordId: number) { return request({ url: '/mes/pro/feedback/confirm/' + recordId, method: 'put' }) }
// 审核报工：CONFIRMED → AUDITED
export function auditFeedback(recordId: number) { return request({ url: '/mes/pro/feedback/audit/' + recordId, method: 'put' }) }
// 获取工单默认物料消耗（新增报工时预填）
export function getConsumeDefaults(workorderId: number) { return request({ url: '/mes/pro/feedback/consumeDefaults/' + workorderId, method: 'get' }) }
