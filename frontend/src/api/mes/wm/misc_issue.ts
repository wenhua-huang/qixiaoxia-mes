import request from '@/utils/request'
import type { WmMiscIssueQueryParams, WmMiscIssue, AjaxResult, TableDataInfo } from '@/types'

export function listWmMiscIssue(q: WmMiscIssueQueryParams): Promise<TableDataInfo<WmMiscIssue[]>> {
  return request({ url: '/mes/wm/misc_issue/list', method: 'get', params: q })
}

export function listAllWmMiscIssue(): Promise<AjaxResult<WmMiscIssue[]>> {
  return request({ url: '/mes/wm/misc_issue/listAll', method: 'get' })
}

export function getWmMiscIssue(issueId: number): Promise<AjaxResult<WmMiscIssue>> {
  return request({ url: '/mes/wm/misc_issue/' + issueId, method: 'get' })
}

export function addWmMiscIssue(d: WmMiscIssue): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_issue', method: 'post', data: d })
}

export function updateWmMiscIssue(d: WmMiscIssue): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_issue', method: 'put', data: d })
}

export function delWmMiscIssue(issueId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_issue/' + issueId, method: 'delete' })
}