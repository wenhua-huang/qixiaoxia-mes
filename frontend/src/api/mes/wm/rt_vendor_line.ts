import request from '@/utils/request'
import type { WmRtVendorLineQueryParams, WmRtVendorLine, AjaxResult, TableDataInfo } from '@/types'

export function listWmRtVendorLine(q: WmRtVendorLineQueryParams): Promise<TableDataInfo<WmRtVendorLine[]>> {
  return request({ url: '/mes/wm/rt_vendor_line/list', method: 'get', params: q })
}

export function listAllWmRtVendorLine(): Promise<AjaxResult<WmRtVendorLine[]>> {
  return request({ url: '/mes/wm/rt_vendor_line/listAll', method: 'get' })
}

export function getWmRtVendorLine(lineId: number): Promise<AjaxResult<WmRtVendorLine>> {
  return request({ url: '/mes/wm/rt_vendor_line/' + lineId, method: 'get' })
}

export function addWmRtVendorLine(d: WmRtVendorLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor_line', method: 'post', data: d })
}

export function updateWmRtVendorLine(d: WmRtVendorLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor_line', method: 'put', data: d })
}

export function delWmRtVendorLine(lineId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor_line/' + lineId, method: 'delete' })
}