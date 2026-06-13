import request from '@/utils/request'
import type { WmBarcodeQueryParams, WmBarcode, AjaxResult, TableDataInfo } from '@/types'

export function listWmBarcode(q: WmBarcodeQueryParams): Promise<TableDataInfo<WmBarcode[]>> {
  return request({ url: '/mes/wm/barcode/list', method: 'get', params: q })
}

export function listAllWmBarcode(): Promise<AjaxResult<WmBarcode[]>> {
  return request({ url: '/mes/wm/barcode/listAll', method: 'get' })
}

export function getWmBarcode(barcodeId: number): Promise<AjaxResult<WmBarcode>> {
  return request({ url: '/mes/wm/barcode/' + barcodeId, method: 'get' })
}

export function addWmBarcode(d: WmBarcode): Promise<AjaxResult> {
  return request({ url: '/mes/wm/barcode', method: 'post', data: d })
}

export function updateWmBarcode(d: WmBarcode): Promise<AjaxResult> {
  return request({ url: '/mes/wm/barcode', method: 'put', data: d })
}

export function delWmBarcode(barcodeId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/barcode/' + barcodeId, method: 'delete' })
}