import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmProductSalesDetailQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmProductSalesDetail extends BaseEntity {
  detailId: number
  factoryId?: number
  salesId?: number
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
  warehouseId?: number
  locationId?: number
  areaId?: number
  materialStockId?: number
}