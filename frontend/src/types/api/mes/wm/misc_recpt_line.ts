import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmMiscRecptLineQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmMiscRecptLine extends BaseEntity {
  lineId: number
  factoryId?: number
  recptId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  quantity?: number
  batchId?: number
  batchCode?: string
  warehouseId?: number
  locationId?: number
  areaId?: number
}