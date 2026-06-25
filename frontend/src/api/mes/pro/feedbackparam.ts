import request from '@/utils/request'

// 查询列表
export function listFeedbackParam(query: any) {
  return request({ url: '/mes/pro/feedbackparam/list', method: 'get', params: query })
}
// 查询详细
export function getFeedbackParam(recordId: number) { return request({ url: '/mes/pro/feedbackparam/' + recordId, method: 'get' }) }
// 新增
export function addFeedbackParam(data: any) { return request({ url: '/mes/pro/feedbackparam', method: 'post', data }) }
// 修改
export function updateFeedbackParam(data: any) { return request({ url: '/mes/pro/feedbackparam', method: 'put', data }) }
// 删除
export function delFeedbackParam(ids: any) { return request({ url: '/mes/pro/feedbackparam/' + ids, method: 'delete' }) }
// 根据反馈ID查询反馈参数列表
export function listFeedbackParamByFeedbackId(feedbackId: number) { return request({ url: '/mes/pro/feedbackparam/listByFeedbackId/' + feedbackId, method: 'get' }) }
