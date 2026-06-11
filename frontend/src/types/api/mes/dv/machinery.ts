import type { PageDomain, BaseEntity } from "../../common";

/** 设备台账分页查询参数 */
export interface MachineryQueryParams extends PageDomain {
  /** 设备编码 */
  machineryCode?: string;
  /** 设备名称 */
  machineryName?: string;
  /** 设备品牌 */
  machineryBrand?: string;
  /** 设备类型ID（含子树过滤） */
  machineryTypeId?: number;
  /** 所属车间ID */
  workshopId?: number;
  /** 设备状态 */
  status?: string;
}

/** 设备台账信息 */
export interface DvMachinery extends BaseEntity {
  /** 设备ID */
  machineryId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 设备编码 */
  machineryCode?: string;
  /** 设备名称 */
  machineryName?: string;
  /** 设备品牌 */
  machineryBrand?: string;
  /** 设备规格型号 */
  machinerySpec?: string;
  /** 设备类型ID */
  machineryTypeId?: number;
  /** 设备类型编码 */
  machineryTypeCode?: string;
  /** 设备类型名称 */
  machineryTypeName?: string;
  /** 所属车间ID */
  workshopId?: number;
  /** 所属车间编码 */
  workshopCode?: string;
  /** 所属车间名称 */
  workshopName?: string;
  /** 最后保养时间 */
  lastMaintenTime?: string;
  /** 最后点检时间 */
  lastCheckTime?: string;
  /** 设备状态 */
  status?: string;
  /** 是否启用(1-是,0-否) */
  enableFlag?: string;
}
