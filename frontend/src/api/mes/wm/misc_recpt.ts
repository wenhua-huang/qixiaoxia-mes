import request from '@/utils/request'
import type { WmMiscRecptQueryParams, WmMiscRecpt, AjaxResult, TableDataInfo } from '@/types'

export function listWmMiscRecpt(q: WmMiscRecptQueryParams): Promise<TableDataInfo<WmMiscRecpt[]>> {
  return request({ url: '/mes/wm/misc_recpt/list', method: 'get', params: q })
}

export function listAllWmMiscRecpt(): Promise<AjaxResult<WmMiscRecpt[]>> {
  return request({ url: '/mes/wm/misc_recpt/listAll', method: 'get' })
}

export function getWmMiscRecpt(recptId: number): Promise<AjaxResult<WmMiscRecpt>> {
  return request({ url: '/mes/wm/misc_recpt/' + recptId, method: 'get' })
}

export function addWmMiscRecpt(d: WmMiscRecpt): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_recpt', method: 'post', data: d })
}

export function updateWmMiscRecpt(d: WmMiscRecpt): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_recpt', method: 'put', data: d })
}

export function delWmMiscRecpt(recptId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/misc_recpt/' + recptId, method: 'delete' })
}