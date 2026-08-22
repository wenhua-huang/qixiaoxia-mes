import request from '@/utils/request'

// 查询工单进度明细
export function getWorkorderProgress(workorderId: number | string) {
  return request({ url: '/mes/pro/progress/' + workorderId, method: 'get' })
}

// 延期/临期预警列表
export function listDelay(query: any) {
  return request({ url: '/mes/pro/progress/delayList', method: 'get', params: query })
}
