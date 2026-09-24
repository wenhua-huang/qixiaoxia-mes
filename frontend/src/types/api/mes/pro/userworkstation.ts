import { PageDomain, BaseEntity } from '@/types/api/common'

export interface UserWorkstationQueryParams extends PageDomain {
  userKeyword?: string
  workstationKeyword?: string
  enableFlag?: string
}

export interface UserWorkstation extends BaseEntity {
  recordId?: number
  userId?: number
  userName?: string
  nickName?: string
  workstationId?: number
  workstationCode?: string
  workstationName?: string
  enableFlag?: string
  operationTime?: string
  remark?: string
}

export interface WorkstationOption {
  workstationId: number
  workstationCode?: string
  workstationName?: string
  enableFlag?: string
}

export interface UserWorkstationBatchParams {
  userIds: number[]
  workstationIds: number[]
  remark?: string
}

export interface UserWorkstationBatchResult {
  successCount: number
  reactivatedCount: number
  skipCount: number
  skips: string[]
}
