import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmBatchQueryParams extends PageDomain {
  batchCode?: string
  batchName?: string
  itemCode?: string
  itemName?: string
  specification?: string
}

export interface WmBatch extends BaseEntity {
  batchId: number
  factoryId?: number
  batchCode?: string
  batchName?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  produceDate?: string
  expireDate?: string
  recptDate?: string
  vendorId?: number
  vendorCode?: string
  vendorName?: string
  workorderId?: number
  workorderCode?: string
  qualityStatus?: string
  status?: string
}