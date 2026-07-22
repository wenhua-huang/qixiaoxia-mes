import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmRtVendorLineQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmRtVendorLine extends BaseEntity {
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
  warehouseCode?: string
  warehouseName?: string
  locationId?: number
  areaId?: number
  locationName?: string
  purOrderLineId?: number
  purOrderLineNo?: string
  quantityOrdered?: number
}