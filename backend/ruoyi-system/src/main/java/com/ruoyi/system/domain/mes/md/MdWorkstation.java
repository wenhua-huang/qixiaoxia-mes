package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工作站对象 qxx_md_workstation
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdWorkstation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 工作站ID */
    private Long workstationId;

    /** 工厂ID */
    private Long factoryId;

    /** 工作站编码 */
    @Excel(name = "工作站编码")
    private String workstationCode;

    /** 工作站名称 */
    @Excel(name = "工作站名称")
    private String workstationName;

    /** 车间ID */
    @Excel(name = "车间ID")
    private Long workshopId;

    /** 工作站类型 */
    @Excel(name = "工作站类型")
    private String workstationType;

    /** 工序类型 */
    @Excel(name = "工序类型")
    private String processType;

    /** 工序ID(关联qxx_pro_process) */
    private Long processId;

    /** 工序名称(关联查询，非DB字段) */
    @Excel(name = "工序名称")
    private String processName;

    /** 产能 */
    @Excel(name = "产能")
    private Integer capacity;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public Long getWorkstationId()
    {
        return workstationId;
    }

    public void setWorkstationId(Long workstationId)
    {
        this.workstationId = workstationId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public String getWorkstationCode()
    {
        return workstationCode;
    }

    public void setWorkstationCode(String workstationCode)
    {
        this.workstationCode = workstationCode;
    }

    public String getWorkstationName()
    {
        return workstationName;
    }

    public void setWorkstationName(String workstationName)
    {
        this.workstationName = workstationName;
    }

    public Long getWorkshopId()
    {
        return workshopId;
    }

    public void setWorkshopId(Long workshopId)
    {
        this.workshopId = workshopId;
    }

    public String getWorkstationType()
    {
        return workstationType;
    }

    public void setWorkstationType(String workstationType)
    {
        this.workstationType = workstationType;
    }

    public String getProcessType()
    {
        return processType;
    }

    public void setProcessType(String processType)
    {
        this.processType = processType;
    }

    public Long getProcessId()
    {
        return processId;
    }

    public void setProcessId(Long processId)
    {
        this.processId = processId;
    }

    public String getProcessName()
    {
        return processName;
    }

    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    public Integer getCapacity()
    {
        return capacity;
    }

    public void setCapacity(Integer capacity)
    {
        this.capacity = capacity;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getEnableFlag()
    {
        return enableFlag;
    }

    public void setEnableFlag(String enableFlag)
    {
        this.enableFlag = enableFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("workstationId", getWorkstationId())
            .append("factoryId", getFactoryId())
            .append("workstationCode", getWorkstationCode())
            .append("workstationName", getWorkstationName())
            .append("workshopId", getWorkshopId())
            .append("workstationType", getWorkstationType())
            .append("processType", getProcessType())
            .append("capacity", getCapacity())
            .append("status", getStatus())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
