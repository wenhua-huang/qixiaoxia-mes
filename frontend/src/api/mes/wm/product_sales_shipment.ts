import request from '@/utils/request'
import type { WmProductSalesShipment, WmProductSalesShipmentQueryParams, AjaxResult, TableDataInfo } from '@/types'

/** 发运单分页列表 */
export function listShipment(q: WmProductSalesShipmentQueryParams): Promise<TableDataInfo<WmProductSalesShipment[]>> {
  return request({ url: '/mes/wm/product_sales_shipment/list', method: 'get', params: q })
}

/** 按出库单查全部发运记录 */
export function listShipmentBySales(salesId: number): Promise<AjaxResult<WmProductSalesShipment[]>> {
  return request({ url: '/mes/wm/product_sales_shipment/bySales/' + salesId, method: 'get' })
}

/** 发运单详情（含关联箱） */
export function getShipment(shipmentId: number): Promise<AjaxResult<WmProductSalesShipment>> {
  return request({ url: '/mes/wm/product_sales_shipment/' + shipmentId, method: 'get' })
}

/** 新增发运（核心）：勾选箱、登记物流、回写头表发运汇总 */
export function addShipment(d: WmProductSalesShipment): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_shipment', method: 'post', data: d })
}

/** 修改发运（仅 SHIPPING/IN_TRANSIT 可改） */
export function updateShipment(d: WmProductSalesShipment): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_shipment', method: 'put', data: d })
}

/** 删除发运（回滚关联箱 + 头表发运汇总） */
export function delShipment(shipmentId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_shipment/' + shipmentId, method: 'delete' })
}

/** 签收：写签收时间/人/备注/回单附件 */
export function receiveShipment(shipmentId: number, info: Partial<WmProductSalesShipment>): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_shipment/receive/' + shipmentId, method: 'put', data: info })
}

/** 取消发运（仅 SHIPPING 可取消） */
export function cancelShipment(shipmentId: number): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_shipment/cancel/' + shipmentId, method: 'put' })
}
