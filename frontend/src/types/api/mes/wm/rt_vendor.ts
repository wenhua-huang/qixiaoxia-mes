import { PageDomain, BaseEntity } from "@/types/api/common"

export interface WmRtVendorQueryParams extends PageDomain {
  rtCode?: string
  rtName?: string
  recptCode?: string
  vendorCode?: string
  vendorName?: string
  status?: string
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
  purOrderId?: number
  purOrderCode?: string
  confirmTime?: string
  confirmBy?: string
  postTime?: string
  postBy?: string
}

/** 可退批次(按 item+warehouse+batch 聚合,向导数据源) */
export interface ReturnableBatch {
  itemId: number
  itemCode?: string
  itemName?: string
  specification?: string
  unitOfMeasure?: string
  unitName?: string
  warehouseId: number
  warehouseCode?: string
  warehouseName?: string
  batchId?: number | null
  batchCode?: string
  expireDate?: string
  recptId: number
  recptCode?: string
  purOrderLineId?: number | null
  quantityRecptTotal: number
  quantityReturned: number
  quantityReturnable: number
}

/** 从采购订单生成退货单 — 退货行(行级仓库/批次/物料引用 + 退货量) */
export interface RtVendorFromPurOrderLine {
  itemId: number
  warehouseId: number
  warehouseCode?: string
  warehouseName?: string
  batchId?: number | null
  batchCode?: string
  purOrderLineId?: number | null
  quantityRt: number
}

/** 从采购订单生成退货单请求 */
export interface RtVendorFromPurOrderRequest {
  purOrderId: number
  rtCode: string
  rtName?: string
  remark?: string
  lines: RtVendorFromPurOrderLine[]
}