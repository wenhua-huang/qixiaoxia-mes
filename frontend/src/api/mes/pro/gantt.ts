import request from '@/utils/request'
import type { GanttData } from '@/types/api/mes/pro/gantt'

/** 获取单工单甘特图数据 */
export function getWorkOrderGantt(workorderId: number) {
  return request<{ code: number; data: GanttData; msg: string }>({
    url: '/mes/pro/gantt/workorder/' + workorderId,
    method: 'get'
  })
}

/** 获取工作站维度甘特图数据 */
export function getWorkstationGantt(workstationId: number, startDate?: string, endDate?: string) {
  return request<{ code: number; data: GanttData; msg: string }>({
    url: '/mes/pro/gantt/workstation/' + workstationId,
    method: 'get',
    params: { startDate, endDate }
  })
}
