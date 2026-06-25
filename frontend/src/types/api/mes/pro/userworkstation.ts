import { PageDomain, BaseEntity } from '@/types/api/common'

export interface UserWorkstationQueryParams extends PageDomain {
  userId?: number
  workstationId?: number
  enableFlag?: string
}

export interface UserWorkstation extends BaseEntity {
  recordId?: number
  userId?: number
  userName?: string
  nickName?: string
  workstationId?: number
  workstationName?: string
  enableFlag?: string
  operationTime?: string
  remark?: string
}
