import request from '@/utils/request'
import type { WmTransferLineQueryParams, WmTransferLine, AjaxResult, TableDataInfo } from '@/types'

export function listWmTransferLine(q: WmTransferLineQueryParams): Promise<TableDataInfo<WmTransferLine[]>> {
  return request({ url: '/mes/wm/transfer_line/list', method: 'get', params: q })
}

export function listAllWmTransferLine(): Promise<AjaxResult<WmTransferLine[]>> {
  return request({ url: '/mes/wm/transfer_line/listAll', method: 'get' })
}

export function getWmTransferLine(lineId: number): Promise<AjaxResult<WmTransferLine>> {
  return request({ url: '/mes/wm/transfer_line/' + lineId, method: 'get' })
}

export function addWmTransferLine(d: WmTransferLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer_line', method: 'post', data: d })
}

export function updateWmTransferLine(d: WmTransferLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer_line', method: 'put', data: d })
}

export function delWmTransferLine(lineId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer_line/' + lineId, method: 'delete' })
}