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