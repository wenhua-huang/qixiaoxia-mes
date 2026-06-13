import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmTransferDetailQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmTransferDetail extends BaseEntity {
  detailId: number
  factoryId?: number
  transferId?: number
  lineId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  quantity?: number
  batchId?: number
  batchCode?: string
  materialStockId?: number
}