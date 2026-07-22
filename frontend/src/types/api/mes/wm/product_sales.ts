import { PageDomain, BaseEntity } from "@/types/api/common"
import type { WmProductSalesLine } from './product_sales_line'
import type { WmProductSalesDetail } from './product_sales_detail'

export interface WmProductSalesQueryParams extends PageDomain {
  salesCode?: string
  salesName?: string
  clientCode?: string
  clientName?: string
  clientOrderCode?: string
  status?: string
  salesType?: string
  beginSalesDate?: string
  endSalesDate?: string
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
  postedQuantity?: number
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
  salesOrderId?: number
  salesOrderCode?: string
  status?: string
  lines?: WmProductSalesLine[]
  details?: WmProductSalesDetail[]
}
