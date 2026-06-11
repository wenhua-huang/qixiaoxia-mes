import request from '@/utils/request'
import type { MachineryTypeQueryParams, DvMachineryType, AjaxResult, TreeSelect } from '@/types'

// 查询设备类型列表（全量，不分页，树形表格用）
export function listMachinerytype(query: MachineryTypeQueryParams): Promise<AjaxResult<DvMachineryType[]>> {
  return request({
    url: '/mes/dv/machinerytype/list',
    method: 'get',
    params: query
  })
}

// 查询设备类型树（下拉用）
export function treeselect(): Promise<AjaxResult<TreeSelect[]>> {
  return request({
    url: '/mes/dv/machinerytype/treeselect',
    method: 'get'
  })
}

// 查询设备类型列表（排除自身及子孙节点）
export function listExcludeChild(machineryTypeId: number): Promise<AjaxResult<DvMachineryType[]>> {
  return request({
    url: '/mes/dv/machinerytype/list/exclude/' + machineryTypeId,
    method: 'get'
  })
}

// 查询设备类型详细
export function getMachinerytype(machineryTypeId: number): Promise<AjaxResult<DvMachineryType>> {
  return request({
    url: '/mes/dv/machinerytype/' + machineryTypeId,
    method: 'get'
  })
}

// 新增设备类型
export function addMachinerytype(data: DvMachineryType): Promise<AjaxResult> {
  return request({
    url: '/mes/dv/machinerytype',
    method: 'post',
    data: data
  })
}

// 修改设备类型
export function updateMachinerytype(data: DvMachineryType): Promise<AjaxResult> {
  return request({
    url: '/mes/dv/machinerytype',
    method: 'put',
    data: data
  })
}

// 删除设备类型
export function delMachinerytype(machineryTypeId: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mes/dv/machinerytype/' + machineryTypeId,
    method: 'delete'
  })
}
