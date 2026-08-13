package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.common.annotation.SkipFactoryId;
import com.ruoyi.system.domain.SysApiKey;

/**
 * 外部系统 API Key 凭证 数据层
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
public interface SysApiKeyMapper
{
    /**
     * 按 api_key_hash 查询凭证（校验入口，含 factory_id）
     * <p>跳过 FactoryIdInterceptor：凭证校验在 ApiKeyAuthenticationFilter 中无登录上下文，
     * 且为全局 hash 精确匹配，不应受工厂隔离。
     * @param apiKeyHash SHA-256 hex
     * @return 凭证（不存在返回 null）
     */
    @SkipFactoryId
    public SysApiKey selectByApiKeyHash(String apiKeyHash);

    /**
     * 查询凭证列表（受 FactoryIdInterceptor 自动按当前用户工厂隔离）
     * @param apiKey 查询条件（name 模糊）
     * @return 凭证列表（不含明文 key）
     */
    public List<SysApiKey> selectApiKeyList(SysApiKey apiKey);

    /**
     * 按 ID 查询
     */
    public SysApiKey selectApiKeyById(Long id);

    /**
     * 新增凭证
     * <p>跳过 FactoryIdInterceptor：凭证 factory_id 由 service 显式写入参数指定值，
     * 使管理员能为任意工厂签发凭证，而非被强制写入自身工厂。
     */
    @SkipFactoryId
    public int insertApiKey(SysApiKey apiKey);

    /**
     * 修改启用状态
     */
    public int updateEnabled(SysApiKey apiKey);

    /**
     * 按 ID 删除
     */
    public int deleteApiKeyById(Long id);
}
