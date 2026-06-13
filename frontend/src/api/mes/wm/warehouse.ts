import request from '@/utils/request'
import type { WmWarehouseQueryParams, WmWarehouse, AjaxResult, TableDataInfo } from '@/types'

export function listWmWarehouse(q: WmWarehouseQueryParams): Promise<TableDataInfo<WmWarehouse[]>> {
  return request({ url: '/mes/wm/warehouse/list', method: 'get', params: q })
}

export function listAllWmWarehouse(): Promise<AjaxResult<WmWarehouse[]>> {
  return request({ url: '/mes/wm/warehouse/listAll', method: 'get' })
}

export function getWmWarehouse(warehouseId: number): Promise<AjaxResult<WmWarehouse>> {
  return request({ url: '/mes/wm/warehouse/' + warehouseId, method: 'get' })
}

export function addWmWarehouse(d: WmWarehouse): Promise<AjaxResult> {
  return request({ url: '/mes/wm/warehouse', method: 'post', data: d })
}

export function updateWmWarehouse(d: WmWarehouse): Promise<AjaxResult> {
  return request({ url: '/mes/wm/warehouse', method: 'put', data: d })
}

export function delWmWarehouse(warehouseId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/warehouse/' + warehouseId, method: 'delete' })
}