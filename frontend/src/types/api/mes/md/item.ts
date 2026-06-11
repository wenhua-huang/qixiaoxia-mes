import type { PageDomain, BaseEntity } from "../../common";

/** 物料产品分页查询参数 */
export interface ItemQueryParams extends PageDomain {
  itemCode?: string;
  itemName?: string;
  itemTypeId?: number;
  specification?: string;
  enableFlag?: string;
}

/** 物料纸张属性 */
export interface MdItemAttrPaper {
  attrId?: number;
  factoryId?: number;
  itemId?: number;
  paperWidth?: string;
  paperWeight?: string;
  paperSource?: string;
  paperType?: string;
}

/** 物料纸袋属性 */
export interface MdItemAttrPaperBag {
  attrId?: number;
  factoryId?: number;
  itemId?: number;
  ropeSpec?: string;
  mouthType?: string;
  bottomType?: string;
}

/** 物料礼品盒属性 */
export interface MdItemAttrGiftBox {
  attrId?: number;
  factoryId?: number;
  itemId?: number;
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
  /** 行业子表关联（GET时填充） */
  attrPaper?: MdItemAttrPaper;
  attrPaperBag?: MdItemAttrPaperBag;
  attrGiftBox?: MdItemAttrGiftBox;
}
