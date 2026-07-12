import { PageDomain, BaseEntity } from '@/types/api/common'

export interface WorkrecordQueryParams extends PageDomain {
  userName?: string
  nickName?: string
  workstationId?: number
  taskId?: number
  status?: string
}

export interface Workrecord extends BaseEntity {
  recordId?: number
  userId?: number
  userName?: string
  nickName?: string
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  workorderId?: number
  workorderCode?: string
  taskId?: number
  taskCode?: string
  processName?: string
  clockInTime?: string
  clockOutTime?: string
  workDuration?: number
  status?: string  // ACTIVE-在岗 / CLOSED-已下工
  remark?: string
}
