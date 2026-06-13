import request from '@/utils/request'
import type { WmTransferDetailQueryParams, WmTransferDetail, AjaxResult, TableDataInfo } from '@/types'

export function listWmTransferDetail(q: WmTransferDetailQueryParams): Promise<TableDataInfo<WmTransferDetail[]>> {
  return request({ url: '/mes/wm/transfer_detail/list', method: 'get', params: q })
}

export function listAllWmTransferDetail(): Promise<AjaxResult<WmTransferDetail[]>> {
  return request({ url: '/mes/wm/transfer_detail/listAll', method: 'get' })
}

export function getWmTransferDetail(detailId: number): Promise<AjaxResult<WmTransferDetail>> {
  return request({ url: '/mes/wm/transfer_detail/' + detailId, method: 'get' })
}

export function addWmTransferDetail(d: WmTransferDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer_detail', method: 'post', data: d })
}

export function updateWmTransferDetail(d: WmTransferDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer_detail', method: 'put', data: d })
}

export function delWmTransferDetail(detailId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/transfer_detail/' + detailId, method: 'delete' })
}