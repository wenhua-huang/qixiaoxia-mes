import type { PageDomain, BaseEntity } from "../../common";

/** 销售订单状态（四态主线 + CANCEL 链外） */
export type SalOrderStatus = 'CONFIRMED' | 'PRODUCING' | 'SHIPPED' | 'CLOSED' | 'CANCEL'

export interface SalOrderQueryParams extends PageDomain {
  orderCode?: string;
  orderName?: string;
  clientCode?: string;
  clientName?: string;
  clientOrderCode?: string;
  businessLine?: string;
  /** 订单类型：NEW=新单 REPEAT=返单 STOCK=备货订单（字典 mes_sal_order_type） */
  orderType?: string;
  status?: string;
  /** 多状态筛选（后端 List<String> 绑定，重复 statusList 参数） */
  statusList?: SalOrderStatus[];
  /** 是否在列表行内返回生产进度 progressPercent */
  includeProgress?: boolean;
  /** 订单来源：1=直接新增 2=CRM系统 */
  source?: number;
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
  /** 订单来源：1=直接新增 2=CRM系统 */
  source?: number;
  orderDate?: string;
  requestDate?: string;
  totalAmount?: number;
  paymentMethod?: string;
  status?: SalOrderStatus;
  /** 生产进度百分比（includeProgress=true 时后端返回，0-100） */
  progressPercent?: number;
  /** 已派生未取消工单数（includeProgress=true 时后端返回；>0 时改/删隐藏） */
  workorderCount?: number;
  /** @deprecated 审核流已废弃，历史数据 */
  approveBy?: string;
  /** @deprecated 审核流已废弃，历史数据 */
  approveTime?: string;
  /** @deprecated 审核流已废弃，历史数据 */
  approveRemark?: string;
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
  /** 扩展属性(扁平JSON {attrCode:value})，分类驱动的动态属性快照 */
  lineAttrs?: Record<string, any>;
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
