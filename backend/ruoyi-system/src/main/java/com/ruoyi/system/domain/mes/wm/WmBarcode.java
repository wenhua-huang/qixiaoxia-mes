package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 条码清单表对象 qxx_wm_barcode
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmBarcode extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long barcodeId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "条码内容")
    private String barcodeCode;

    @Excel(name = "条码类型:PRODUCT-产品")
    private String barcodeType;

    @Excel(name = "物料ID")
    private Long itemId;

    @Excel(name = "物料编码")
    private String itemCode;

    @Excel(name = "物料名称")
    private String itemName;

    @Excel(name = "生产工单ID")
    private Long workorderId;

    @Excel(name = "生产工单编码")
    private String workorderCode;

    @Excel(name = "数量")
    private BigDecimal quantity;

    @Excel(name = "单位")
    private String unitOfMeasure;

    @Excel(name = "批次号")
    private String batchCode;

    @Excel(name = "打印次数")
    private Long printCount;

    @Excel(name = "最后打印时间")
    private Date lastPrintTime;

    @Excel(name = "是否启用")
    private String enableFlag;

    public Long getBarcodeId() { return barcodeId; }
    public void setBarcodeId(Long v) { this.barcodeId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getBarcodeCode() { return barcodeCode; }
    public void setBarcodeCode(String v) { this.barcodeCode = v; }

    public String getBarcodeType() { return barcodeType; }
    public void setBarcodeType(String v) { this.barcodeType = v; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }

    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }

    public Long getPrintCount() { return printCount; }
    public void setPrintCount(Long v) { this.printCount = v; }

    public Date getLastPrintTime() { return lastPrintTime; }
    public void setLastPrintTime(Date v) { this.lastPrintTime = v; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String v) { this.enableFlag = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("barcodeId", getBarcodeId())
            .append("barcodeCode", getBarcodeCode())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .toString();
    }
}