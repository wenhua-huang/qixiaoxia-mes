import type { PageDomain, BaseEntity } from "../../common";

export interface PurOrderQueryParams extends PageDomain {
  orderCode?: string;
  orderName?: string;
  vendorId?: number;
  vendorCode?: string;
  vendorName?: string;
  purchaseType?: string;
  status?: string;
  /** 状态多值筛选(查询专用):供选择器按业务语义默认筛选多个状态 */
  statusList?: string[];
}

export interface PurOrder extends BaseEntity {
  orderId: number;
  factoryId?: number;
  orderCode?: string;
  orderName?: string;
  vendorId?: number;
  vendorCode?: string;
  vendorName?: string;
  purchaseType?: string;
  orderDate?: string;
  expectedDate?: string;
  purchaser?: string;
  approver?: string;
  totalQuantity?: number;
  receivedQuantity?: number;
  totalAmount?: number;
  currency?: string;
  sourceOrderCode?: string;
  status?: string;
  /** 订单明细行（详情接口返回） */
  lines?: PurOrderLine[];
}

/** 采购订单明细行 */
export interface PurOrderLine extends BaseEntity {
  lineId?: number;
  orderId?: number;
  itemCode?: string;
  itemName?: string;
  specification?: string;
  unitOfMeasure?: string;
  unitName?: string;
  quantityOrdered?: number;
  quantityReceived?: number;
  unitPrice?: number;
  amount?: number;
  taxRate?: number;
  expectedDate?: string;
  status?: string;
}
