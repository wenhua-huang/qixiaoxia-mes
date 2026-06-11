import request from '@/utils/request'
import type { AutoCodeRuleQueryParams, SysAutoCodeRule, AjaxResult, TableDataInfo } from '@/types'

// 查询编码规则列表
export function listAutoCodeRule(query: AutoCodeRuleQueryParams): Promise<TableDataInfo<SysAutoCodeRule[]>> {
  return request({ url: '/mes/sys/autocoderule/list', method: 'get', params: query })
}

// 查询编码规则详细
export function getAutoCodeRule(ruleId: number): Promise<AjaxResult<SysAutoCodeRule>> {
  return request({ url: '/mes/sys/autocoderule/' + ruleId, method: 'get' })
}

// 新增编码规则
export function addAutoCodeRule(data: SysAutoCodeRule): Promise<AjaxResult> {
  return request({ url: '/mes/sys/autocoderule', method: 'post', data: data })
}

// 修改编码规则
export function updateAutoCodeRule(data: SysAutoCodeRule): Promise<AjaxResult> {
  return request({ url: '/mes/sys/autocoderule', method: 'put', data: data })
}

// 删除编码规则
export function delAutoCodeRule(ruleId: number | number[]): Promise<AjaxResult> {
  return request({ url: '/mes/sys/autocoderule/' + ruleId, method: 'delete' })
}

// 校验规则编码唯一性
export function checkRuleCodeUnique(data: SysAutoCodeRule): Promise<AjaxResult<boolean>> {
  return request({ url: '/mes/sys/autocoderule/checkRuleCodeUnique', method: 'get', params: data })
}

// 生成流水编码 (POST — 非幂等操作)
export function genSerialCode(ruleCode: string, inputCharacter?: string): Promise<AjaxResult<string>> {
  return request({
    url: '/mes/sys/autocoderule/genSerialCode/' + ruleCode,
    method: 'post',
    params: { inputCharacter }
  })
}
