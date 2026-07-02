package com.ruoyi.system.domain.mes.md;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料供应商关系对象 qxx_md_item_vendor
 *
 * @author qixiaoxia
 * @date 2026-07-01
 */
public class MdItemVendor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long recordId;
    private Long factoryId;
    @Excel(name = "物料ID")
    private Long itemId;
    @Excel(name = "物料编码")
    private String itemCode;
    @Excel(name = "物料名称")
    private String itemName;
    @Excel(name = "供应商ID")
    private Long vendorId;
    @Excel(name = "供应商编码")
    private String vendorCode;
    @Excel(name = "供应商名称")
    private String vendorName;
    @Excel(name = "是否首选")
    private String isPreferred;
    @Excel(name = "最小起订量")
    private BigDecimal minOrderQty;
    @Excel(name = "采购提前期")
    private Integer leadTimeDays;
    @Excel(name = "是否启用")
    private String enableFlag;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long v) { this.recordId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public String getIsPreferred() { return isPreferred; }
    public void setIsPreferred(String v) { this.isPreferred = v; }
    public BigDecimal getMinOrderQty() { return minOrderQty; }
    public void setMinOrderQty(BigDecimal v) { this.minOrderQty = v; }
    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer v) { this.leadTimeDays = v; }
    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String v) { this.enableFlag = v; }
}
