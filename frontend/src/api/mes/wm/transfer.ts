import request from '@/utils/request'
import type { WmTransferQueryParams, WmTransfer, AjaxResult, TableDataInfo } from '@/types'

export function listWmTransfer(q: WmTransferQueryParams): Promise<TableDataInfo<WmTransfer[]>> {
  return request({ url: '/mes/wm/transfer/list', method: 'get', params: q })
}

export function listAllWmTransfer(): Promise<AjaxResult<WmTransfer[]>> {
  return request({ url: '/mes/wm/transfer/listAll', method: 'get' })
}

export function getWmTransfer(transferId: number): Promise<AjaxResult<WmTransfer>> {
  return request({ url: '/mes/wm/transfer/' + transferId, method: 'get' })
}

export function addWmTransfer(d: WmTransfer): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer', method: 'post', data: d })
}

export function updateWmTransfer(d: WmTransfer): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer', method: 'put', data: d })
}

export function delWmTransfer(transferId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer/' + transferId, method: 'delete' })
}