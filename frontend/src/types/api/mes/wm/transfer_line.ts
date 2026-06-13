import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmTransferLineQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmTransferLine extends BaseEntity {
  lineId: number
  factoryId?: number
  transferId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  quantityTransfer?: number
  batchId?: number
  batchCode?: string
  sourceLocationId?: number
  sourceAreaId?: number
  targetLocationId?: number
  targetAreaId?: number
}