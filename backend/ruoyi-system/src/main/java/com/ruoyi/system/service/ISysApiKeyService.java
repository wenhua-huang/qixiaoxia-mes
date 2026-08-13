package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysApiKey;

/**
 * 外部系统 API Key 凭证 服务层
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
public interface ISysApiKeyService
{
    /**
     * 生成新凭证（明文 key 仅本次返回，后续不可查）
     * @param name 凭证名称
     * @param factoryId 绑定工厂
     * @param expiresAt 过期时间（null=永不过期）
     * @param remark 备注
     * @return 含明文 apiKey 的凭证对象
     */
    SysApiKey createApiKey(String name, Long factoryId, java.util.Date expiresAt, String remark);

    /**
     * 校验明文 API Key（供 ApiKeyAuthenticationFilter 调用）
     * @param rawKey 调用方传入的明文 key
     * @return 命中且启用且未过期的凭证，否则 null
     */
    SysApiKey verify(String rawKey);

    /**
     * 查询凭证列表（不含明文 key）
     */
    List<SysApiKey> selectApiKeyList(SysApiKey query);

    /**
     * 按 ID 查询
     */
    SysApiKey selectApiKeyById(Long id);

    /**
     * 启用/停用
     */
    int toggleEnabled(Long id, String enabled);

    /**
     * 按 ID 删除
     */
    int deleteApiKeyById(Long id);
}
