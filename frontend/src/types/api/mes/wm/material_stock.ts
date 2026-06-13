import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmMaterialStockQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmMaterialStock extends BaseEntity {
  materialStockId: number
  factoryId?: number
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  unit2?: string
  unit2Name?: string
  conversionRate?: number
  quantityOnhand?: number
  quantityOnhand2?: number
  quantityAvailable?: number
  batchId?: number
  batchCode?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  locationId?: number
  locationCode?: string
  locationName?: string
  areaId?: number
  areaCode?: string
  areaName?: string
  vendorId?: number
  vendorCode?: string
  vendorName?: string
  workorderId?: number
  workorderCode?: string
  expireDate?: string
  lotNumber?: string
  qualityStatus?: string
  status?: string
  enableFlag?: string
}