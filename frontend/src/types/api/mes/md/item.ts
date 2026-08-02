import type { PageDomain, BaseEntity } from "../../common";

/** 物料产品分页查询参数 */
export interface ItemQueryParams extends PageDomain {
  itemCode?: string;
  itemName?: string;
  itemTypeId?: number;
  specification?: string;
  enableFlag?: string;
  /** 产品类型筛选：spu=标准产品(parent_id=0) | variant=变体(parent_id>0) */
  parentIdFilter?: string;
}

/** 物料产品信息 */
export interface MdItem extends BaseEntity {
  itemId?: number;
  factoryId?: number;
  itemCode?: string;
  itemName?: string;
  specification?: string;
  unitOfMeasure?: string;
  unitName?: string;
  unit2?: string;
  unit2Name?: string;
  conversionRate?: number;
  itemTypeId?: number;
  itemTypeCode?: string;
  itemTypeName?: string;
  parentId?: number;
  /** 父产品编码（变体物料查询时后端带出，只读） */
  parentItemCode?: string;
  /** 父产品名称（变体物料查询时后端带出，只读） */
  parentItemName?: string;
  productSize?: string;
  packageSpec?: string;
  printingReq?: string;
  enableFlag?: string;
  safeStockFlag?: string;
  minStock?: number;
  maxStock?: number;
  alertStock?: number;
  highValue?: string;
  batchFlag?: string;
  /** 扩展属性(扁平JSON {attrCode:value})，分类驱动的动态属性 */
  extAttrs?: Record<string, any>;
}
