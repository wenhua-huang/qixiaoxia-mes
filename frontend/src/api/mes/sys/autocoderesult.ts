import request from '@/utils/request'
import type { AutoCodeResultQueryParams, SysAutoCodeResult, AjaxResult, TableDataInfo } from '@/types'

// 查询编码生成记录列表
export function listAutoCodeResult(query: AutoCodeResultQueryParams): Promise<TableDataInfo<SysAutoCodeResult[]>> {
  return request({ url: '/mes/sys/autocoderesult/list', method: 'get', params: query })
}

// 查询编码生成记录详细
export function getAutoCodeResult(codeId: number): Promise<AjaxResult<SysAutoCodeResult>> {
  return request({ url: '/mes/sys/autocoderesult/' + codeId, method: 'get' })
}

// 删除编码生成记录
export function delAutoCodeResult(codeId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/sys/autocoderesult/' + codeId, method: 'delete' })
}
