import request from '@/utils/request'

// 查询列表
export function listIssueLine(query: any) {
  return request({ url: '/mes/wm/issueline/list', method: 'get', params: query })
}
// 查询详细
export function getIssueLine(lineId: number) { return request({ url: '/mes/wm/issueline/' + lineId, method: 'get' }) }
// 新增
export function addIssueLine(data: any) { return request({ url: '/mes/wm/issueline', method: 'post', data }) }
// 修改
export function updateIssueLine(data: any) { return request({ url: '/mes/wm/issueline', method: 'put', data }) }
// 删除
export function delIssueLine(ids: any) { return request({ url: '/mes/wm/issueline/' + ids, method: 'delete' }) }
// 根据发料单ID查询行列表
export function listIssueLineByIssueId(issueId: number) { return request({ url: '/mes/wm/issueline/listByIssueId/' + issueId, method: 'get' }) }
