import request from '@/utils/request'
import type { AutoCodePartQueryParams, SysAutoCodePart, AjaxResult, TableDataInfo } from '@/types'

// 查询编码分段列表
export function listAutoCodePart(query: AutoCodePartQueryParams): Promise<TableDataInfo<SysAutoCodePart[]>> {
  return request({ url: '/mes/sys/autocodepart/list', method: 'get', params: query })
}

// 按规则ID查询分段列表（不分页）
export function listAutoCodePartByRuleId(ruleId: number): Promise<AjaxResult<SysAutoCodePart[]>> {
  return request({ url: '/mes/sys/autocodepart/listByRuleId/' + ruleId, method: 'get' })
}

// 查询编码分段详细
export function getAutoCodePart(partId: number): Promise<AjaxResult<SysAutoCodePart>> {
  return request({ url: '/mes/sys/autocodepart/' + partId, method: 'get' })
}

// 新增编码分段
export function addAutoCodePart(data: SysAutoCodePart): Promise<AjaxResult> {
  return request({ url: '/mes/sys/autocodepart', method: 'post', data: data })
}

// 修改编码分段
export function updateAutoCodePart(data: SysAutoCodePart): Promise<AjaxResult> {
  return request({ url: '/mes/sys/autocodepart', method: 'put', data: data })
}

// 删除编码分段
export function delAutoCodePart(partId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/sys/autocodepart/' + partId, method: 'delete' })
}
