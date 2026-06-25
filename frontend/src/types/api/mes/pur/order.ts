import type { PageDomain, BaseEntity } from "../../common";

export interface PurOrderQueryParams extends PageDomain {
  orderCode?: string;
  orderName?: string;
  vendorId?: number;
  vendorCode?: string;
  vendorName?: string;
  purchaseType?: string;
  status?: string;
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
  totalAmount?: number;
  currency?: string;
  sourceOrderCode?: string;
  status?: string;
}
