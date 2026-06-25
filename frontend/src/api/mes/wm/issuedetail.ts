import request from '@/utils/request'

// 查询列表
export function listIssueDetail(query: any) {
  return request({ url: '/mes/wm/issuedetail/list', method: 'get', params: query })
}
// 查询详细
export function getIssueDetail(detailId: number) { return request({ url: '/mes/wm/issuedetail/' + detailId, method: 'get' }) }
// 新增
export function addIssueDetail(data: any) { return request({ url: '/mes/wm/issuedetail', method: 'post', data }) }
// 修改
export function updateIssueDetail(data: any) { return request({ url: '/mes/wm/issuedetail', method: 'put', data }) }
// 删除
export function delIssueDetail(ids: any) { return request({ url: '/mes/wm/issuedetail/' + ids, method: 'delete' }) }
