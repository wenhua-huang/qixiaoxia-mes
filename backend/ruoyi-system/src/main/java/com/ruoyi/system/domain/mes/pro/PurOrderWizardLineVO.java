package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;

/**
 * 采购向导提交行 — 用户在弹窗中勾选、修改数量后提交的每行数据
 *
 * @author qixiaoxia
 * @date 2026-07-01
 */
public class PurOrderWizardLineVO
{
    private Long itemId;
    private String itemCode;
    private String itemName;
    private String unitOfMeasure;
    private String unitName;
    private Long vendorId;
    private String vendorCode;
    private String vendorName;
    private BigDecimal quantity;

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
}
