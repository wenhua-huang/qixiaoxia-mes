import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmItemRecptDetailQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmItemRecptDetail extends BaseEntity {
  detailId: number
  factoryId?: number
  recptId?: number
  lineId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  unit2?: string
  unit2Name?: string
  quantity?: number
  quantity2?: number
  batchId?: number
  batchCode?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  locationId?: number
  areaId?: number
  materialStockId?: number
}