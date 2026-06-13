import request from '@/utils/request'
import type { WmPackageLineQueryParams, WmPackageLine, AjaxResult, TableDataInfo } from '@/types'

export function listWmPackageLine(q: WmPackageLineQueryParams): Promise<TableDataInfo<WmPackageLine[]>> {
  return request({ url: '/mes/wm/package_line/list', method: 'get', params: q })
}

export function listAllWmPackageLine(): Promise<AjaxResult<WmPackageLine[]>> {
  return request({ url: '/mes/wm/package_line/listAll', method: 'get' })
}

export function getWmPackageLine(lineId: number): Promise<AjaxResult<WmPackageLine>> {
  return request({ url: '/mes/wm/package_line/' + lineId, method: 'get' })
}

export function addWmPackageLine(d: WmPackageLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/package_line', method: 'post', data: d })
}

export function updateWmPackageLine(d: WmPackageLine): Promise<AjaxResult> {
  return request({ url: '/mes/wm/package_line', method: 'put', data: d })
}

export function delWmPackageLine(lineId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/package_line/' + lineId, method: 'delete' })
}