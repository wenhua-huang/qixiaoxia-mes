package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 外协收货行 qxx_wm_outsource_recpt_line
 * @author qixiaoxia
 */
public class WmOutsourceRecptLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long lineId;
    private Long factoryId;
    private Long orderId;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String specification;
    private String unitOfMeasure;
    private String unitName;
    private BigDecimal quantity;
    private Long batchId;
    private String batchCode;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    /** 扩展属性JSON（分切子卷 width/gsm 等） */
    private String extAttrs;
    /** 产出对象类型：ROLL-纸卷 */
    private String sourceRefType;
    /** 产出对象ID（如子卷 roll_id） */
    private Long sourceRefId;
    /** 生产日期（厂商录入，用于生成独立成品批次） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date produceDate;
    /** 有效期至 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expireDate;
    /** 生产批号 */
    private String lotNumber;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long v) { this.orderId = v; }
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
    public String getExtAttrs() { return extAttrs; }
    public void setExtAttrs(String v) { this.extAttrs = v; }
    public String getSourceRefType() { return sourceRefType; }
    public void setSourceRefType(String v) { this.sourceRefType = v; }
    public Long getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(Long v) { this.sourceRefId = v; }
    public Date getProduceDate() { return produceDate; }
    public void setProduceDate(Date v) { this.produceDate = v; }
    public Date getExpireDate() { return expireDate; }
    public void setExpireDate(Date v) { this.expireDate = v; }
    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String v) { this.lotNumber = v; }
}
