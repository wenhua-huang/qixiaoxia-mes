import request from '@/utils/request'
import type { WmItemRecptDetailQueryParams, WmItemRecptDetail, AjaxResult, TableDataInfo } from '@/types'

export function listWmItemRecptDetail(q: WmItemRecptDetailQueryParams): Promise<TableDataInfo<WmItemRecptDetail[]>> {
  return request({ url: '/mes/wm/item_recpt_detail/list', method: 'get', params: q })
}

export function listAllWmItemRecptDetail(): Promise<AjaxResult<WmItemRecptDetail[]>> {
  return request({ url: '/mes/wm/item_recpt_detail/listAll', method: 'get' })
}

export function getWmItemRecptDetail(detailId: number): Promise<AjaxResult<WmItemRecptDetail>> {
  return request({ url: '/mes/wm/item_recpt_detail/' + detailId, method: 'get' })
}

export function addWmItemRecptDetail(d: WmItemRecptDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt_detail', method: 'post', data: d })
}

export function updateWmItemRecptDetail(d: WmItemRecptDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt_detail', method: 'put', data: d })
}

export function delWmItemRecptDetail(detailId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt_detail/' + detailId, method: 'delete' })
}