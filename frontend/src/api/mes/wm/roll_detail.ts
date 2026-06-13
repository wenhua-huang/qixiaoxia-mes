import request from '@/utils/request'
import type { WmRollDetailQueryParams, WmRollDetail, AjaxResult, TableDataInfo } from '@/types'

export function listWmRollDetail(q: WmRollDetailQueryParams): Promise<TableDataInfo<WmRollDetail[]>> {
  return request({ url: '/mes/wm/roll_detail/list', method: 'get', params: q })
}

export function listAllWmRollDetail(): Promise<AjaxResult<WmRollDetail[]>> {
  return request({ url: '/mes/wm/roll_detail/listAll', method: 'get' })
}

export function getWmRollDetail(rollId: number): Promise<AjaxResult<WmRollDetail>> {
  return request({ url: '/mes/wm/roll_detail/' + rollId, method: 'get' })
}

export function addWmRollDetail(d: WmRollDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/roll_detail', method: 'post', data: d })
}

export function updateWmRollDetail(d: WmRollDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/roll_detail', method: 'put', data: d })
}

export function delWmRollDetail(rollId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/roll_detail/' + rollId, method: 'delete' })
}