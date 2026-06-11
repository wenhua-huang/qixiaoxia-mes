import type { PageDomain, BaseEntity } from "../../common";

/** 附件分页查询参数 */
export interface AttachmentQueryParams extends PageDomain {
  /** 关联单据ID */
  sourceDocId?: number;
  /** 关联单据类型 */
  sourceDocType?: string;
  /** 原始文件名 */
  orignalName?: string;
  /** 文件类型 */
  fileType?: string;
}

/** 通用附件信息 */
export interface SysAttachment extends BaseEntity {
  /** 附件ID */
  attachmentId?: number;
  /** 工厂ID */
  factoryId?: number;
  /** 关联业务单据ID */
  sourceDocId?: number;
  /** 业务单据类型 */
  sourceDocType?: string;
  /** 文件访问URL */
  fileUrl?: string;
  /** 存储域名/Base路径 */
  basePath?: string;
  /** 存储文件名(UUID) */
  fileName?: string;
  /** 原始文件名 */
  orignalName?: string;
  /** 文件类型 */
  fileType?: string;
  /** 文件大小(KB) */
  fileSize?: number;
}
