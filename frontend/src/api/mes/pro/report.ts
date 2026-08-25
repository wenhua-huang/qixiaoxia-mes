import request from '@/utils/request'

// 生产统计 - 总览 KPI
export function getOverview(params: any) {
  return request({ url: '/mes/pro/report/overview', method: 'get', params })
}

// 生产统计 - 产能/效率分组
export function getProductivity(params: any) {
  return request({ url: '/mes/pro/report/productivity', method: 'get', params })
}

// 生产统计 - 每日趋势
export function getTrend(params: any) {
  return request({ url: '/mes/pro/report/trend', method: 'get', params })
}

// 生产统计 - 任务状态分布
export function getTaskStatus(params: any) {
  return request({ url: '/mes/pro/report/taskStatus', method: 'get', params })
}

// 生产统计 - 工单明细分页
export function getReportDetail(params: any) {
  return request({ url: '/mes/pro/report/detail', method: 'get', params })
}
