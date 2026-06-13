import request from '@/utils/request'
import type { WmStockTakingPlanQueryParams, WmStockTakingPlan, AjaxResult, TableDataInfo } from '@/types'

export function listWmStockTakingPlan(q: WmStockTakingPlanQueryParams): Promise<TableDataInfo<WmStockTakingPlan[]>> {
  return request({ url: '/mes/wm/stock_taking_plan/list', method: 'get', params: q })
}

export function listAllWmStockTakingPlan(): Promise<AjaxResult<WmStockTakingPlan[]>> {
  return request({ url: '/mes/wm/stock_taking_plan/listAll', method: 'get' })
}

export function getWmStockTakingPlan(planId: number): Promise<AjaxResult<WmStockTakingPlan>> {
  return request({ url: '/mes/wm/stock_taking_plan/' + planId, method: 'get' })
}

export function addWmStockTakingPlan(d: WmStockTakingPlan): Promise<AjaxResult> {
  return request({ url: '/mes/wm/stock_taking_plan', method: 'post', data: d })
}

export function updateWmStockTakingPlan(d: WmStockTakingPlan): Promise<AjaxResult> {
  return request({ url: '/mes/wm/stock_taking_plan', method: 'put', data: d })
}

export function delWmStockTakingPlan(planId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/stock_taking_plan/' + planId, method: 'delete' })
}