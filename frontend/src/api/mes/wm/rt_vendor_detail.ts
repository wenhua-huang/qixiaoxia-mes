import request from '@/utils/request'
import type { WmRtVendorDetailQueryParams, WmRtVendorDetail, AjaxResult, TableDataInfo } from '@/types'

export function listWmRtVendorDetail(q: WmRtVendorDetailQueryParams): Promise<TableDataInfo<WmRtVendorDetail[]>> {
  return request({ url: '/mes/wm/rt_vendor_detail/list', method: 'get', params: q })
}

export function listAllWmRtVendorDetail(): Promise<AjaxResult<WmRtVendorDetail[]>> {
  return request({ url: '/mes/wm/rt_vendor_detail/listAll', method: 'get' })
}

export function getWmRtVendorDetail(detailId: number): Promise<AjaxResult<WmRtVendorDetail>> {
  return request({ url: '/mes/wm/rt_vendor_detail/' + detailId, method: 'get' })
}

export function addWmRtVendorDetail(d: WmRtVendorDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor_detail', method: 'post', data: d })
}

export function updateWmRtVendorDetail(d: WmRtVendorDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor_detail', method: 'put', data: d })
}

export function delWmRtVendorDetail(detailId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor_detail/' + detailId, method: 'delete' })
}