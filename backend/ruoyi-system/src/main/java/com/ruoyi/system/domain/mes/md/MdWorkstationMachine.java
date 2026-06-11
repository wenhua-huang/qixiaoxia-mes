package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工作站-设备对象 qxx_md_workstation_machine
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdWorkstationMachine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long recordId;

    /** 工厂ID */
    private Long factoryId;

    /** 工作站ID */
    @Excel(name = "工作站ID")
    private Long workstationId;

    /** 设备ID */
    @Excel(name = "设备ID")
    private Long machineryId;

    /** 设备编码 */
    @Excel(name = "设备编码")
    private String machineryCode;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String machineryName;

    public Long getRecordId()
    {
        return recordId;
    }

    public void setRecordId(Long recordId)
    {
        this.recordId = recordId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public Long getWorkstationId()
    {
        return workstationId;
    }

    public void setWorkstationId(Long workstationId)
    {
        this.workstationId = workstationId;
    }

    public Long getMachineryId()
    {
        return machineryId;
    }

    public void setMachineryId(Long machineryId)
    {
        this.machineryId = machineryId;
    }

    public String getMachineryCode()
    {
        return machineryCode;
    }

    public void setMachineryCode(String machineryCode)
    {
        this.machineryCode = machineryCode;
    }

    public String getMachineryName()
    {
        return machineryName;
    }

    public void setMachineryName(String machineryName)
    {
        this.machineryName = machineryName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("factoryId", getFactoryId())
            .append("workstationId", getWorkstationId())
            .append("machineryId", getMachineryId())
            .append("machineryCode", getMachineryCode())
            .append("machineryName", getMachineryName())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
