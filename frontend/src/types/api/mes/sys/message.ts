import type { PageDomain, BaseEntity } from "../../common";

/** 系统消息分页查询参数 */
export interface MessageQueryParams extends PageDomain {
  /** 用户ID */
  userId?: number;
  /** 消息标题 */
  title?: string;
  /** 消息类型 */
  messageType?: string;
  /** 是否已读 */
  readFlag?: string;
}

/** 系统消息信息 */
export interface SysMessage extends BaseEntity {
  /** 消息ID */
  messageId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 接收用户ID */
  userId?: number;
  /** 消息标题 */
  title?: string;
  /** 消息内容 */
  content?: string;
  /** 消息类型:SYSTEM-系统消息,APPROVAL-审批通知,WARNING-预警通知,NOTICE-公告 */
  messageType?: string;
  /** 关联单据ID */
  sourceDocId?: number;
  /** 关联单据类型 */
  sourceDocType?: string;
  /** 是否已读(1-已读,0-未读) */
  readFlag?: string;
  /** 阅读时间 */
  readTime?: string;
}
