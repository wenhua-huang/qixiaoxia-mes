import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

/** 产品入库单（工单完工入库） */
export interface WmProductRecpt {
  recptId?: number
  recptCode?: string
  recptName?: string
  produceId?: number
  produceCode?: string
  workorderId?: number
  workorderCode?: string
  sourceWarehouseId?: number
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  recptDate?: string
  totalQuantity?: number
  totalBox?: number
  ipqcId?: number
  ipqcCode?: string
  status?: string
  remark?: string
  lines?: WmProductRecptLine[]
}

export interface WmProductRecptLine {
  lineId?: number
  recptId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  quantityRecpt?: number
  quantityBox?: number
  batchId?: number
  batchCode?: string
  warehouseId?: number
  locationId?: number
  areaId?: number
}

export interface WmProductRecptQueryParams {
  pageNum?: number
  pageSize?: number
  recptCode?: string
  recptName?: string
  workorderCode?: string
  produceCode?: string
  status?: string
}

export function listWmProductRecpt(q: WmProductRecptQueryParams): Promise<TableDataInfo<WmProductRecpt[]>> {
  return request({ url: '/mes/wm/product_recpt/list', method: 'get', params: q })
}

export function listAllWmProductRecpt(): Promise<AjaxResult<WmProductRecpt[]>> {
  return request({ url: '/mes/wm/product_recpt/listAll', method: 'get' })
}

export function getWmProductRecpt(recptId: number): Promise<AjaxResult<WmProductRecpt>> {
  return request({ url: '/mes/wm/product_recpt/' + recptId, method: 'get' })
}

export function addWmProductRecpt(d: WmProductRecpt): Promise<AjaxResult<WmProductRecpt>> {
  return request({ url: '/mes/wm/product_recpt', method: 'post', data: d })
}

export function updateWmProductRecpt(d: WmProductRecpt): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_recpt', method: 'put', data: d })
}

export function delWmProductRecpt(recptId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/product_recpt/' + recptId, method: 'delete' })
}
