package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存事务表对象 qxx_wm_transaction
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmTransaction extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long transactionId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "事务类型:RECEIPT-入库")
    private String transactionType;

    @Excel(name = "来源单据类型")
    private String sourceDocType;

    @Excel(name = "来源单据ID")
    private Long sourceDocId;

    @Excel(name = "来源单据编码")
    private String sourceDocCode;

    @Excel(name = "来源单据行ID")
    private Long sourceLineId;

    @Excel(name = "库存记录ID")
    private Long materialStockId;

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

    @Excel(name = "变动数量")
    private BigDecimal quantity;

    @Excel(name = "辅助单位编码")
    private String unit2;

    @Excel(name = "辅助单位名称")
    private String unit2Name;

    @Excel(name = "变动数量")
    private BigDecimal quantity2;

    @Excel(name = "批次ID")
    private Long batchId;

    @Excel(name = "批次编码")
    private String batchCode;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "库区ID")
    private Long locationId;

    @Excel(name = "库位ID")
    private Long areaId;

    @Excel(name = "生产工单ID")
    private Long workorderId;

    @Excel(name = "生产工单编码")
    private String workorderCode;

    @Excel(name = "供应商ID")
    private Long vendorId;

    @Excel(name = "客户ID")
    private Long clientId;

    @Excel(name = "关联事务ID")
    private Long relatedTransactionId;

    @Excel(name = "事务时间")
    private Date transactionTime;

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long v) { this.transactionId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String v) { this.transactionType = v; }

    public String getSourceDocType() { return sourceDocType; }
    public void setSourceDocType(String v) { this.sourceDocType = v; }

    public Long getSourceDocId() { return sourceDocId; }
    public void setSourceDocId(Long v) { this.sourceDocId = v; }

    public String getSourceDocCode() { return sourceDocCode; }
    public void setSourceDocCode(String v) { this.sourceDocCode = v; }

    public Long getSourceLineId() { return sourceLineId; }
    public void setSourceLineId(Long v) { this.sourceLineId = v; }

    public Long getMaterialStockId() { return materialStockId; }
    public void setMaterialStockId(Long v) { this.materialStockId = v; }

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

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }

    public String getUnit2() { return unit2; }
    public void setUnit2(String v) { this.unit2 = v; }

    public String getUnit2Name() { return unit2Name; }
    public void setUnit2Name(String v) { this.unit2Name = v; }

    public BigDecimal getQuantity2() { return quantity2; }
    public void setQuantity2(BigDecimal v) { this.quantity2 = v; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }

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

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long v) { this.clientId = v; }

    public Long getRelatedTransactionId() { return relatedTransactionId; }
    public void setRelatedTransactionId(Long v) { this.relatedTransactionId = v; }

    public Date getTransactionTime() { return transactionTime; }
    public void setTransactionTime(Date v) { this.transactionTime = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("transactionId", getTransactionId())
            .append("sourceDocCode", getSourceDocCode())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .toString();
    }
}