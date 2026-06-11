import type { PageDomain, BaseEntity, TreeSelect } from "../../common";

/** 物料产品分类分页查询参数 */
export interface ItemTypeQueryParams extends PageDomain {
  /** 分类编码 */
  itemTypeCode?: string;
  /** 分类名称 */
  itemTypeName?: string;
  /** 产品物料标识 */
  itemOrProduct?: string;
  /** 是否启用 */
  enableFlag?: string;
}

/** 物料产品分类信息 */
export interface MdItemType extends BaseEntity {
  /** 分类ID */
  itemTypeId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 分类编码 */
  itemTypeCode?: string;
  /** 分类名称 */
  itemTypeName?: string;
  /** 父类型ID(0=根节点) */
  parentTypeId?: number;
  /** 产品物料标识:RAW-原料,SEMI-半成品,FINISHED-成品,AUXILIARY-辅料,PACK-包材 */
  itemOrProduct?: string;
  /** 同级排序号 */
  orderNum?: number;
  /** 是否启用(1-是,0-否) */
  enableFlag?: string;
  /** 子分类列表（内存树结构） */
  children?: MdItemType[];
}
