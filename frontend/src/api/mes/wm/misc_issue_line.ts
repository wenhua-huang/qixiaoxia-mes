import request from '@/utils/request'
import type { WmMiscIssueLineQueryParams, WmMiscIssueLine, AjaxResult, TableDataInfo } from '@/types'

export function listWmMiscIssueLine(q: WmMiscIssueLineQueryParams): Promise<TableDataInfo<WmMiscIssueLine[]>> {
  return request({ url: '/mes/wm/misc_issue_line/list', method: 'get', params: q })
}

export function listAllWmMiscIssueLine(): Promise<AjaxResult<WmMiscIssueLine[]>> {
  return request({ url: '/mes/wm/misc_issue_line/listAll', method: 'get' })
}

export function getWmMiscIssueLine(lineId: number): Promise<AjaxResult<WmMiscIssueLine>> {
  return request({ url: '/mes/wm/misc_issue_line/' + lineId, method: 'get' })
}

export function addWmMiscIssueLine(d: WmMiscIssueLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_issue_line', method: 'post', data: d })
}

export function updateWmMiscIssueLine(d: WmMiscIssueLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_issue_line', method: 'put', data: d })
}

export function delWmMiscIssueLine(lineId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_issue_line/' + lineId, method: 'delete' })
}