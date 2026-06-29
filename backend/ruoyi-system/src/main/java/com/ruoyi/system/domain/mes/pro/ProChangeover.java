package com.ruoyi.system.domain.mes.pro;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工序换型时间对象 qxx_pro_changeover
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
public class ProChangeover extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 换型ID */
    private Long id;

    /** 工厂ID */
    @Excel(name = "工厂ID")
    private Long factoryId;

    /** 换型名称 */
    @Excel(name = "换型名称")
    private String name;

    /** 从工序ID(NULL=任意工序) */
    @Excel(name = "从工序ID")
    private Long fromProcessId;

    /** 到工序ID */
    @Excel(name = "到工序ID")
    private Long toProcessId;

    /** 工作站ID(NULL=通用换型规范) */
    @Excel(name = "工作站ID")
    private Long workstationId;

    /** 换型时长(分钟) */
    @Excel(name = "换型时长")
    private Integer durationMinutes;

    // -- getters/setters --

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getFromProcessId() { return fromProcessId; }
    public void setFromProcessId(Long fromProcessId) { this.fromProcessId = fromProcessId; }

    public Long getToProcessId() { return toProcessId; }
    public void setToProcessId(Long toProcessId) { this.toProcessId = toProcessId; }

    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long workstationId) { this.workstationId = workstationId; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("factoryId", getFactoryId())
            .append("name", getName())
            .append("fromProcessId", getFromProcessId())
            .append("toProcessId", getToProcessId())
            .append("workstationId", getWorkstationId())
            .append("durationMinutes", getDurationMinutes())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
