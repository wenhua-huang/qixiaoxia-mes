import request from '@/utils/request'
import type { WmBatchQueryParams, WmBatch, AjaxResult, TableDataInfo } from '@/types'

export function listWmBatch(q: WmBatchQueryParams): Promise<TableDataInfo<WmBatch[]>> {
  return request({ url: '/mes/wm/batch/list', method: 'get', params: q })
}

export function listAllWmBatch(): Promise<AjaxResult<WmBatch[]>> {
  return request({ url: '/mes/wm/batch/listAll', method: 'get' })
}

export function getWmBatch(batchId: number): Promise<AjaxResult<WmBatch>> {
  return request({ url: '/mes/wm/batch/' + batchId, method: 'get' })
}

export function addWmBatch(d: WmBatch): Promise<AjaxResult> {
  return request({ url: '/mes/wm/batch', method: 'post', data: d })
}

export function updateWmBatch(d: WmBatch): Promise<AjaxResult> {
  return request({ url: '/mes/wm/batch', method: 'put', data: d })
}

export function delWmBatch(batchId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/batch/' + batchId, method: 'delete' })
}