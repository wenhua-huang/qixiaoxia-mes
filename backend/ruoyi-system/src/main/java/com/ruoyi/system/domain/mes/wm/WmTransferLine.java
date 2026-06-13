package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 调拨转移单行表对象 qxx_wm_transfer_line
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmTransferLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long lineId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "调拨单ID")
    private Long transferId;

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

    @Excel(name = "调拨数量")
    private BigDecimal quantityTransfer;

    @Excel(name = "批次ID")
    private Long batchId;

    @Excel(name = "批次编码")
    private String batchCode;

    @Excel(name = "来源库区ID")
    private Long sourceLocationId;

    @Excel(name = "来源库位ID")
    private Long sourceAreaId;

    @Excel(name = "目标库区ID")
    private Long targetLocationId;

    @Excel(name = "目标库位ID")
    private Long targetAreaId;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public Long getTransferId() { return transferId; }
    public void setTransferId(Long v) { this.transferId = v; }

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

    public BigDecimal getQuantityTransfer() { return quantityTransfer; }
    public void setQuantityTransfer(BigDecimal v) { this.quantityTransfer = v; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }

    public Long getSourceLocationId() { return sourceLocationId; }
    public void setSourceLocationId(Long v) { this.sourceLocationId = v; }

    public Long getSourceAreaId() { return sourceAreaId; }
    public void setSourceAreaId(Long v) { this.sourceAreaId = v; }

    public Long getTargetLocationId() { return targetLocationId; }
    public void setTargetLocationId(Long v) { this.targetLocationId = v; }

    public Long getTargetAreaId() { return targetAreaId; }
    public void setTargetAreaId(Long v) { this.targetAreaId = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("lineId", getLineId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("unitName", getUnitName())
            .toString();
    }
}