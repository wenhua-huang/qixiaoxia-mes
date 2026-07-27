import request from '@/utils/request'
import type { WmProductSalesBox, AjaxResult } from '@/types'

/** 装箱列表查询 */
export function listBox(q: Partial<WmProductSalesBox>): Promise<AjaxResult<WmProductSalesBox[]>> {
  return request({ url: '/mes/wm/product_sales_box/list', method: 'get', params: q })
}

/** 按出库单查全部装箱 */
export function listBoxBySales(salesId: number): Promise<AjaxResult<WmProductSalesBox[]>> {
  return request({ url: '/mes/wm/product_sales_box/bySales/' + salesId, method: 'get' })
}

/** 装箱详情 */
export function getBox(boxId: number): Promise<AjaxResult<WmProductSalesBox>> {
  return request({ url: '/mes/wm/product_sales_box/' + boxId, method: 'get' })
}

/** 新增装箱（自动算体积、自动箱号 BOX-NNN） */
export function addBox(d: WmProductSalesBox): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_box', method: 'post', data: d })
}

/** 修改装箱 */
export function updateBox(d: WmProductSalesBox): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_box', method: 'put', data: d })
}

/** 删除装箱（仅 PACKED 可删） */
export function delBox(boxId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_sales_box/' + boxId, method: 'delete' })
}
