import type { PageDomain, BaseEntity } from "../../common";

/** 编码生成记录分页查询参数 */
export interface AutoCodeResultQueryParams extends PageDomain {
  /** 规则ID */
  ruleId?: number;
}

/** 编码生成记录信息 */
export interface SysAutoCodeResult extends BaseEntity {
  /** 记录ID */
  codeId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 规则ID */
  ruleId?: number;
  /** 生成日期 */
  genDate?: string;
  /** 生成序号 */
  genIndex?: number;
  /** 最后完整编码值 */
  lastResult?: string;
  /** 最后流水号 */
  lastSerialNo?: number;
  /** 最后传入参数 */
  lastInputChar?: string;
}
