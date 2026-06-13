import request from '@/utils/request'
import type { WmTransactionQueryParams, WmTransaction, AjaxResult, TableDataInfo } from '@/types'

export function listWmTransaction(q: WmTransactionQueryParams): Promise<TableDataInfo<WmTransaction[]>> {
  return request({ url: '/mes/wm/transaction/list', method: 'get', params: q })
}

export function listAllWmTransaction(): Promise<AjaxResult<WmTransaction[]>> {
  return request({ url: '/mes/wm/transaction/listAll', method: 'get' })
}

export function getWmTransaction(transactionId: number): Promise<AjaxResult<WmTransaction>> {
  return request({ url: '/mes/wm/transaction/' + transactionId, method: 'get' })
}

export function addWmTransaction(d: WmTransaction): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transaction', method: 'post', data: d })
}

export function updateWmTransaction(d: WmTransaction): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transaction', method: 'put', data: d })
}

export function delWmTransaction(transactionId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transaction/' + transactionId, method: 'delete' })
}