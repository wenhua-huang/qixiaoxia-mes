package com.ruoyi.system.domain.mes.md;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料产品对象 qxx_md_item
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 产品物料ID */
    private Long itemId;

    /** 工厂ID */
    private Long factoryId;

    /** 产品物料编码 */
    @Excel(name = "物料编码")
    private String itemCode;

    /** 产品物料名称 */
    @Excel(name = "物料名称")
    private String itemName;

    /** 规格型号描述 */
    @Excel(name = "规格型号")
    private String specification;

    /** 主单位编码 */
    @Excel(name = "主单位编码")
    private String unitOfMeasure;

    /** 主单位名称 */
    @Excel(name = "主单位名称")
    private String unitName;

    /** 辅助单位编码 */
    @Excel(name = "辅助单位编码")
    private String unit2;

    /** 辅助单位名称 */
    @Excel(name = "辅助单位名称")
    private String unit2Name;

    /** 主单位→辅助单位换算率 */
    @Excel(name = "换算率")
    private BigDecimal conversionRate;

    /** 物料类型ID */
    private Long itemTypeId;

    /** 物料类型编码 */
    private String itemTypeCode;

    /** 物料类型名称 */
    @Excel(name = "物料类型")
    private String itemTypeName;

    /** 父产品ID(0=SPU/原料,非0=变体) */
    private Long parentId;

    /** 产品尺寸 */
    @Excel(name = "产品尺寸")
    private String productSize;

    /** 装箱规格 */
    @Excel(name = "装箱规格")
    private String packageSpec;

    /** 印刷要求描述 */
    @Excel(name = "印刷要求")
    private String printingReq;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    /** 是否设置安全库存 */
    private String safeStockFlag;

    /** 安全库存最低量 */
    private BigDecimal minStock;

    /** 安全库存最高量 */
    private BigDecimal maxStock;

    /** 预警库存量 */
    private BigDecimal alertStock;

    /** 是否高价值物资 */
    private String highValue;

    /** 是否启用批次管理 */
    private String batchFlag;

    // ---- 行业子表关联字段（非DB列，GET时JOIN填充） ----
    /** 纸张属性 */
    private MdItemAttrPaper attrPaper;

    /** 纸袋属性 */
    private MdItemAttrPaperBag attrPaperBag;

    /** 礼品盒属性 */
    private MdItemAttrGiftBox attrGiftBox;

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public String getUnit2() { return unit2; }
    public void setUnit2(String unit2) { this.unit2 = unit2; }

    public String getUnit2Name() { return unit2Name; }
    public void setUnit2Name(String unit2Name) { this.unit2Name = unit2Name; }

    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) { this.conversionRate = conversionRate; }

    public Long getItemTypeId() { return itemTypeId; }
    public void setItemTypeId(Long itemTypeId) { this.itemTypeId = itemTypeId; }

    public String getItemTypeCode() { return itemTypeCode; }
    public void setItemTypeCode(String itemTypeCode) { this.itemTypeCode = itemTypeCode; }

    public String getItemTypeName() { return itemTypeName; }
    public void setItemTypeName(String itemTypeName) { this.itemTypeName = itemTypeName; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getProductSize() { return productSize; }
    public void setProductSize(String productSize) { this.productSize = productSize; }

    public String getPackageSpec() { return packageSpec; }
    public void setPackageSpec(String packageSpec) { this.packageSpec = packageSpec; }

    public String getPrintingReq() { return printingReq; }
    public void setPrintingReq(String printingReq) { this.printingReq = printingReq; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }

    public String getSafeStockFlag() { return safeStockFlag; }
    public void setSafeStockFlag(String safeStockFlag) { this.safeStockFlag = safeStockFlag; }

    public BigDecimal getMinStock() { return minStock; }
    public void setMinStock(BigDecimal minStock) { this.minStock = minStock; }

    public BigDecimal getMaxStock() { return maxStock; }
    public void setMaxStock(BigDecimal maxStock) { this.maxStock = maxStock; }

    public BigDecimal getAlertStock() { return alertStock; }
    public void setAlertStock(BigDecimal alertStock) { this.alertStock = alertStock; }

    public String getHighValue() { return highValue; }
    public void setHighValue(String highValue) { this.highValue = highValue; }

    public String getBatchFlag() { return batchFlag; }
    public void setBatchFlag(String batchFlag) { this.batchFlag = batchFlag; }

    public MdItemAttrPaper getAttrPaper() { return attrPaper; }
    public void setAttrPaper(MdItemAttrPaper attrPaper) { this.attrPaper = attrPaper; }

    public MdItemAttrPaperBag getAttrPaperBag() { return attrPaperBag; }
    public void setAttrPaperBag(MdItemAttrPaperBag attrPaperBag) { this.attrPaperBag = attrPaperBag; }

    public MdItemAttrGiftBox getAttrGiftBox() { return attrGiftBox; }
    public void setAttrGiftBox(MdItemAttrGiftBox attrGiftBox) { this.attrGiftBox = attrGiftBox; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("specification", getSpecification())
            .append("unitOfMeasure", getUnitOfMeasure())
            .append("parentId", getParentId())
            .append("enableFlag", getEnableFlag())
            .toString();
    }
}
