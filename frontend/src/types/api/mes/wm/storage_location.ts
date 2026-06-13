import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmStorageLocationQueryParams extends PageDomain {
  locationCode?: string
  locationName?: string
  warehouseCode?: string
  warehouseName?: string
  enableFlag?: string
}

export interface WmStorageLocation extends BaseEntity {
  locationId: number
  factoryId?: number
  locationCode?: string
  locationName?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  area?: number
  enableFlag?: string
}