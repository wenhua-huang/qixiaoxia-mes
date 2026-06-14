package com.ruoyi.system.domain.mes.tm;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 模具台账对象 qxx_tm_tool
 *
 * @author qixiaoxia
 * @date 2026-06-14
 */
public class TmTool extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模具ID */
    private Long toolId;

    /** 工厂ID */
    private Long factoryId;

    /** 模具编码 */
    @Excel(name = "模具编码")
    private String toolCode;

    /** 模具名称 */
    @Excel(name = "模具名称")
    private String toolName;

    /** 品牌 */
    @Excel(name = "品牌")
    private String brand;

    /** 规格型号 */
    @Excel(name = "规格型号")
    private String spec;

    /** 模具类型ID */
    @Excel(name = "模具类型ID")
    private Long toolTypeId;

    /** 模具类型编码 */
    @Excel(name = "模具类型编码")
    private String toolTypeCode;

    /** 模具类型名称 */
    @Excel(name = "模具类型名称")
    private String toolTypeName;

    /** 存放位置 */
    @Excel(name = "存放位置")
    private String location;

    /** 总数量 */
    @Excel(name = "总数量")
    private Integer quantity;

    /** 可用数量 */
    @Excel(name = "可用数量")
    private Integer availableQuantity;

    /** 保养类型 */
    @Excel(name = "保养类型")
    private String maintenType;

    /** 保养周期 */
    @Excel(name = "保养周期")
    private Integer maintenCycle;

    /** 下次保养日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "下次保养日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date nextMaintenDate;

    /** 状态: STORE-在库, USING-使用中, MAINTAINING-保养中, SCRAPPED-报废 */
    @Excel(name = "状态", readConverterExp = "STORE=在库,USING=使用中,MAINTAINING=保养中,SCRAPPED=报废")
    private String status;

    /** 是否启用(1-是, 0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    public void setToolId(Long toolId) { this.toolId = toolId; }
    public Long getToolId() { return toolId; }

    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }
    public Long getFactoryId() { return factoryId; }

    public void setToolCode(String toolCode) { this.toolCode = toolCode; }
    public String getToolCode() { return toolCode; }

    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getToolName() { return toolName; }

    public void setBrand(String brand) { this.brand = brand; }
    public String getBrand() { return brand; }

    public void setSpec(String spec) { this.spec = spec; }
    public String getSpec() { return spec; }

    public void setToolTypeId(Long toolTypeId) { this.toolTypeId = toolTypeId; }
    public Long getToolTypeId() { return toolTypeId; }

    public void setToolTypeCode(String toolTypeCode) { this.toolTypeCode = toolTypeCode; }
    public String getToolTypeCode() { return toolTypeCode; }

    public void setToolTypeName(String toolTypeName) { this.toolTypeName = toolTypeName; }
    public String getToolTypeName() { return toolTypeName; }

    public void setLocation(String location) { this.location = location; }
    public String getLocation() { return location; }

    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getQuantity() { return quantity; }

    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    public Integer getAvailableQuantity() { return availableQuantity; }

    public void setMaintenType(String maintenType) { this.maintenType = maintenType; }
    public String getMaintenType() { return maintenType; }

    public void setMaintenCycle(Integer maintenCycle) { this.maintenCycle = maintenCycle; }
    public Integer getMaintenCycle() { return maintenCycle; }

    public void setNextMaintenDate(Date nextMaintenDate) { this.nextMaintenDate = nextMaintenDate; }
    public Date getNextMaintenDate() { return nextMaintenDate; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }
    public String getEnableFlag() { return enableFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("toolId", getToolId())
            .append("factoryId", getFactoryId())
            .append("toolCode", getToolCode())
            .append("toolName", getToolName())
            .append("brand", getBrand())
            .append("spec", getSpec())
            .append("toolTypeId", getToolTypeId())
            .append("toolTypeCode", getToolTypeCode())
            .append("toolTypeName", getToolTypeName())
            .append("location", getLocation())
            .append("quantity", getQuantity())
            .append("availableQuantity", getAvailableQuantity())
            .append("maintenType", getMaintenType())
            .append("maintenCycle", getMaintenCycle())
            .append("nextMaintenDate", getNextMaintenDate())
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
