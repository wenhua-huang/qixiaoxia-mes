import request from '@/utils/request'

// 新增报工（POST /mes/pro/feedback）
export function addFeedback(data) {
  return request({ url: '/mes/pro/feedback', method: 'post', data: data })
}

// 报工列表（分页，支持按状态/工单过滤）
export function listFeedback(query) {
  return request({ url: '/mes/pro/feedback/list', method: 'get', params: query })
}

// 报工详情
export function getFeedback(recordId) {
  return request({ url: '/mes/pro/feedback/' + recordId, method: 'get' })
}

// 按工单编码搜索工单
export function getWorkorderByCode(workorderCode) {
  return request({ url: '/mes/pro/workorder/list', method: 'get', params: { workorderCode } })
}

// 工单详情
export function getWorkorderDetail(workorderId) {
  return request({ url: '/mes/pro/workorder/' + workorderId, method: 'get' })
}

// 按工单ID查询任务列表（每个任务对应一个工序）
export function listTaskByWorkorder(workorderId) {
  return request({ url: '/mes/pro/task/list', method: 'get', params: { workorderId } })
}

// 移动端报工入口：一次请求返回工单 + 可报工任务列表（PRODUCING 状态）
export function getFeedbackEntry(workorderCode) {
  return request({ url: '/mes/pro/workorder/feedbackEntry/' + encodeURIComponent(workorderCode), method: 'get' })
}
