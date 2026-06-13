import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmProductSalesQueryParams extends PageDomain {
  salesCode?: string
  salesName?: string
  clientCode?: string
  clientName?: string
  clientOrderCode?: string
}

export interface WmProductSales extends BaseEntity {
  salesId: number
  factoryId?: number
  salesCode?: string
  salesName?: string
  clientId?: number
  clientCode?: string
  clientName?: string
  clientOrderCode?: string
  salesperson?: string
  warehouseId?: number
  warehouseCode?: string
  warehouseName?: string
  salesDate?: string
  totalQuantity?: number
  totalBox?: number
  totalVolume?: number
  totalWeight?: number
  logisticsCompany?: string
  trackingNo?: string
  logisticsFee?: number
  shippingAddress?: string
  receiverName?: string
  receiverTel?: string
  palletFlag?: string
  boxLabel?: string
  salesType?: string
  oqcId?: number
  oqcCode?: string
  status?: string
}