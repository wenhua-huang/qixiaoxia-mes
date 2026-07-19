import request from '@/utils/request'
import type { WmRtVendorQueryParams, WmRtVendor, RtVendorFromPurOrderRequest, ReturnableBatch, AjaxResult, TableDataInfo } from '@/types'

export function listWmRtVendor(q: WmRtVendorQueryParams): Promise<TableDataInfo<WmRtVendor[]>> {
  return request({ url: '/mes/wm/rt_vendor/list', method: 'get', params: q })
}

export function listAllWmRtVendor(): Promise<AjaxResult<WmRtVendor[]>> {
  return request({ url: '/mes/wm/rt_vendor/listAll', method: 'get' })
}

export function getWmRtVendor(rtId: number): Promise<AjaxResult<WmRtVendor>> {
  return request({ url: '/mes/wm/rt_vendor/' + rtId, method: 'get' })
}

export function addWmRtVendor(d: WmRtVendor): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor', method: 'post', data: d })
}

export function updateWmRtVendor(d: WmRtVendor): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor', method: 'put', data: d })
}

export function delWmRtVendor(rtId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor/' + rtId, method: 'delete' })
}

// 确认退货单（DRAFT -> CONFIRMED），执行库存扣减
export function confirmRtVendor(rtId: number): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor/confirm/' + rtId, method: 'put' })
}

// 过账退货单（CONFIRMED -> POSTED），回写采购订单已退货数量
export function postRtVendor(rtId: number): Promise<AjaxResult> {
  return request({ url: '/mes/wm/rt_vendor/post/' + rtId, method: 'put' })
}

// 从采购订单生成退货单（DRAFT）：选 PO + 勾可退批次 + 填退货数量，一次性提交
export function createRtVendorFromPurOrder(d: RtVendorFromPurOrderRequest): Promise<AjaxResult<WmRtVendor>> {
  return request({ url: '/mes/wm/rt_vendor/fromPurOrder', method: 'post', data: d })
}

// 查询某采购订单的可退入库批次(向导数据源)
export function listReturnableBatches(purOrderId: number): Promise<AjaxResult<ReturnableBatch[]>> {
  return request({ url: '/mes/wm/rt_vendor/returnableBatches/' + purOrderId, method: 'get' })
}