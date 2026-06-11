import request from '@/utils/request'
import type { BatchConfigQueryParams, MdItemBatchConfig, AjaxResult, TableDataInfo } from '@/types'

export function listBatchConfig(query: BatchConfigQueryParams): Promise<TableDataInfo<MdItemBatchConfig[]>> {
  return request({ url: '/mes/md/batchconfig/list', method: 'get', params: query })
}

export function getBatchConfigByItemId(itemId: number): Promise<AjaxResult<MdItemBatchConfig>> {
  return request({ url: '/mes/md/batchconfig/byItem/' + itemId, method: 'get' })
}

export function getBatchConfig(configId: number): Promise<AjaxResult<MdItemBatchConfig>> {
  return request({ url: '/mes/md/batchconfig/' + configId, method: 'get' })
}

export function addBatchConfig(data: MdItemBatchConfig): Promise<AjaxResult> {
  return request({ url: '/mes/md/batchconfig', method: 'post', data })
}

export function updateBatchConfig(data: MdItemBatchConfig): Promise<AjaxResult> {
  return request({ url: '/mes/md/batchconfig', method: 'put', data })
}

export function delBatchConfig(configId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/md/batchconfig/' + configId, method: 'delete' })
}
