import request from '@/utils/request'
import type { WmRtSalesLineQueryParams, WmRtSalesLine, AjaxResult, TableDataInfo } from '@/types'

export function listWmRtSalesLine(q: WmRtSalesLineQueryParams): Promise<TableDataInfo<WmRtSalesLine[]>> {
  return request({ url: '/mes/wm/rt_sales_line/list', method: 'get', params: q })
}

export function listAllWmRtSalesLine(): Promise<AjaxResult<WmRtSalesLine[]>> {
  return request({ url: '/mes/wm/rt_sales_line/listAll', method: 'get' })
}

export function getWmRtSalesLine(lineId: number): Promise<AjaxResult<WmRtSalesLine>> {
  return request({ url: '/mes/wm/rt_sales_line/' + lineId, method: 'get' })
}

export function addWmRtSalesLine(d: WmRtSalesLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales_line', method: 'post', data: d })
}

export function updateWmRtSalesLine(d: WmRtSalesLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales_line', method: 'put', data: d })
}

export function delWmRtSalesLine(lineId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_sales_line/' + lineId, method: 'delete' })
}