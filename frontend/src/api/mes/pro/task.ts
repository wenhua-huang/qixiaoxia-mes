import request from '@/utils/request'

// 查询列表
export function listTask(query: any) {
  return request({ url: '/mes/pro/task/list', method: 'get', params: query })
}
// 查询详细
export function getTask(taskId: number) { return request({ url: '/mes/pro/task/' + taskId, method: 'get' }) }
// 新增
export function addTask(data: any) { return request({ url: '/mes/pro/task', method: 'post', data }) }
// 修改
export function updateTask(data: any) { return request({ url: '/mes/pro/task', method: 'put', data }) }
// 删除
export function delTask(ids: any) { return request({ url: '/mes/pro/task/' + ids, method: 'delete' }) }
// 下发：NORMAL/PREPARE → PRODUCING
export function dispatchTask(taskId: number) { return request({ url: '/mes/pro/task/dispatch/' + taskId, method: 'put' }) }
// 完成：PRODUCING → COMPLETED
export function completeTask(taskId: number) { return request({ url: '/mes/pro/task/complete/' + taskId, method: 'put' }) }
// 取消
export function cancelTask(taskId: number) { return request({ url: '/mes/pro/task/cancel/' + taskId, method: 'put' }) }
// 工序进度汇总
export function progressByWorkorder(workorderId: number) { return request({ url: '/mes/pro/task/progressByWorkorder/' + workorderId, method: 'get' }) }
// 跟单质检不合格放行（理由必填，需 mes:pro:task:release 权限）
export function releaseQcBlock(taskId: number, reason: string) {
  return request({ url: '/mes/pro/task/releaseQcBlock/' + taskId, method: 'put', params: { reason } })
}
// 批量查询任务质检锁态（≤100，key=任务ID字符串，value={blocked, reason}）
export function getQcBlockState(taskIds: number[]) {
  return request({ url: '/mes/pro/task/qcBlockState', method: 'post', data: taskIds })
}
