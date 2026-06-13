package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 纸卷明细表对象 qxx_wm_roll_detail
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmRollDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long rollId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "纸卷号")
    private String rollCode;

    @Excel(name = "物料ID")
    private Long itemId;

    @Excel(name = "物料编码")
    private String itemCode;

    @Excel(name = "物料名称")
    private String itemName;

    @Excel(name = "规格型号")
    private String specification;

    @Excel(name = "批次ID")
    private Long batchId;

    @Excel(name = "批次编码")
    private String batchCode;

    @Excel(name = "入库单ID")
    private Long recptId;

    @Excel(name = "入库明细ID")
    private Long recptDetailId;

    @Excel(name = "供应商ID")
    private Long vendorId;

    @Excel(name = "供应商编码")
    private String vendorCode;

    @Excel(name = "供应商名称")
    private String vendorName;

    @Excel(name = "供应商原始卷号")
    private String vendorRollNo;

    @Excel(name = "父卷ID")
    private Long parentRollId;

    @Excel(name = "实际门幅")
    private String actualWidth;

    @Excel(name = "实际克重")
    private String actualWeightGsm;

    @Excel(name = "实际长度")
    private BigDecimal actualLength;

    @Excel(name = "实际重量")
    private BigDecimal actualWeight;

    @Excel(name = "计量单位")
    private String unitOfMeasure;

    @Excel(name = "原始数量")
    private BigDecimal originalQuantity;

    @Excel(name = "剩余数量")
    private BigDecimal remainingQuantity;

    @Excel(name = "所在仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "库区ID")
    private Long locationId;

    @Excel(name = "库位ID")
    private Long areaId;

    @Excel(name = "库存记录ID")
    private Long materialStockId;

    @Excel(name = "最近领料单ID")
    private Long lastIssueId;

    @Excel(name = "最近生产工单ID")
    private Long lastWorkorderId;

    @Excel(name = "最近生产工单编码")
    private String lastWorkorderCode;

    @Excel(name = "纸卷状态:IN_STOCK-在库")
    private String status;

    public Long getRollId() { return rollId; }
    public void setRollId(Long v) { this.rollId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getRollCode() { return rollCode; }
    public void setRollCode(String v) { this.rollCode = v; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }

    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }

    public String getSpecification() { return specification; }
    public void setSpecification(String v) { this.specification = v; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }

    public Long getRecptId() { return recptId; }
    public void setRecptId(Long v) { this.recptId = v; }

    public Long getRecptDetailId() { return recptDetailId; }
    public void setRecptDetailId(Long v) { this.recptDetailId = v; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }

    public String getVendorRollNo() { return vendorRollNo; }
    public void setVendorRollNo(String v) { this.vendorRollNo = v; }

    public Long getParentRollId() { return parentRollId; }
    public void setParentRollId(Long v) { this.parentRollId = v; }

    public String getActualWidth() { return actualWidth; }
    public void setActualWidth(String v) { this.actualWidth = v; }

    public String getActualWeightGsm() { return actualWeightGsm; }
    public void setActualWeightGsm(String v) { this.actualWeightGsm = v; }

    public BigDecimal getActualLength() { return actualLength; }
    public void setActualLength(BigDecimal v) { this.actualLength = v; }

    public BigDecimal getActualWeight() { return actualWeight; }
    public void setActualWeight(BigDecimal v) { this.actualWeight = v; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }

    public BigDecimal getOriginalQuantity() { return originalQuantity; }
    public void setOriginalQuantity(BigDecimal v) { this.originalQuantity = v; }

    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal v) { this.remainingQuantity = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long v) { this.locationId = v; }

    public Long getAreaId() { return areaId; }
    public void setAreaId(Long v) { this.areaId = v; }

    public Long getMaterialStockId() { return materialStockId; }
    public void setMaterialStockId(Long v) { this.materialStockId = v; }

    public Long getLastIssueId() { return lastIssueId; }
    public void setLastIssueId(Long v) { this.lastIssueId = v; }

    public Long getLastWorkorderId() { return lastWorkorderId; }
    public void setLastWorkorderId(Long v) { this.lastWorkorderId = v; }

    public String getLastWorkorderCode() { return lastWorkorderCode; }
    public void setLastWorkorderCode(String v) { this.lastWorkorderCode = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("rollId", getRollId())
            .append("rollCode", getRollCode())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .toString();
    }
}