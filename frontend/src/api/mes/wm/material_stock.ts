import request from '@/utils/request'
import type { WmMaterialStockQueryParams, WmMaterialStock, AjaxResult, TableDataInfo } from '@/types'

export function listWmMaterialStock(q: WmMaterialStockQueryParams): Promise<TableDataInfo<WmMaterialStock[]>> {
  return request({ url: '/mes/wm/material_stock/list', method: 'get', params: q })
}

export function listAllWmMaterialStock(): Promise<AjaxResult<WmMaterialStock[]>> {
  return request({ url: '/mes/wm/material_stock/listAll', method: 'get' })
}

export function getWmMaterialStock(materialStockId: number): Promise<AjaxResult<WmMaterialStock>> {
  return request({ url: '/mes/wm/material_stock/' + materialStockId, method: 'get' })
}

export function addWmMaterialStock(d: WmMaterialStock): Promise<AjaxResult> {
  return request({ url: '/mes/wm/material_stock', method: 'post', data: d })
}

export function updateWmMaterialStock(d: WmMaterialStock): Promise<AjaxResult> {
  return request({ url: '/mes/wm/material_stock', method: 'put', data: d })
}

export function delWmMaterialStock(materialStockId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/material_stock/' + materialStockId, method: 'delete' })
}

/**
 * 查可用批次列表（发料弹窗批次下拉用）：按 itemId 查所有 onhand>0 的批次记录。
 * 不传 warehouseId 时跨仓返回，前端可用 warehouseName 区分。
 * 返回字段含 materialStockId/batchId/batchCode/warehouseId/warehouseName/quantityAvailable/quantityOnhand 等。
 */
export function availableBatches(itemId: number, warehouseId?: number | null): Promise<AjaxResult<any[]>> {
  const params: Record<string, any> = { itemId }
  if (warehouseId != null) params.warehouseId = warehouseId
  return request({ url: '/mes/wm/material_stock/availableBatches', method: 'get', params })
}