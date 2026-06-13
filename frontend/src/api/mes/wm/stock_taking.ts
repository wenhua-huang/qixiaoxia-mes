import request from '@/utils/request'
import type { WmStockTakingQueryParams, WmStockTaking, AjaxResult, TableDataInfo } from '@/types'

export function listWmStockTaking(q: WmStockTakingQueryParams): Promise<TableDataInfo<WmStockTaking[]>> {
  return request({ url: '/mes/wm/stock_taking/list', method: 'get', params: q })
}

export function listAllWmStockTaking(): Promise<AjaxResult<WmStockTaking[]>> {
  return request({ url: '/mes/wm/stock_taking/listAll', method: 'get' })
}

export function getWmStockTaking(takingId: number): Promise<AjaxResult<WmStockTaking>> {
  return request({ url: '/mes/wm/stock_taking/' + takingId, method: 'get' })
}

export function addWmStockTaking(d: WmStockTaking): Promise<AjaxResult> {
  return request({ url: '/mes/wm/stock_taking', method: 'post', data: d })
}

export function updateWmStockTaking(d: WmStockTaking): Promise<AjaxResult> {
  return request({ url: '/mes/wm/stock_taking', method: 'put', data: d })
}

export function delWmStockTaking(takingId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/stock_taking/' + takingId, method: 'delete' })
}