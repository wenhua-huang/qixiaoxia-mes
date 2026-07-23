import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmProductSalesLineQueryParams extends PageDomain {
  salesId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmProductSalesLine extends BaseEntity {
  lineId: number
  factoryId?: number
  salesId?: number
  salesOrderLineId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  quantitySales?: number
  quantityPosted?: number
  quantityBox?: number
  boxSpec?: string
  boxLength?: number
  boxWidth?: number
  boxHeight?: number
  volume?: number
  weight?: number
  batchId?: number
  batchCode?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  availableQty?: number
  locationId?: number
  areaId?: number
}
