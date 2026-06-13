import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmStockTakingQueryParams extends PageDomain {
  takingCode?: string
  planCode?: string
  itemCode?: string
  itemName?: string
  specification?: string
}

export interface WmStockTaking extends BaseEntity {
  takingId: number
  factoryId?: number
  takingCode?: string
  planId?: number
  planCode?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  batchId?: number
  batchCode?: string
  warehouseId?: number
  locationId?: number
  areaId?: number
  bookQuantity?: number
  actualQuantity?: number
  difference?: number
  diffReason?: string
  takingUser?: string
  takingDate?: string
  status?: string
}