import request from '@/utils/request'

// 查询列表
export function listIssueHeader(query: any) {
  return request({ url: '/mes/wm/issueheader/list', method: 'get', params: query })
}
// 查询详细
export function getIssueHeader(issueId: number) { return request({ url: '/mes/wm/issueheader/' + issueId, method: 'get' }) }
// 查询详情（头+行聚合）
export function getIssueDetail(issueId: number) { return request({ url: '/mes/wm/issueheader/detail/' + issueId, method: 'get' }) }
// 新增
export function addIssueHeader(data: any) { return request({ url: '/mes/wm/issueheader', method: 'post', data }) }
// 修改
export function updateIssueHeader(data: any) { return request({ url: '/mes/wm/issueheader', method: 'put', data }) }
// 删除
export function delIssueHeader(ids: any) { return request({ url: '/mes/wm/issueheader/' + ids, method: 'delete' }) }
// 确认领料单（DRAFT→CONFIRMED，预占库存）
export function confirmIssue(issueId: number) { return request({ url: '/mes/wm/issueheader/confirm/' + issueId, method: 'put' }) }
// 执行出库（CONFIRMED→POSTED，扣库存+过账）
export function executeIssue(issueId: number) { return request({ url: '/mes/wm/issueheader/execute/' + issueId, method: 'put' }) }
