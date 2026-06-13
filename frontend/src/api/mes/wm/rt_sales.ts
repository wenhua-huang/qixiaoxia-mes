import request from '@/utils/request'
import type { WmRtSalesQueryParams, WmRtSales, AjaxResult, TableDataInfo } from '@/types'

export function listWmRtSales(q: WmRtSalesQueryParams): Promise<TableDataInfo<WmRtSales[]>> {
  return request({ url: '/mes/wm/rt_sales/list', method: 'get', params: q })
}

export function listAllWmRtSales(): Promise<AjaxResult<WmRtSales[]>> {
  return request({ url: '/mes/wm/rt_sales/listAll', method: 'get' })
}

export function getWmRtSales(rtId: number): Promise<AjaxResult<WmRtSales>> {
  return request({ url: '/mes/wm/rt_sales/' + rtId, method: 'get' })
}

export function addWmRtSales(d: WmRtSales): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales', method: 'post', data: d })
}

export function updateWmRtSales(d: WmRtSales): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales', method: 'put', data: d })
}

export function delWmRtSales(rtId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales/' + rtId, method: 'delete' })
}