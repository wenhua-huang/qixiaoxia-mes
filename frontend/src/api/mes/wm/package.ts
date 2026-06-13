import request from '@/utils/request'
import type { WmPackageQueryParams, WmPackage, AjaxResult, TableDataInfo } from '@/types'

export function listWmPackage(q: WmPackageQueryParams): Promise<TableDataInfo<WmPackage[]>> {
  return request({ url: '/mes/wm/package/list', method: 'get', params: q })
}

export function listAllWmPackage(): Promise<AjaxResult<WmPackage[]>> {
  return request({ url: '/mes/wm/package/listAll', method: 'get' })
}

export function getWmPackage(packageId: number): Promise<AjaxResult<WmPackage>> {
  return request({ url: '/mes/wm/package/' + packageId, method: 'get' })
}

export function addWmPackage(d: WmPackage): Promise<AjaxResult> {
  return request({ url: '/mes/wm/package', method: 'post', data: d })
}

export function updateWmPackage(d: WmPackage): Promise<AjaxResult> {
  return request({ url: '/mes/wm/package', method: 'put', data: d })
}

export function delWmPackage(packageId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/package/' + packageId, method: 'delete' })
}