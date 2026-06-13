import request from '@/utils/request'
import type { WmRtSalesDetailQueryParams, WmRtSalesDetail, AjaxResult, TableDataInfo } from '@/types'

export function listWmRtSalesDetail(q: WmRtSalesDetailQueryParams): Promise<TableDataInfo<WmRtSalesDetail[]>> {
  return request({ url: '/mes/wm/rt_sales_detail/list', method: 'get', params: q })
}

export function listAllWmRtSalesDetail(): Promise<AjaxResult<WmRtSalesDetail[]>> {
  return request({ url: '/mes/wm/rt_sales_detail/listAll', method: 'get' })
}

export function getWmRtSalesDetail(detailId: number): Promise<AjaxResult<WmRtSalesDetail>> {
  return request({ url: '/mes/wm/rt_sales_detail/' + detailId, method: 'get' })
}

export function addWmRtSalesDetail(d: WmRtSalesDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales_detail', method: 'post', data: d })
}

export function updateWmRtSalesDetail(d: WmRtSalesDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales_detail', method: 'put', data: d })
}

export function delWmRtSalesDetail(detailId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales_detail/' + detailId, method: 'delete' })
}