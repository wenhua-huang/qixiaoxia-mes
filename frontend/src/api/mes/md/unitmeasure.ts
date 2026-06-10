import request from '@/utils/request'
import type { UnitMeasureQueryParams, MdUnitMeasure, AjaxResult, TableDataInfo } from '@/types'

// 查询单位列表
export function listUnitmeasure(query: UnitMeasureQueryParams): Promise<TableDataInfo<MdUnitMeasure[]>> {
  return request({
    url: '/mes/md/unitmeasure/list',
    method: 'get',
    params: query
  })
}

// 查询主单位列表
export function listPrimaryUnitmeasure(): Promise<AjaxResult<MdUnitMeasure[]>> {
  return request({
    url: '/mes/md/unitmeasure/listprimary',
    method: 'get'
  })
}

// 查询所有启用单位
export function listAllUnitmeasure(): Promise<AjaxResult<MdUnitMeasure[]>> {
  return request({
    url: '/mes/md/unitmeasure/selectall',
    method: 'get'
  })
}

// 查询单位详细
export function getUnitmeasure(unitId: number): Promise<AjaxResult<MdUnitMeasure>> {
  return request({
    url: '/mes/md/unitmeasure/' + unitId,
    method: 'get'
  })
}

// 新增单位
export function addUnitmeasure(data: MdUnitMeasure): Promise<AjaxResult> {
  return request({
    url: '/mes/md/unitmeasure',
    method: 'post',
    data: data
  })
}

// 修改单位
export function updateUnitmeasure(data: MdUnitMeasure): Promise<AjaxResult> {
  return request({
    url: '/mes/md/unitmeasure',
    method: 'put',
    data: data
  })
}

// 删除单位
export function delUnitmeasure(unitId: number | number[]): Promise<AjaxResult> {
  return request({
    url: '/mes/md/unitmeasure/' + unitId,
    method: 'delete'
  })
}
