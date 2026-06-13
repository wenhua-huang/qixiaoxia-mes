import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmTransactionQueryParams extends PageDomain {
  transactionType?: string
  sourceDocType?: string
  sourceDocCode?: string
  itemCode?: string
  itemName?: string
}

export interface WmTransaction extends BaseEntity {
  transactionId: number
  factoryId?: number
  transactionType?: string
  sourceDocType?: string
  sourceDocId?: number
  sourceDocCode?: string
  sourceLineId?: number
  materialStockId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  quantity?: number
  unit2?: string
  unit2Name?: string
  quantity2?: number
  batchId?: number
  batchCode?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  locationId?: number
  areaId?: number
  workorderId?: number
  workorderCode?: string
  vendorId?: number
  clientId?: number
  relatedTransactionId?: number
  transactionTime?: string
}