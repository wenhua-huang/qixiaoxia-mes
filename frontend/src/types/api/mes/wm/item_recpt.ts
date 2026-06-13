import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmItemRecptQueryParams extends PageDomain {
  recptCode?: string
  recptName?: string
  purOrderCode?: string
  vendorCode?: string
  vendorName?: string
}

export interface WmItemRecpt extends BaseEntity {
  recptId: number
  factoryId?: number
  recptCode?: string
  recptName?: string
  purOrderId?: number
  purOrderCode?: string
  vendorId?: number
  vendorCode?: string
  vendorName?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  recptDate?: string
  recptType?: string
  totalQuantity?: number
  iqcId?: number
  iqcCode?: string
  status?: string
}