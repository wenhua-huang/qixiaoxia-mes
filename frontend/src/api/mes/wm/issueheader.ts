import request from '@/utils/request'

// 查询列表
export function listIssueHeader(query: any) {
  return request({ url: '/mes/wm/issueheader/list', method: 'get', params: query })
}
// 查询详细
export function getIssueHeader(issueId: number) { return request({ url: '/mes/wm/issueheader/' + issueId, method: 'get' }) }
// 新增
export function addIssueHeader(data: any) { return request({ url: '/mes/wm/issueheader', method: 'post', data }) }
// 修改
export function updateIssueHeader(data: any) { return request({ url: '/mes/wm/issueheader', method: 'put', data }) }
// 删除
export function delIssueHeader(ids: any) { return request({ url: '/mes/wm/issueheader/' + ids, method: 'delete' }) }
