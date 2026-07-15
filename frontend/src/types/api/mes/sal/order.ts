import type { PageDomain, BaseEntity } from "../../common";

export interface SalOrderQueryParams extends PageDomain {
  orderCode?: string;
  orderName?: string;
  clientCode?: string;
  clientName?: string;
  clientOrderCode?: string;
  businessLine?: string;
  status?: string;
}

export interface SalOrder extends BaseEntity {
  orderId?: number;
  factoryId?: number;
  orderCode?: string;
  orderName?: string;
  orderType?: string;
  clientId?: number;
  clientCode?: string;
  clientName?: string;
  clientNick?: string;
  clientOrderCode?: string;
  salesperson?: string;
  businessLine?: string;
  sampleFlag?: string;
  orderDate?: string;
  requestDate?: string;
  totalAmount?: number;
  paymentMethod?: string;
  status?: string;
  lines?: SalOrderLine[];
}

export interface SalOrderLine extends BaseEntity {
  lineId?: number;
  factoryId?: number;
  orderId?: number;
  lineNo?: number;
  productId?: number;
  productCode?: string;
  productName?: string;
  productSpc?: string;
  unitOfMeasure?: string;
  unitName?: string;
  quantity?: number;
  unitPrice?: number;
  lineAmount?: number;
  spacing?: string;
  productSize?: string;
  printingReq?: string;
  ropeSpec?: string;
  packageReq?: string;
  shippingReq?: string;
  requestDate?: string;
  /** 已转工单数量(后端计算) */
  quantityProduced?: number;
  /** 可转数量(后端计算) */
  quantityConvertible?: number;
}

export interface SalOrderCreateRequest {
  order: SalOrder;
  lines: SalOrderLine[];
}

export interface SalOrderToWorkorderRequest {
  lineId: number;
  quantity: number;
  workorderCode: string;
  workorderName?: string;
  requestDate?: string;
  routeProductId?: number;
  createSkuVariant?: boolean;
  skuCode?: string;
  skuName?: string;
  bomList?: any[];
  paramList?: any[];
  remark?: string;
}
