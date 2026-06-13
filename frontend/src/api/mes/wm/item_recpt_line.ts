import request from '@/utils/request'
import type { WmItemRecptLineQueryParams, WmItemRecptLine, AjaxResult, TableDataInfo } from '@/types'

export function listWmItemRecptLine(q: WmItemRecptLineQueryParams): Promise<TableDataInfo<WmItemRecptLine[]>> {
  return request({ url: '/mes/wm/item_recpt_line/list', method: 'get', params: q })
}

export function listAllWmItemRecptLine(): Promise<AjaxResult<WmItemRecptLine[]>> {
  return request({ url: '/mes/wm/item_recpt_line/listAll', method: 'get' })
}

export function getWmItemRecptLine(lineId: number): Promise<AjaxResult<WmItemRecptLine>> {
  return request({ url: '/mes/wm/item_recpt_line/' + lineId, method: 'get' })
}

export function addWmItemRecptLine(d: WmItemRecptLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt_line', method: 'post', data: d })
}

export function updateWmItemRecptLine(d: WmItemRecptLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt_line', method: 'put', data: d })
}

export function delWmItemRecptLine(lineId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt_line/' + lineId, method: 'delete' })
}