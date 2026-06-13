import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmRtSalesLineQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmRtSalesLine extends BaseEntity {
  lineId: number
  factoryId?: number
  rtId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  quantityRt?: number
  batchId?: number
  batchCode?: string
  warehouseId?: number
  locationId?: number
  areaId?: number
}