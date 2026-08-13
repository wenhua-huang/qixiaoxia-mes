package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 外部系统 API Key 凭证 qxx_sys_api_key
 *
 * 用途：CRM 等外部系统调用 /open-api/** 的接入凭证。
 * 每个 Key 绑定一个 factory_id，推单工厂归属由此决定。
 * api_key 明文只在生成时返回一次，后续查询不暴露（@JsonIgnore）。
 *
 * @author qixiaoxia
 * @date 2026-08-05
 */
public class SysApiKey extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** API Key 明文（仅在生成时返回一次给调用方；查询接口不暴露） */
    @JsonIgnore
    private String apiKey;

    /** API Key 的 SHA-256 hex（校验匹配用） */
    @JsonIgnore
    private String apiKeyHash;

    /** 凭证名称/用途说明 */
    @Excel(name = "凭证名称")
    private String name;

    /** 绑定的工厂ID（推单工厂归属） */
    @Excel(name = "工厂ID")
    private Long factoryId;

    /** 是否启用 Y/N */
    @Excel(name = "是否启用", readConverterExp = "Y=启用,N=停用")
    private String enabled;

    /** 过期时间（NULL=永不过期） */
    private Date expiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
}
