import type { PageDomain, BaseEntity } from "../../common";

/** 车间管理分页查询参数 */
export interface WorkshopQueryParams extends PageDomain {
  /** 车间编码 */
  workshopCode?: string;
  /** 车间名称 */
  workshopName?: string;
  /** 是否启用 */
  enableFlag?: string;
}

/** 车间管理信息 */
export interface MdWorkshop extends BaseEntity {
  /** 车间ID */
  workshopId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 车间编码 */
  workshopCode?: string;
  /** 车间名称 */
  workshopName?: string;
  /** 车间位置/地址 */
  address?: string;
  /** 车间负责人 */
  manager?: string;
  /** 是否启用(1-是,0-否) */
  enableFlag?: string;
}
