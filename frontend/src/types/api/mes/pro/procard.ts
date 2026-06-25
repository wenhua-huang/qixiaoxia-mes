import { PageDomain, BaseEntity } from '@/types/api/common'

export interface ProProcardQueryParams extends PageDomain {
  cardCode?: string
  workorderName?: string
  itemName?: string
  status?: string
}

export interface ProProcard extends BaseEntity {
  cardId?: number
  cardCode?: string
  workorderId?: number
  workorderCode?: string
  workorderName?: string
  taskId?: number
  taskCode?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  batchCode?: string
  quantityTransfered?: number
  currentProcessId?: number
  currentProcessName?: string
  status?: string
}

export interface ProCardProcess extends BaseEntity {
  recordId?: number
  cardId?: number
  processId?: number
  processName?: string
  processCode?: string
  workstationId?: number
  workstationName?: string
  quantityQualified?: number
  quantityUnqualified?: number
  startTime?: string
  endTime?: string
  operatorName?: string
  remark?: string
}
