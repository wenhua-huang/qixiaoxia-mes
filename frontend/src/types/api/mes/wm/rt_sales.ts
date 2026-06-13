import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmRtSalesQueryParams extends PageDomain {
  rtCode?: string
  rtName?: string
  salesCode?: string
  clientCode?: string
  clientName?: string
}

export interface WmRtSales extends BaseEntity {
  rtId: number
  factoryId?: number
  rtCode?: string
  rtName?: string
  salesId?: number
  salesCode?: string
  clientId?: number
  clientCode?: string
  clientName?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  rtDate?: string
  totalQuantity?: number
  rqcId?: number
  rqcCode?: string
  status?: string
}