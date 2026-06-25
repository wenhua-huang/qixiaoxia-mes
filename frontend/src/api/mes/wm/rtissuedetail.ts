import request from '@/utils/request'

// 查询列表
export function listRtIssueDetail(query: any) {
  return request({ url: '/mes/wm/rtissuedetail/list', method: 'get', params: query })
}
// 查询详细
export function getRtIssueDetail(detailId: number) { return request({ url: '/mes/wm/rtissuedetail/' + detailId, method: 'get' }) }
// 新增
export function addRtIssueDetail(data: any) { return request({ url: '/mes/wm/rtissuedetail', method: 'post', data }) }
// 修改
export function updateRtIssueDetail(data: any) { return request({ url: '/mes/wm/rtissuedetail', method: 'put', data }) }
// 删除
export function delRtIssueDetail(ids: any) { return request({ url: '/mes/wm/rtissuedetail/' + ids, method: 'delete' }) }
