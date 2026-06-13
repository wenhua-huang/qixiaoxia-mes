import request from '@/utils/request'
import type { WmProductSalesDetailQueryParams, WmProductSalesDetail, AjaxResult, TableDataInfo } from '@/types'

export function listWmProductSalesDetail(q: WmProductSalesDetailQueryParams): Promise<TableDataInfo<WmProductSalesDetail[]>> {
  return request({ url: '/mes/wm/product_sales_detail/list', method: 'get', params: q })
}

export function listAllWmProductSalesDetail(): Promise<AjaxResult<WmProductSalesDetail[]>> {
  return request({ url: '/mes/wm/product_sales_detail/listAll', method: 'get' })
}

export function getWmProductSalesDetail(detailId: number): Promise<AjaxResult<WmProductSalesDetail>> {
  return request({ url: '/mes/wm/product_sales_detail/' + detailId, method: 'get' })
}

export function addWmProductSalesDetail(d: WmProductSalesDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_detail', method: 'post', data: d })
}

export function updateWmProductSalesDetail(d: WmProductSalesDetail): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_detail', method: 'put', data: d })
}

export function delWmProductSalesDetail(detailId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_detail/' + detailId, method: 'delete' })
}