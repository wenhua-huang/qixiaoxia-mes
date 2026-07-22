import request from '@/utils/request'
import type { WmProductSalesQueryParams, WmProductSales, WmProductSalesDetail, AjaxResult, TableDataInfo } from '@/types'

export function listWmProductSales(q: WmProductSalesQueryParams): Promise<TableDataInfo<WmProductSales[]>> {
  return request({ url: '/mes/wm/product_sales/list', method: 'get', params: q })
}

export function listAllWmProductSales(): Promise<AjaxResult<WmProductSales[]>> {
  return request({ url: '/mes/wm/product_sales/listAll', method: 'get' })
}

export function getWmProductSales(salesId: number): Promise<AjaxResult<WmProductSales>> {
  return request({ url: '/mes/wm/product_sales/' + salesId, method: 'get' })
}

/** 详情：聚合头+行+明细 */
export function getSalesDetail(salesId: number): Promise<AjaxResult<WmProductSales>> {
  return request({ url: '/mes/wm/product_sales/detail/' + salesId, method: 'get' })
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

// ════════════════════ 业务生命周期 ════════════════════

/** 过账出库 */
export function postOut(salesId: number, details: WmProductSalesDetail[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales/post/' + salesId, method: 'put', data: details })
}

/** 发货 */
export function shipOut(salesId: number, info: Partial<WmProductSales>): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales/ship/' + salesId, method: 'put', data: info })
}

/** 关闭 */
export function closeSales(salesId: number): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales/close/' + salesId, method: 'put' })
}

/** 作废 */
export function cancelSales(salesId: number): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales/cancel/' + salesId, method: 'put' })
}

/** 从销售订单生成出库单草稿 */
export function buildFromSaleOrder(orderId: number): Promise<AjaxResult<WmProductSales>> {
  return request({ url: '/mes/wm/product_sales/fromSaleOrder/' + orderId, method: 'get' })
}
