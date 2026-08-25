import request from '@/utils/request'

// 查询工单进度明细
export function getWorkorderProgress(workorderId: number | string) {
  return request({ url: '/mes/pro/progress/' + workorderId, method: 'get' })
}

// 工单进度详情内嵌的只读甘特图（复用工单查询权限，不触发自动排产）
export function getWorkorderGanttReadonly(workorderId: number | string) {
  return request({ url: '/mes/pro/progress/' + workorderId + '/gantt', method: 'get' })
}

// 延期/临期预警列表
export function listDelay(query: any) {
  return request({ url: '/mes/pro/progress/delayList', method: 'get', params: query })
}
