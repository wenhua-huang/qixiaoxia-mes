import request from '@/utils/request'

// 查询列表
export function listIssueHeader(query: any) {
  return request({ url: '/mes/wm/issueheader/list', method: 'get', params: query })
}
// 从工单BOM导入领料行
export function loadBomLines(issueId: number, workorderId: number) {
  return request({ url: `/mes/wm/issueheader/loadBom/${issueId}/${workorderId}`, method: 'put' })
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
// 确认领料单（DRAFT/APPROVED→ALLOCATED 已预占，扣减可用库存）
export function confirmIssue(issueId: number) { return request({ url: '/mes/wm/issueheader/confirm/' + issueId, method: 'put' }) }
// 释放预占（ALLOCATED→APPROVED，恢复可用库存）
export function releaseAllocation(issueId: number) { return request({ url: '/mes/wm/issueheader/release/' + issueId, method: 'put' }) }
// 执行出库（旧接口，全量发料，ALLOCATED→ISSUED）
export function executeIssue(issueId: number) { return request({ url: '/mes/wm/issueheader/execute/' + issueId, method: 'put' }) }
// 提交审核（DRAFT→PENDING）
export function submitForApprove(issueId: number) { return request({ url: '/mes/wm/issueheader/submit/' + issueId, method: 'put' }) }
// 审核通过（PENDING→APPROVED）
export function approveIssue(issueId: number) { return request({ url: '/mes/wm/issueheader/approve/' + issueId, method: 'put' }) }
// 审核退回（PENDING→DRAFT）
export function rejectIssue(issueId: number) { return request({ url: '/mes/wm/issueheader/reject/' + issueId, method: 'put' }) }
// 发料出库（分批，ALLOCATED/PARTIAL_ISSUED→PARTIAL_ISSUED/ISSUED），body 为发料明细数组
export function issueOut(issueId: number, details: any[]) { return request({ url: '/mes/wm/issueheader/issueOut/' + issueId, method: 'put', data: details }) }
// 关闭（ISSUED/PARTIAL_ISSUED→CLOSED 终态）
export function closeIssue(issueId: number) { return request({ url: '/mes/wm/issueheader/close/' + issueId, method: 'put' }) }
// 作废（非终态→CANCELED）
export function cancelIssue(issueId: number, reason?: string) { return request({ url: '/mes/wm/issueheader/cancel/' + issueId, method: 'put', data: { reason: reason || '' } }) }
