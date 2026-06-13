package com.ruoyi.system.domain.mes.wm;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 盘点方案表对象 qxx_wm_stock_taking_plan
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmStockTakingPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long planId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "盘点方案编码")
    private String planCode;

    @Excel(name = "盘点方案名称")
    private String planName;

    @Excel(name = "盘点类型:FULL-全盘")
    private String planType;

    @Excel(name = "盘点仓库ID")
    private Long warehouseId;

    @Excel(name = "盘点仓库名称")
    private String warehouseName;

    @Excel(name = "计划盘点日期")
    private Date planDate;

    @Excel(name = "状态:PREPARE-准备中")
    private String status;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long v) { this.planId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getPlanCode() { return planCode; }
    public void setPlanCode(String v) { this.planCode = v; }

    public String getPlanName() { return planName; }
    public void setPlanName(String v) { this.planName = v; }

    public String getPlanType() { return planType; }
    public void setPlanType(String v) { this.planType = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Date getPlanDate() { return planDate; }
    public void setPlanDate(Date v) { this.planDate = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("planId", getPlanId())
            .append("planCode", getPlanCode())
            .append("planName", getPlanName())
            .append("warehouseName", getWarehouseName())
            .toString();
    }
}