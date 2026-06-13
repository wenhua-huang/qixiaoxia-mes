import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmItemRecptLineQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmItemRecptLine extends BaseEntity {
  lineId: number
  factoryId?: number
  recptId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  unit2?: string
  unit2Name?: string
  conversionRate?: number
  quantityRecpt?: number
  quantityRecpt2?: number
  batchId?: number
  batchCode?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  locationId?: number
  areaId?: number
  expireDate?: string
  noticeLineId?: number
}