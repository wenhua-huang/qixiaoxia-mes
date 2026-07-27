package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 销售出库-装箱明细对象 qxx_wm_product_sales_box
 * 一箱一条；本期不支持混装（line_id 单行）
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
public class WmProductSalesBox extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long boxId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "出库单ID")
    private Long salesId;

    @Excel(name = "出库行ID")
    private Long lineId;

    @Excel(name = "箱号")
    private String boxNo;

    @Excel(name = "物料ID")
    private Long itemId;

    @Excel(name = "物料编码")
    private String itemCode;

    @Excel(name = "物料名称")
    private String itemName;

    @Excel(name = "规格")
    private String specification;

    @Excel(name = "数量")
    private BigDecimal quantity;

    @Excel(name = "单位编码")
    private String unitOfMeasure;

    @Excel(name = "单位名称")
    private String unitName;

    @Excel(name = "箱规")
    private String boxSpec;

    @Excel(name = "箱长cm")
    private BigDecimal boxLength;

    @Excel(name = "箱宽cm")
    private BigDecimal boxWidth;

    @Excel(name = "箱高cm")
    private BigDecimal boxHeight;

    @Excel(name = "体积m³")
    private BigDecimal volume;

    @Excel(name = "重量kg")
    private BigDecimal weight;

    @Excel(name = "发运单ID")
    private Long shipmentId;

    @Excel(name = "装箱状态")
    private String status;

    public Long getBoxId() { return boxId; }
    public void setBoxId(Long v) { this.boxId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public Long getSalesId() { return salesId; }
    public void setSalesId(Long v) { this.salesId = v; }

    public Long getLineId() { return lineId; }
    public void setLineId(Long v) { this.lineId = v; }

    public String getBoxNo() { return boxNo; }
    public void setBoxNo(String v) { this.boxNo = v; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }

    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }

    public String getSpecification() { return specification; }
    public void setSpecification(String v) { this.specification = v; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }

    public String getBoxSpec() { return boxSpec; }
    public void setBoxSpec(String v) { this.boxSpec = v; }

    public BigDecimal getBoxLength() { return boxLength; }
    public void setBoxLength(BigDecimal v) { this.boxLength = v; }

    public BigDecimal getBoxWidth() { return boxWidth; }
    public void setBoxWidth(BigDecimal v) { this.boxWidth = v; }

    public BigDecimal getBoxHeight() { return boxHeight; }
    public void setBoxHeight(BigDecimal v) { this.boxHeight = v; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal v) { this.volume = v; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal v) { this.weight = v; }

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long v) { this.shipmentId = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("boxId", getBoxId())
            .append("boxNo", getBoxNo())
            .append("salesId", getSalesId())
            .append("itemCode", getItemCode())
            .append("quantity", getQuantity())
            .toString();
    }
}
