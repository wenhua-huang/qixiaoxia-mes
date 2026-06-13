import request from '@/utils/request'
import type { WmItemRecptQueryParams, WmItemRecpt, AjaxResult, TableDataInfo } from '@/types'

export function listWmItemRecpt(q: WmItemRecptQueryParams): Promise<TableDataInfo<WmItemRecpt[]>> {
  return request({ url: '/mes/wm/item_recpt/list', method: 'get', params: q })
}

export function listAllWmItemRecpt(): Promise<AjaxResult<WmItemRecpt[]>> {
  return request({ url: '/mes/wm/item_recpt/listAll', method: 'get' })
}

export function getWmItemRecpt(recptId: number): Promise<AjaxResult<WmItemRecpt>> {
  return request({ url: '/mes/wm/item_recpt/' + recptId, method: 'get' })
}

export function addWmItemRecpt(d: WmItemRecpt): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt', method: 'post', data: d })
}

export function updateWmItemRecpt(d: WmItemRecpt): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt', method: 'put', data: d })
}

export function delWmItemRecpt(recptId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/item_recpt/' + recptId, method: 'delete' })
}