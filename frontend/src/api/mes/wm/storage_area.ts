import request from '@/utils/request'
import type { WmStorageAreaQueryParams, WmStorageArea, AjaxResult, TableDataInfo } from '@/types'

export function listWmStorageArea(q: WmStorageAreaQueryParams): Promise<TableDataInfo<WmStorageArea[]>> {
  return request({ url: '/mes/wm/storage_area/list', method: 'get', params: q })
}

export function listAllWmStorageArea(): Promise<AjaxResult<WmStorageArea[]>> {
  return request({ url: '/mes/wm/storage_area/listAll', method: 'get' })
}

export function getWmStorageArea(areaId: number): Promise<AjaxResult<WmStorageArea>> {
  return request({ url: '/mes/wm/storage_area/' + areaId, method: 'get' })
}

export function addWmStorageArea(d: WmStorageArea): Promise<AjaxResult> {
  return request({ url: '/mes/wm/storage_area', method: 'post', data: d })
}

export function updateWmStorageArea(d: WmStorageArea): Promise<AjaxResult> {
  return request({ url: '/mes/wm/storage_area', method: 'put', data: d })
}

export function delWmStorageArea(areaId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/wm/storage_area/' + areaId, method: 'delete' })
}