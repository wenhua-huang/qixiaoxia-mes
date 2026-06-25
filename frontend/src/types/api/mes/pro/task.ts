import { PageDomain, BaseEntity } from '@/types/api/common'

export interface ProTaskQueryParams extends PageDomain {
  taskCode?: string
  taskName?: string
  workorderName?: string
  workstationId?: number
  processName?: string
  status?: string
}

export interface ProTask extends BaseEntity {
  taskId?: number
  taskCode?: string
  taskName?: string
  workorderId?: number
  workorderCode?: string
  workorderName?: string
  workstationId?: number
  workstationName?: string
  processId?: number
  processName?: string
  processCode?: string
  quantity?: number
  quantityProduced?: number
  machineCode?: string
  setupDuration?: number
  unitDuration?: number
  duration?: number
  startTime?: string
  endTime?: string
  status?: string
}
