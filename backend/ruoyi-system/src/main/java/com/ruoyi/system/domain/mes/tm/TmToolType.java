package com.ruoyi.system.domain.mes.tm;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 模具类型对象 qxx_tm_tool_type
 *
 * @author qixiaoxia
 * @date 2026-06-14
 */
public class TmToolType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模具类型ID */
    private Long toolTypeId;

    /** 工厂ID */
    private Long factoryId;

    /** 类型编码 */
    @Excel(name = "类型编码")
    private String toolTypeCode;

    /** 类型名称 */
    @Excel(name = "类型名称")
    private String toolTypeName;

    /** 保养类型 */
    @Excel(name = "保养类型")
    private String maintenType;

    /** 保养周期 */
    @Excel(name = "保养周期")
    private Integer maintenCycle;

    /** 是否启用(1-是, 0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public void setToolTypeId(Long toolTypeId) { this.toolTypeId = toolTypeId; }
    public Long getToolTypeId() { return toolTypeId; }

    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }
    public Long getFactoryId() { return factoryId; }

    public void setToolTypeCode(String toolTypeCode) { this.toolTypeCode = toolTypeCode; }
    public String getToolTypeCode() { return toolTypeCode; }

    public void setToolTypeName(String toolTypeName) { this.toolTypeName = toolTypeName; }
    public String getToolTypeName() { return toolTypeName; }

    public void setMaintenType(String maintenType) { this.maintenType = maintenType; }
    public String getMaintenType() { return maintenType; }

    public void setMaintenCycle(Integer maintenCycle) { this.maintenCycle = maintenCycle; }
    public Integer getMaintenCycle() { return maintenCycle; }

    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }
    public String getEnableFlag() { return enableFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("toolTypeId", getToolTypeId())
            .append("factoryId", getFactoryId())
            .append("toolTypeCode", getToolTypeCode())
            .append("toolTypeName", getToolTypeName())
            .append("maintenType", getMaintenType())
            .append("maintenCycle", getMaintenCycle())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
