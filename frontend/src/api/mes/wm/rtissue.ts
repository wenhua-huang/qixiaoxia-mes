import request from '@/utils/request'

// 查询列表
export function listRtIssue(query: any) {
  return request({ url: '/mes/wm/rtissue/list', method: 'get', params: query })
}
// 从领料单生成退料单草稿（不落库，差额退料，返回头+行草稿供前端编辑）
export function buildFromIssue(issueId: number) {
  return request({ url: `/mes/wm/rtissue/buildFromIssue/${issueId}`, method: 'get' })
}
// 退料领料单选择弹窗：分页查 ISSUED 领料单，每行带预算可退量(returnableQty)
export function returnablePreview(query: any) {
  return request({ url: '/mes/wm/rtissue/returnablePreview', method: 'get', params: query })
}
// 执行退库（DRAFT→POSTED，加库存+写事务+物料追溯）
export function executeReturn(rtId: number) {
  return request({ url: `/mes/wm/rtissue/execute/${rtId}`, method: 'put' })
}
// 查询详细
export function getRtIssue(rtId: number) { return request({ url: '/mes/wm/rtissue/' + rtId, method: 'get' }) }
// 新增
export function addRtIssue(data: any) { return request({ url: '/mes/wm/rtissue', method: 'post', data }) }
// 修改
export function updateRtIssue(data: any) { return request({ url: '/mes/wm/rtissue', method: 'put', data }) }
// 删除
export function delRtIssue(ids: any) { return request({ url: '/mes/wm/rtissue/' + ids, method: 'delete' }) }
