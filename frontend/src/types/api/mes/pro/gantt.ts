/** 甘特图任务节点 */
export interface GanttTask {
  id: string
  text: string
  type?: 'project' | 'task'
  start?: string | null
  end?: string | null
  duration?: number
  colorCode?: string
  processId?: number
  processName?: string
  workstationId?: number
  workstationName?: string
  workorderId?: number
  workorderName?: string
  itemName?: string
  predecessorId?: number
  status?: string
  quantity?: number
  quantityProduced?: number
  actualStartTime?: string | null
  actualEndTime?: string | null
  progressPercent?: number
  delayLevel?: 'NORMAL' | 'WARNING' | 'DELAY' | 'FINISHED_DELAY' | 'BEHIND'
  behindSchedule?: boolean
  children?: GanttTask[]
  materialStatus?: { status: 'ok' | 'shortage'; shortageNames: string }
}

/** 甘特图依赖连线 */
export interface GanttLink {
  id: string
  source: string
  target: string
  type: 'FS' | 'SS' | 'FF' | 'SF'
}

/** 甘特图完整数据 */
export interface GanttData {
  tasks: GanttTask[]
  links: GanttLink[]
  calendars?: { workingRanges: any[]; holidays: string[] }
}

/** 机台泳道行（机台视角）：workstationId 为 -1=待指派、-2=外协 */
export interface WorkstationLane {
  workstationId: number
  workstationName: string
  workstationCode: string
  tasks: GanttTask[]
}
