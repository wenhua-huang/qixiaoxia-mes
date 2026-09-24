import request from '@/utils/request'

// 查询异常台账
export function listException(query: any) {
  return request({ url: '/mes/pro/exception/list', method: 'get', params: query })
}
// 查询异常单详情
export function getException(exceptionId: number) {
  return request({ url: '/mes/pro/exception/' + exceptionId, method: 'get' })
}
// 上报异常（手机端 E6 / PC 工序任务页 E1）
export function addException(data: any) {
  return request({ url: '/mes/pro/exception', method: 'post', data })
}
// PC 补全/定责（E1/E2）
export function updateException(data: any) {
  return request({ url: '/mes/pro/exception', method: 'put', data })
}
// 选出口处理（E3 七选一，返回处理后的最新异常单）
export function resolveException(exceptionId: number, data: any) {
  return request({ url: '/mes/pro/exception/resolve/' + exceptionId, method: 'put', data })
}
// 回流类手动收口关闭（处理结论必填）
export function closeException(exceptionId: number, conclusion: string) {
  return request({ url: '/mes/pro/exception/close/' + exceptionId, method: 'put', data: { conclusion } })
}
// 作废（E1：挂错对象作废重开，仅待处理可作废，原因必填）
export function voidException(exceptionId: number, conclusion: string) {
  return request({ url: '/mes/pro/exception/void/' + exceptionId, method: 'put', data: { conclusion } })
}
// 手机上报页只读关联对象
export function exceptionReportContext(taskId: number) {
  return request({ url: '/mes/pro/exception/reportContext/' + taskId, method: 'get' })
}
// 工单未关闭异常明细（工单详情警示区）
export function openExceptionByWorkorder(workorderId: number) {
  return request({ url: '/mes/pro/exception/openByWorkorder/' + workorderId, method: 'get' })
}
// 工单列表未关闭异常角标批量查询（≤100）
export function openExceptionState(workorderIds: number[]) {
  return request({ url: '/mes/pro/exception/openState', method: 'post', data: workorderIds })
}
