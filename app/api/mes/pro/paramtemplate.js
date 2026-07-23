import request from '@/utils/request'

// 按工序ID查询参数模板（报工时加载报工可见的参数）
export function listParamTemplateByProcessId(processId) {
  return request({ url: '/mes/pro/paramtemplate/listByProcessId/' + processId, method: 'get' })
}
