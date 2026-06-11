import type { PageDomain, BaseEntity } from "../../common";

/** 编码生成规则分页查询参数 */
export interface AutoCodeRuleQueryParams extends PageDomain {
  /** 工厂ID */
  factoryId?: number;
  /** 规则编码 */
  ruleCode?: string;
  /** 规则名称 */
  ruleName?: string;
  /** 是否启用 */
  enableFlag?: string;
}

/** 编码生成规则信息 */
export interface SysAutoCodeRule extends BaseEntity {
  /** 规则ID */
  ruleId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 规则编码 */
  ruleCode?: string;
  /** 规则名称 */
  ruleName?: string;
  /** 规则描述 */
  ruleDesc?: string;
  /** 最大长度 */
  maxLength?: number;
  /** 是否补齐(1-是,0-否) */
  isPadded?: string;
  /** 补齐字符 */
  paddedChar?: string;
  /** 补齐方式(L-左补齐,R-右补齐) */
  paddedMethod?: string;
  /** 是否启用(1-启用,0-停用) */
  enableFlag?: string;
}
