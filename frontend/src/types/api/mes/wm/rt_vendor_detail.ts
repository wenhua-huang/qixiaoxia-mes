import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmRtVendorDetailQueryParams extends PageDomain {
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
}

export interface WmRtVendorDetail extends BaseEntity {
  detailId: number
  factoryId?: number
  rtId?: number
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