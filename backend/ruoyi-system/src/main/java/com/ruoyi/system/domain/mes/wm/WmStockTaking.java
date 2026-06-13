package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 盘点任务表对象 qxx_wm_stock_taking
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmStockTaking extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long takingId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "盘点任务编码")
    private String takingCode;

    @Excel(name = "盘点方案ID")
    private Long planId;

    @Excel(name = "盘点方案编码")
    private String planCode;

    @Excel(name = "物料ID")
    private Long itemId;

    @Excel(name = "物料编码")
    private String itemCode;

    @Excel(name = "物料名称")
    private String itemName;

    @Excel(name = "规格型号")
    private String specification;

    @Excel(name = "单位编码")
    private String unitOfMeasure;

    @Excel(name = "单位名称")
    private String unitName;

    @Excel(name = "批次ID")
    private Long batchId;

    @Excel(name = "批次编码")
    private String batchCode;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "库区ID")
    private Long locationId;

    @Excel(name = "库位ID")
    private Long areaId;

    @Excel(name = "账面数量")
    private BigDecimal bookQuantity;

    @Excel(name = "实际盘点数量")
    private BigDecimal actualQuantity;

    @Excel(name = "差异数量")
    private BigDecimal difference;

    @Excel(name = "差异原因")
    private String diffReason;

    @Excel(name = "盘点人")
    private String takingUser;

    @Excel(name = "盘点日期")
    private Date takingDate;

    @Excel(name = "状态:PENDING-待盘点")
    private String status;

    public Long getTakingId() { return takingId; }
    public void setTakingId(Long v) { this.takingId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getTakingCode() { return takingCode; }
    public void setTakingCode(String v) { this.takingCode = v; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long v) { this.planId = v; }

    public String getPlanCode() { return planCode; }
    public void setPlanCode(String v) { this.planCode = v; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }

    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }

    public String getSpecification() { return specification; }
    public void setSpecification(String v) { this.specification = v; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long v) { this.locationId = v; }

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long v) { this.areaId = v; }

    public BigDecimal getBookQuantity() { return bookQuantity; }
    public void setBookQuantity(BigDecimal v) { this.bookQuantity = v; }

    public BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(BigDecimal v) { this.actualQuantity = v; }

    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal v) { this.difference = v; }

    public String getDiffReason() { return diffReason; }
    public void setDiffReason(String v) { this.diffReason = v; }

    public String getTakingUser() { return takingUser; }
    public void setTakingUser(String v) { this.takingUser = v; }

    public Date getTakingDate() { return takingDate; }
    public void setTakingDate(Date v) { this.takingDate = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("takingId", getTakingId())
            .append("takingCode", getTakingCode())
            .append("planCode", getPlanCode())
            .append("itemCode", getItemCode())
            .toString();
    }
}