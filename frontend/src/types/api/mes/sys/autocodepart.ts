import type { PageDomain, BaseEntity } from "../../common";

/** 编码分段分页查询参数 */
export interface AutoCodePartQueryParams extends PageDomain {
  /** 规则ID */
  ruleId?: number;
  /** 分段类型 */
  partType?: string;
  /** 分段编码 */
  partCode?: string;
  /** 分段名称 */
  partName?: string;
}

/** 编码生成规则组成信息 */
export interface SysAutoCodePart extends BaseEntity {
  /** 分段ID */
  partId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 规则ID */
  ruleId?: number;
  /** 分段序号 */
  partIndex?: number;
  /** 分段类型:INPUTCHAR-输入字符,NOWDATE-当前日期,FIXCHAR-固定字符,SERIALNO-流水号 */
  partType?: string;
  /** 分段编码 */
  partCode?: string;
  /** 分段名称 */
  partName?: string;
  /** 分段长度 */
  partLength?: number;
  /** 日期时间格式 */
  dateFormat?: string;
  /** 输入字符 */
  inputCharacter?: string;
  /** 固定字符 */
  fixCharacter?: string;
  /** 流水号起始值 */
  seriaStartNo?: number;
  /** 流水号步长 */
  seriaStep?: number;
  /** 流水号当前值 */
  seriaNowNo?: number;
  /** 流水号是否循环(1-是,0-否) */
  cycleFlag?: string;
  /** 循环方式 */
  cycleMethod?: string;
}
