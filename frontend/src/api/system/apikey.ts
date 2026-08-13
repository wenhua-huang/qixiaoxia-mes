import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'
import type { SysApiKey, ApiKeyQueryParams, GenApiKeyParams, GenApiKeyResult } from '@/types/api/system/apikey'

/** 查询 API Key 列表 */
export function listApiKey(query: ApiKeyQueryParams): Promise<TableDataInfo<SysApiKey[]>> {
  return request({ url: '/system/apikey/list', method: 'get', params: query })
}

/** 生成 API Key（明文 key 仅本次返回） */
export function genApiKey(params: GenApiKeyParams): Promise<AjaxResult<GenApiKeyResult>> {
  return request({ url: '/system/apikey/gen', method: 'post', params })
}

/** 启用/停用 API Key */
export function toggleApiKey(id: number, enabled: string): Promise<AjaxResult> {
  return request({ url: '/system/apikey/toggle/' + id, method: 'put', params: { enabled } })
}

/** 删除（吊销）API Key */
export function delApiKey(id: number): Promise<AjaxResult> {
  return request({ url: '/system/apikey/' + id, method: 'delete' })
}
