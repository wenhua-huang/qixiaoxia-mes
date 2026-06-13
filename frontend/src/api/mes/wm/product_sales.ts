import request from '@/utils/request'
import type { WmProductSalesQueryParams, WmProductSales, AjaxResult, TableDataInfo } from '@/types'

export function listWmProductSales(q: WmProductSalesQueryParams): Promise<TableDataInfo<WmProductSales[]>> {
  return request({ url: '/mes/wm/product_sales/list', method: 'get', params: q })
}

export function listAllWmProductSales(): Promise<AjaxResult<WmProductSales[]>> {
  return request({ url: '/mes/wm/product_sales/listAll', method: 'get' })
}

export function getWmProductSales(salesId: number): Promise<AjaxResult<WmProductSales>> {
  return request({ url: '/mes/wm/product_sales/' + salesId, method: 'get' })
}

export function addWmProductSales(d: WmProductSales): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales', method: 'post', data: d })
}

export function updateWmProductSales(d: WmProductSales): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales', method: 'put', data: d })
}

export function delWmProductSales(salesId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales/' + salesId, method: 'delete' })
}