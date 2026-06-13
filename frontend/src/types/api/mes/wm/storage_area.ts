import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmStorageAreaQueryParams extends PageDomain {
  areaCode?: string
  areaName?: string
  locationCode?: string
  locationName?: string
  warehouseCode?: string
}

export interface WmStorageArea extends BaseEntity {
  areaId: number
  factoryId?: number
  areaCode?: string
  areaName?: string
  locationId?: number
  locationCode?: string
  locationName?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  maxVolume?: number
  maxWeight?: number
  enableFlag?: string
}