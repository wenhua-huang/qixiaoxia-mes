package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 工单BOM组成对象 qxx_pro_workorder_bom
 */
public class ProWorkorderBom extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long lineId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "工单ID") private Long workorderId;
    @Excel(name = "物料ID") private Long itemId;
    @Excel(name = "物料编码") private String itemCode;
    @Excel(name = "物料名称") private String itemName;
    @Excel(name = "规格型号") private String itemSpc;
    @Excel(name = "主单位编码") private String unitOfMeasure;
    @Excel(name = "主单位名称") private String unitName;
    @Excel(name = "辅助单位编码") private String unit2;
    @Excel(name = "辅助单位名称") private String unit2Name;
    @Excel(name = "换算率") private BigDecimal conversionRate;
    @Excel(name = "工序ID") private Long processId;
    @Excel(name = "工序名称") private String processName;
    @Excel(name = "物料产品标识") private String itemOrProduct;
    @Excel(name = "单位用量") private BigDecimal quantity;
    @Excel(name = "预计总用量") private BigDecimal totalQuantity;
    @Excel(name = "预计总用量(辅助单位)") private BigDecimal totalQuantity2;

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public String getItemSpc() { return itemSpc; }
    public void setItemSpc(String v) { this.itemSpc = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public String getUnit2() { return unit2; }
    public void setUnit2(String v) { this.unit2 = v; }
    public String getUnit2Name() { return unit2Name; }
    public void setUnit2Name(String v) { this.unit2Name = v; }
    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal v) { this.conversionRate = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public String getItemOrProduct() { return itemOrProduct; }
    public void setItemOrProduct(String v) { this.itemOrProduct = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }
    public BigDecimal getTotalQuantity2() { return totalQuantity2; }
    public void setTotalQuantity2(BigDecimal v) { this.totalQuantity2 = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("lineId", getLineId()).append("workorderId", getWorkorderId())
            .append("itemCode", getItemCode()).append("quantity", getQuantity()).toString();
    }
}
