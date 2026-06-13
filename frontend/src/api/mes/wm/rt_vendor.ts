import request from '@/utils/request'
import type { WmRtVendorQueryParams, WmRtVendor, AjaxResult, TableDataInfo } from '@/types'

export function listWmRtVendor(q: WmRtVendorQueryParams): Promise<TableDataInfo<WmRtVendor[]>> {
  return request({ url: '/mes/wm/rt_vendor/list', method: 'get', params: q })
}

export function listAllWmRtVendor(): Promise<AjaxResult<WmRtVendor[]>> {
  return request({ url: '/mes/wm/rt_vendor/listAll', method: 'get' })
}

export function getWmRtVendor(rtId: number): Promise<AjaxResult<WmRtVendor>> {
  return request({ url: '/mes/wm/rt_vendor/' + rtId, method: 'get' })
}

export function addWmRtVendor(d: WmRtVendor): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor', method: 'post', data: d })
}

export function updateWmRtVendor(d: WmRtVendor): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor', method: 'put', data: d })
}

export function delWmRtVendor(rtId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor/' + rtId, method: 'delete' })
}