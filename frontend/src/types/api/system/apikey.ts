import type { PageDomain, BaseEntity } from "../common";

/** API Key 列表查询参数 */
export interface ApiKeyQueryParams extends PageDomain {
  name?: string;
  enabled?: string;
}

/** API Key 凭证（列表/管理用，不含明文 key） */
export interface SysApiKey extends BaseEntity {
  id?: number;
  name?: string;
  factoryId?: number;
  enabled?: 'Y' | 'N';
  expiresAt?: string;
}

/** 生成凭证请求参数 */
export interface GenApiKeyParams {
  name: string;
  factoryId: number;
  expiresAt?: string;
  remark?: string;
}

/** 生成凭证返回（含明文 key，仅本次返回） */
export interface GenApiKeyResult {
  id: number;
  apiKey: string;
  name: string;
  factoryId: number;
  expiresAt?: string;
  msg: string;
}
