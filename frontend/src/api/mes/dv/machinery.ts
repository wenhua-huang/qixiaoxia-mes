import request from '@/utils/request'
import type { MachineryQueryParams, DvMachinery, AjaxResult, TableDataInfo } from '@/types'

// 查询设备台账列表（分页）
export function listMachinery(query: MachineryQueryParams): Promise<TableDataInfo<DvMachinery[]>> {
  return request({
    url: '/mes/dv/machinery/list',
    method: 'get',
    params: query
  })
}

// 查询设备台账详细
export function getMachinery(machineryId: number): Promise<AjaxResult<DvMachinery>> {
  return request({
    url: '/mes/dv/machinery/' + machineryId,
    method: 'get'
  })
}

// 新增设备台账
export function addMachinery(data: DvMachinery): Promise<AjaxResult> {
  return request({
    url: '/mes/dv/machinery',
    method: 'post',
    data: data
  })
}

// 修改设备台账
export function updateMachinery(data: DvMachinery): Promise<AjaxResult> {
  return request({
    url: '/mes/dv/machinery',
    method: 'put',
    data: data
  })
}

// 删除设备台账
export function delMachinery(machineryId: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mes/dv/machinery/' + machineryId,
    method: 'delete'
  })
}
