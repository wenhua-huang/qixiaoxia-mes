import type { PageDomain, BaseEntity } from "../../common";

/** 单位管理分页查询参数 */
export interface UnitMeasureQueryParams extends PageDomain {
  /** 工厂ID */
  factoryId?: number;
  /** 单位编码 */
  unitCode?: string;
  /** 单位名称 */
  unitName?: string;
  /** 所属主单位编码 */
  primaryUnit?: string;
  /** 是否启用 */
  enableFlag?: string;
}

/** 单位管理信息 */
export interface MdUnitMeasure extends BaseEntity {
  /** 单位ID */
  unitId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 单位编码 */
  unitCode?: string;
  /** 单位名称 */
  unitName?: string;
  /** 所属主单位编码（为空表示本单位是主单位） */
  primaryUnit?: string;
  /** 与主单位的换算率 */
  conversionRate?: number;
  /** 是否启用(1-是,0-否) */
  enableFlag?: string;
}
