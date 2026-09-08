import request from '@/utils/request'
import type { GanttData, WorkstationLane } from '@/types/api/mes/pro/gantt'

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

/** 机台泳道视图：每个启用工作站一行 + 行内任务条；含「待指派」「外协」虚拟行 */
export function getWorkstationView(startDate?: string, endDate?: string) {
  return request<{ code: number; data: { rows: WorkstationLane[] }; msg: string }>({
    url: '/mes/pro/gantt/workstationView',
    method: 'get',
    params: { startDate, endDate }
  })
}

/** 查询某工序可用工作站：按工序类型(process_type)过滤，并标记给定时段是否空闲 */
export function getAvailableWorkstations(params: {
  processId: number
  processType?: string
  startTime?: string
  endTime?: string
  excludeTaskId?: number
}) {
  return request<{ code: number; data: any[]; msg: string }>({
    url: '/mes/pro/gantt/availableWorkstations',
    method: 'get',
    params
  })
}
