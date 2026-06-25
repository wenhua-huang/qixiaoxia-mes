import { PageDomain, BaseEntity } from '@/types/api/common'

export interface WorkrecordQueryParams extends PageDomain {
  userName?: string
  workstationId?: number
  taskId?: number
  operationFlag?: string
}

export interface Workrecord extends BaseEntity {
  recordId?: number
  userId?: number
  userName?: string
  nickName?: string
  workstationId?: number
  workstationName?: string
  taskId?: number
  taskCode?: string
  operationFlag?: string
  operationTime?: string
  remark?: string
}
