import { PageDomain, BaseEntity } from "@/types/api/common"

/** 发运单 */
export interface WmProductSalesShipment extends BaseEntity {
  shipmentId: number
  factoryId?: number
  salesId?: number
  shipmentCode?: string
  shipMethod?: string         // LOGISTICS/EXPRESS/PICKUP/SELF
  logisticsCompany?: string
  trackingNo?: string
  logisticsFee?: number
  vehicleNo?: string
  driverName?: string
  driverTel?: string
  receiverName?: string
  receiverTel?: string
  shippingAddress?: string
  planShipDate?: string
  actualShipDate?: string
  shippedQuantity?: number
  boxCount?: number
  status?: string             // SHIPPING/IN_TRANSIT/RECEIVED/CANCELED
  receivedTime?: string
  receivedBy?: string
  receivedRemark?: string
  attachmentUrl?: string
  boxes?: WmProductSalesBox[] // 详情聚合
}

export interface WmProductSalesShipmentQueryParams extends PageDomain {
  salesId?: number
  shipmentCode?: string
  shipMethod?: string
  status?: string
  trackingNo?: string
}

/** 装箱明细 */
export interface WmProductSalesBox extends BaseEntity {
  boxId: number
  factoryId?: number
  salesId?: number
  lineId?: number
  boxNo?: string
  itemId?: number
  itemCode?: string
  itemName?: string
  specification?: string
  quantity?: number
  unitOfMeasure?: string
  unitName?: string
  boxSpec?: string
  boxLength?: number
  boxWidth?: number
  boxHeight?: number
  volume?: number
  weight?: number
  shipmentId?: number
  status?: string             // PACKED/SHIPPED
}
