package com.ruoyi.system.domain.mes.pro;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 排产快照对象 qxx_pro_snapshot
 *
 * @author qixiaoxia
 * @date 2026-06-29
 */
public class ProSnapshot extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "快照名称") private String name;
    @Excel(name = "范围类型") private String scopeType;
    @Excel(name = "范围ID") private Long scopeId;
    @Excel(name = "状态") private String status;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getScopeType() { return scopeType; }
    public void setScopeType(String v) { this.scopeType = v; }
    public Long getScopeId() { return scopeId; }
    public void setScopeId(Long v) { this.scopeId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
