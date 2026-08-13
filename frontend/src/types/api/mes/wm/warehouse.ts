import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmWarehouseQueryParams extends PageDomain {
  warehouseCode?: string
  warehouseName?: string
  warehouseType?: string
  ownershipType?: string
  address?: string
  charge?: string
}

export interface WmWarehouse extends BaseEntity {
  warehouseId: number
  factoryId?: number
  warehouseCode?: string
  warehouseName?: string
  /** 内容类型: RAW/FINISHED/AUX/LINE/TEMP */
  warehouseType?: string
  /** 归属类型: PUBLIC/CUSTOMER/SUPPLIER */
  ownershipType?: string
  /** 客户仓归属客户ID（ownershipType=CUSTOMER 时必填） */
  clientId?: number
  /** 供应商仓归属供应商ID（ownershipType=SUPPLIER 时必填） */
  vendorId?: number
  /** 归属客户名称（列表/编辑回显用，后端按需携带） */
  clientName?: string
  /** 归属供应商名称（列表/编辑回显用，后端按需携带） */
  vendorName?: string
  address?: string
  area?: number
  charge?: string
  enableFlag?: string
}