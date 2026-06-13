import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmRtVendorQueryParams extends PageDomain {
  rtCode?: string
  rtName?: string
  recptCode?: string
  vendorCode?: string
  vendorName?: string
}

export interface WmRtVendor extends BaseEntity {
  rtId: number
  factoryId?: number
  rtCode?: string
  rtName?: string
  recptId?: number
  recptCode?: string
  vendorId?: number
  vendorCode?: string
  vendorName?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  rtDate?: string
  totalQuantity?: number
  rqcId?: number
  rqcCode?: string
  status?: string
}