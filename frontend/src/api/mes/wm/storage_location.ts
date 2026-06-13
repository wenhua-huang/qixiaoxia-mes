import request from '@/utils/request'
import type { WmStorageLocationQueryParams, WmStorageLocation, AjaxResult, TableDataInfo } from '@/types'

export function listWmStorageLocation(q: WmStorageLocationQueryParams): Promise<TableDataInfo<WmStorageLocation[]>> {
  return request({ url: '/mes/wm/storage_location/list', method: 'get', params: q })
}

export function listAllWmStorageLocation(): Promise<AjaxResult<WmStorageLocation[]>> {
  return request({ url: '/mes/wm/storage_location/listAll', method: 'get' })
}

export function getWmStorageLocation(locationId: number): Promise<AjaxResult<WmStorageLocation>> {
  return request({ url: '/mes/wm/storage_location/' + locationId, method: 'get' })
}

export function addWmStorageLocation(d: WmStorageLocation): Promise<AjaxResult> {
  return request({ url: '/mes/wm/storage_location', method: 'post', data: d })
}

export function updateWmStorageLocation(d: WmStorageLocation): Promise<AjaxResult> {
  return request({ url: '/mes/wm/storage_location', method: 'put', data: d })
}

export function delWmStorageLocation(locationId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/storage_location/' + locationId, method: 'delete' })
}