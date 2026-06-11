import type { PageDomain, BaseEntity } from "../../common";

/** 待办分页查询参数 */
export interface TodoListQueryParams extends PageDomain {
  /** 用户ID */
  userId?: number;
  /** 待办标题 */
  todoTitle?: string;
  /** 待办类型 */
  todoType?: string;
  /** 优先级 */
  priority?: string;
  /** 状态 */
  status?: string;
  /** 单据编码 */
  sourceDocCode?: string;
}

/** 通用待办事项信息 */
export interface SysTodoList extends BaseEntity {
  /** 待办ID */
  todoId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 待办人用户ID */
  userId?: number;
  /** 待办标题 */
  todoTitle?: string;
  /** 待办类型 */
  todoType?: string;
  /** 关联单据ID */
  sourceDocId?: number;
  /** 关联单据类型 */
  sourceDocType?: string;
  /** 关联单据编码 */
  sourceDocCode?: string;
  /** 优先级:URGENT-紧急,HIGH-高,NORMAL-普通,LOW-低 */
  priority?: string;
  /** 状态:PENDING-待处理,PROCESSING-处理中,COMPLETED-已完成,REJECTED-已驳回 */
  status?: string;
  /** 截止时间 */
  deadline?: string;
  /** 处理时间 */
  handleTime?: string;
  /** 处理结果/意见 */
  handleResult?: string;
}
