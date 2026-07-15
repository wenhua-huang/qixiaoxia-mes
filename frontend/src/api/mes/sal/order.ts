import request from '@/utils/request'
import type { SalOrderQueryParams, SalOrder, SalOrderCreateRequest, SalOrderToWorkorderRequest, AjaxResult, TableDataInfo } from '@/types'

export function listOrder(q: SalOrderQueryParams): Promise<TableDataInfo<SalOrder[]>> {
  return request({ url: '/mes/sal/order/list', method: 'get', params: q })
}
export function listAllOrder(): Promise<AjaxResult<SalOrder[]>> {
  return request({ url: '/mes/sal/order/listAll', method: 'get' })
}
export function getOrder(id: number): Promise<AjaxResult<SalOrder>> {
  return request({ url: '/mes/sal/order/' + id, method: 'get' })
}
export function getOrderDetail(id: number): Promise<AjaxResult<SalOrder>> {
  return request({ url: '/mes/sal/order/detail/' + id, method: 'get' })
}
export function createOrderWithLines(data: SalOrderCreateRequest): Promise<AjaxResult<SalOrder>> {
  return request({ url: '/mes/sal/order/createWithLines', method: 'post', data })
}
export function updateOrderWithLines(data: SalOrderCreateRequest): Promise<AjaxResult<SalOrder>> {
  return request({ url: '/mes/sal/order/updateWithLines', method: 'put', data })
}
export function confirmOrder(id: number): Promise<AjaxResult> {
  return request({ url: '/mes/sal/order/confirm/' + id, method: 'put' })
}
export function closeOrder(id: number): Promise<AjaxResult> {
  return request({ url: '/mes/sal/order/close/' + id, method: 'put' })
}
export function cancelOrder(id: number): Promise<AjaxResult> {
  return request({ url: '/mes/sal/order/cancel/' + id, method: 'put' })
}
export function toWorkorder(data: SalOrderToWorkorderRequest): Promise<AjaxResult<SalOrder>> {
  return request({ url: '/mes/sal/order/toWorkorder', method: 'post', data })
}
export function delOrder(id: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/sal/order/' + id, method: 'delete' })
}
