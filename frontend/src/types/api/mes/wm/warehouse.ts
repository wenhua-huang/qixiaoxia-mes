import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmWarehouseQueryParams extends PageDomain {
  warehouseCode?: string
  warehouseName?: string
  warehouseType?: string
  address?: string
  charge?: string
}

export interface WmWarehouse extends BaseEntity {
  warehouseId: number
  factoryId?: number
  warehouseCode?: string
  warehouseName?: string
  warehouseType?: string
  address?: string
  area?: number
  charge?: string
  enableFlag?: string
}