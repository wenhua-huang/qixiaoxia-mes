package com.ruoyi.system.domain.mes.md;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 产品BOM对象 qxx_md_product_bom
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdProductBom extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** BOM ID */
    private Long bomId;

    /** 工厂ID */
    private Long factoryId;

    /** 物料产品ID */
    @Excel(name = "物料产品ID")
    private Long itemId;

    /** 子物料产品ID */
    @Excel(name = "子物料产品ID")
    private Long bomItemId;

    /** 单位 */
    @Excel(name = "单位")
    private String unitOfMeasure;

    /** 数量 */
    @Excel(name = "数量")
    private BigDecimal quantity;

    /** 废品率 */
    @Excel(name = "废品率")
    private BigDecimal scrapRate;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    // ---- JOIN 字段（非 DB 列，查询时填充） ----
    private String itemCode;
    private String itemName;
    private String bomItemCode;
    private String bomItemName;

    public Long getBomId()
    {
        return bomId;
    }

    public void setBomId(Long bomId)
    {
        this.bomId = bomId;
    }

    public Long getFactoryId()
    {
        return factoryId;
    }

    public void setFactoryId(Long factoryId)
    {
        this.factoryId = factoryId;
    }

    public Long getItemId()
    {
        return itemId;
    }

    public void setItemId(Long itemId)
    {
        this.itemId = itemId;
    }

    public Long getBomItemId()
    {
        return bomItemId;
    }

    public void setBomItemId(Long bomItemId)
    {
        this.bomItemId = bomItemId;
    }

    public String getUnitOfMeasure()
    {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure)
    {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getQuantity()
    {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getScrapRate()
    {
        return scrapRate;
    }

    public void setScrapRate(BigDecimal scrapRate)
    {
        this.scrapRate = scrapRate;
    }

    public String getEnableFlag()
    {
        return enableFlag;
    }

    public void setEnableFlag(String enableFlag)
    {
        this.enableFlag = enableFlag;
    }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public String getBomItemCode() { return bomItemCode; }
    public void setBomItemCode(String v) { this.bomItemCode = v; }
    public String getBomItemName() { return bomItemName; }
    public void setBomItemName(String v) { this.bomItemName = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("bomId", getBomId())
            .append("factoryId", getFactoryId())
            .append("itemId", getItemId())
            .append("bomItemId", getBomItemId())
            .append("unitOfMeasure", getUnitOfMeasure())
            .append("quantity", getQuantity())
            .append("scrapRate", getScrapRate())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
