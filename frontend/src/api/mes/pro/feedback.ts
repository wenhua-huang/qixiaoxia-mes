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
// 确认报工：PREPARE → CONFIRMED
export function confirmFeedback(recordId: number) { return request({ url: '/mes/pro/feedback/confirm/' + recordId, method: 'put' }) }
// 审核报工：CONFIRMED → AUDITED
export function auditFeedback(recordId: number) { return request({ url: '/mes/pro/feedback/audit/' + recordId, method: 'put' }) }
// 批量确认报工：PREPARE → CONFIRMED，尽力执行
export function batchConfirmFeedback(recordIds: number[]) {
  return request({ url: '/mes/pro/feedback/batchConfirm', method: 'put', data: recordIds })
}
// 批量审核报工：CONFIRMED → AUDITED，尽力执行
export function batchAuditFeedback(recordIds: number[]) {
  return request({ url: '/mes/pro/feedback/batchAudit', method: 'put', data: recordIds })
}
// 获取工单默认物料消耗（新增报工时预填）
export function getConsumeDefaults(workorderId: number) { return request({ url: '/mes/pro/feedback/consumeDefaults/' + workorderId, method: 'get' }) }
// 查询报工字段变更痕迹（上机数量默认值人工调整等），feedbackId 优先，缺省按 taskId
export function getFeedbackChanges(params: { feedbackId?: number | string; taskId?: number | string }) {
  return request({ url: '/mes/pro/feedback/change/list', method: 'get', params })
}
// 查询单个任务的「本次上机数量」系统默认值（选择任务后由后端权威预填，前端不自算）
export function getInputDefault(taskId: number | string) {
  return request({ url: '/mes/pro/feedback/inputDefault/' + taskId, method: 'get' })
}
