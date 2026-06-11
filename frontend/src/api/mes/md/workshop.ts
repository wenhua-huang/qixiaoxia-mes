import request from '@/utils/request'
import type { WorkshopQueryParams, MdWorkshop, AjaxResult, TableDataInfo } from '@/types'

// 查询车间列表
export function listWorkshop(query: WorkshopQueryParams): Promise<TableDataInfo<MdWorkshop[]>> {
  return request({
    url: '/mes/md/workshop/list',
    method: 'get',
    params: query
  })
}

// 查询所有启用车间（下拉用）
export function listAllWorkshop(): Promise<AjaxResult<MdWorkshop[]>> {
  return request({
    url: '/mes/md/workshop/listAll',
    method: 'get'
  })
}

// 查询车间详细
export function getWorkshop(workshopId: number): Promise<AjaxResult<MdWorkshop>> {
  return request({
    url: '/mes/md/workshop/' + workshopId,
    method: 'get'
  })
}

// 新增车间
export function addWorkshop(data: MdWorkshop): Promise<AjaxResult> {
  return request({
    url: '/mes/md/workshop',
    method: 'post',
    data: data
  })
}

// 修改车间
export function updateWorkshop(data: MdWorkshop): Promise<AjaxResult> {
  return request({
    url: '/mes/md/workshop',
    method: 'put',
    data: data
  })
}

// 删除车间
export function delWorkshop(workshopId: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mes/md/workshop/' + workshopId,
    method: 'delete'
  })
}
