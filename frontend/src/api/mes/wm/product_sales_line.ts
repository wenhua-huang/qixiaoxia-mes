import request from '@/utils/request'
import type { WmProductSalesLineQueryParams, WmProductSalesLine, AjaxResult, TableDataInfo } from '@/types'

export function listWmProductSalesLine(q: WmProductSalesLineQueryParams): Promise<TableDataInfo<WmProductSalesLine[]>> {
  return request({ url: '/mes/wm/product_sales_line/list', method: 'get', params: q })
}

export function listAllWmProductSalesLine(): Promise<AjaxResult<WmProductSalesLine[]>> {
  return request({ url: '/mes/wm/product_sales_line/listAll', method: 'get' })
}

export function getWmProductSalesLine(lineId: number): Promise<AjaxResult<WmProductSalesLine>> {
  return request({ url: '/mes/wm/product_sales_line/' + lineId, method: 'get' })
}

export function addWmProductSalesLine(d: WmProductSalesLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_line', method: 'post', data: d })
}

export function updateWmProductSalesLine(d: WmProductSalesLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_line', method: 'put', data: d })
}

export function delWmProductSalesLine(lineId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_line/' + lineId, method: 'delete' })
}