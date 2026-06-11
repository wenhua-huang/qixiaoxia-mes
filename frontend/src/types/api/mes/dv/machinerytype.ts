import type { PageDomain, BaseEntity } from "../../common";

/** 设备类型分页查询参数 */
export interface MachineryTypeQueryParams extends PageDomain {
  /** 设备类型编码 */
  machineryTypeCode?: string;
  /** 设备类型名称 */
  machineryTypeName?: string;
  /** 是否启用 */
  enableFlag?: string;
}

/** 设备类型信息 */
export interface DvMachineryType extends BaseEntity {
  /** 设备类型ID */
  machineryTypeId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 设备类型编码 */
  machineryTypeCode?: string;
  /** 设备类型名称 */
  machineryTypeName?: string;
  /** 父类型ID(0=根节点) */
  parentTypeId?: number;
  /** 同级排序号 */
  orderNum?: number;
  /** 是否启用(1-是,0-否) */
  enableFlag?: string;
  /** 子类型列表（内存树结构） */
  children?: DvMachineryType[];
}
