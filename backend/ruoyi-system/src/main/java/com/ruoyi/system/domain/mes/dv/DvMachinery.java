package com.ruoyi.system.domain.mes.dv;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备台账对象 qxx_dv_machinery
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
public class DvMachinery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    private Long machineryId;

    /** 工厂ID */
    private Long factoryId;

    /** 设备编码 */
    @Excel(name = "设备编码")
    private String machineryCode;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String machineryName;

    /** 设备品牌 */
    @Excel(name = "品牌")
    private String machineryBrand;

    /** 设备规格型号 */
    @Excel(name = "规格型号")
    private String machinerySpec;

    /** 设备类型ID */
    @Excel(name = "设备类型ID")
    private Long machineryTypeId;

    /** 设备类型编码 */
    @Excel(name = "设备类型编码")
    private String machineryTypeCode;

    /** 设备类型名称 */
    @Excel(name = "设备类型名称")
    private String machineryTypeName;

    /** 所属车间ID */
    @Excel(name = "所属车间ID")
    private Long workshopId;

    /** 所属车间编码 */
    @Excel(name = "所属车间编码")
    private String workshopCode;

    /** 所属车间名称 */
    @Excel(name = "所属车间名称")
    private String workshopName;

    /** 最后保养时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "最后保养时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastMaintenTime;

    /** 最后点检时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "最后点检时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheckTime;

    /** 设备类型ID列表（查询时子树过滤用，不入库） */
    private List<Long> machineryTypeIds;

    /** 设备状态 */
    @Excel(name = "设备状态", readConverterExp = "IDLE=空闲,RUNNING=运行中,MAINTENANCE=保养中,BREAKDOWN=故障停机")
    private String status;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public Long getMachineryId() { return machineryId; }
    public void setMachineryId(Long machineryId) { this.machineryId = machineryId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public String getMachineryCode() { return machineryCode; }
    public void setMachineryCode(String machineryCode) { this.machineryCode = machineryCode; }

    public String getMachineryName() { return machineryName; }
    public void setMachineryName(String machineryName) { this.machineryName = machineryName; }

    public String getMachineryBrand() { return machineryBrand; }
    public void setMachineryBrand(String machineryBrand) { this.machineryBrand = machineryBrand; }

    public String getMachinerySpec() { return machinerySpec; }
    public void setMachinerySpec(String machinerySpec) { this.machinerySpec = machinerySpec; }

    public Long getMachineryTypeId() { return machineryTypeId; }
    public void setMachineryTypeId(Long machineryTypeId) { this.machineryTypeId = machineryTypeId; }
    public List<Long> getMachineryTypeIds() { return machineryTypeIds; }
    public void setMachineryTypeIds(List<Long> machineryTypeIds) { this.machineryTypeIds = machineryTypeIds; }

    public String getMachineryTypeCode() { return machineryTypeCode; }
    public void setMachineryTypeCode(String machineryTypeCode) { this.machineryTypeCode = machineryTypeCode; }

    public String getMachineryTypeName() { return machineryTypeName; }
    public void setMachineryTypeName(String machineryTypeName) { this.machineryTypeName = machineryTypeName; }

    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }

    public String getWorkshopCode() { return workshopCode; }
    public void setWorkshopCode(String workshopCode) { this.workshopCode = workshopCode; }

    public String getWorkshopName() { return workshopName; }
    public void setWorkshopName(String workshopName) { this.workshopName = workshopName; }

    public Date getLastMaintenTime() { return lastMaintenTime; }
    public void setLastMaintenTime(Date lastMaintenTime) { this.lastMaintenTime = lastMaintenTime; }

    public Date getLastCheckTime() { return lastCheckTime; }
    public void setLastCheckTime(Date lastCheckTime) { this.lastCheckTime = lastCheckTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("machineryId", getMachineryId())
            .append("factoryId", getFactoryId())
            .append("machineryCode", getMachineryCode())
            .append("machineryName", getMachineryName())
            .append("machineryBrand", getMachineryBrand())
            .append("machinerySpec", getMachinerySpec())
            .append("machineryTypeId", getMachineryTypeId())
            .append("machineryTypeCode", getMachineryTypeCode())
            .append("machineryTypeName", getMachineryTypeName())
            .append("workshopId", getWorkshopId())
            .append("workshopCode", getWorkshopCode())
            .append("workshopName", getWorkshopName())
            .append("lastMaintenTime", getLastMaintenTime())
            .append("lastCheckTime", getLastCheckTime())
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
