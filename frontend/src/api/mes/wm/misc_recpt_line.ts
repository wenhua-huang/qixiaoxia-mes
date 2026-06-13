import request from '@/utils/request'
import type { WmMiscRecptLineQueryParams, WmMiscRecptLine, AjaxResult, TableDataInfo } from '@/types'

export function listWmMiscRecptLine(q: WmMiscRecptLineQueryParams): Promise<TableDataInfo<WmMiscRecptLine[]>> {
  return request({ url: '/mes/wm/misc_recpt_line/list', method: 'get', params: q })
}

export function listAllWmMiscRecptLine(): Promise<AjaxResult<WmMiscRecptLine[]>> {
  return request({ url: '/mes/wm/misc_recpt_line/listAll', method: 'get' })
}

export function getWmMiscRecptLine(lineId: number): Promise<AjaxResult<WmMiscRecptLine>> {
  return request({ url: '/mes/wm/misc_recpt_line/' + lineId, method: 'get' })
}

export function addWmMiscRecptLine(d: WmMiscRecptLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_recpt_line', method: 'post', data: d })
}

export function updateWmMiscRecptLine(d: WmMiscRecptLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_recpt_line', method: 'put', data: d })
}

export function delWmMiscRecptLine(lineId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_recpt_line/' + lineId, method: 'delete' })
}