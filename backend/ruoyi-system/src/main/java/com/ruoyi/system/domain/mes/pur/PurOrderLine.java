package com.ruoyi.system.domain.mes.pur;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 采购订单行对象 qxx_pur_order_line
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class PurOrderLine extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 行ID */
    private Long lineId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 采购订单ID(关联qxx_pur_order) */
    @Excel(name = "采购订单ID(关联qxx_pur_order)")
    private Long orderId;

    /** 物料ID(关联qxx_md_item) */
    @Excel(name = "物料ID(关联qxx_md_item)")
    private Long itemId;

    /** 物料编码 */
    @Excel(name = "物料编码")
    private String itemCode;

    /** 物料名称 */
    @Excel(name = "物料名称")
    private String itemName;

    /** 规格型号 */
    @Excel(name = "规格型号")
    private String specification;

    /** 主单位编码(如TON-吨,ROLL-卷,PCS-个,KG-千克) */
    @Excel(name = "主单位编码(如TON-吨,ROLL-卷,PCS-个,KG-千克)")
    private String unitOfMeasure;

    /** 主单位名称 */
    @Excel(name = "主单位名称")
    private String unitName;

    /** 辅助单位编码(如ROLL-卷,与主单位联动) */
    @Excel(name = "辅助单位编码(如ROLL-卷,与主单位联动)")
    private String unit2;

    /** 辅助单位名称 */
    @Excel(name = "辅助单位名称")
    private String unit2Name;

    /** 主单位→辅助单位换算率 */
    @Excel(name = "主单位→辅助单位换算率")
    private BigDecimal conversionRate;

    /** 订购数量(主单位) */
    @Excel(name = "订购数量(主单位)")
    private BigDecimal quantityOrdered;

    /** 订购数量(辅助单位,如卷数) */
    @Excel(name = "订购数量(辅助单位,如卷数)")
    private BigDecimal quantityOrdered2;

    /** 已收货数量(主单位) */
    @Excel(name = "已收货数量(主单位)")
    private BigDecimal quantityReceived;

    /** 已收货数量(辅助单位) */
    @Excel(name = "已收货数量(辅助单位)")
    private BigDecimal quantityReceived2;

    /** 单价(元/主单位,如元/吨) */
    @Excel(name = "单价(元/主单位,如元/吨)")
    private BigDecimal unitPrice;

    /** 行金额(不含税) */
    @Excel(name = "行金额(不含税)")
    private BigDecimal amount;

    /** 税率(%) */
    @Excel(name = "税率(%)")
    private BigDecimal taxRate;

    /** 行业属性(JSON): {paper:{width,weight,source,type,rollCount}, paperBag:{ropeSpec,mouthType,bottomType}, product:{size,packageSpec,printingReq}} */
    private Map<String, Object> lineAttrs;

    /** 关联客户订单号 */
    @Excel(name = "关联客户订单号")
    private String sourceOrderCode;

    /** 预计到货日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "预计到货日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expectedDate;

    /** 到货通知单ID(关联qxx_wm_arrival_notice,收货后回写) */
    @Excel(name = "到货通知单ID(关联qxx_wm_arrival_notice,收货后回写)")
    private Long arrivalNoticeId;

    /** 行状态:ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消 */
    @Excel(name = "行状态:ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消")
    private String status;

    /** 已退货数量(主单位) */
    @Excel(name = "已退货数量(主单位)")
    private BigDecimal quantityReturned;

    /** 取消/终止原因 */
    private String cancelReason;

    /** 关闭/终止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closeTime;

    public void setLineId(Long lineId) 
    {
        this.lineId = lineId;
    }

    public Long getLineId() 
    {
        return lineId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
    }

    public void setOrderId(Long orderId) 
    {
        this.orderId = orderId;
    }

    public Long getOrderId() 
    {
        return orderId;
    }

    public void setItemId(Long itemId) 
    {
        this.itemId = itemId;
    }

    public Long getItemId() 
    {
        return itemId;
    }

    public void setItemCode(String itemCode) 
    {
        this.itemCode = itemCode;
    }

    public String getItemCode() 
    {
        return itemCode;
    }

    public void setItemName(String itemName) 
    {
        this.itemName = itemName;
    }

    public String getItemName() 
    {
        return itemName;
    }

    public void setSpecification(String specification) 
    {
        this.specification = specification;
    }

    public String getSpecification() 
    {
        return specification;
    }

    public void setUnitOfMeasure(String unitOfMeasure) 
    {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getUnitOfMeasure() 
    {
        return unitOfMeasure;
    }

    public void setUnitName(String unitName) 
    {
        this.unitName = unitName;
    }

    public String getUnitName() 
    {
        return unitName;
    }

    public void setUnit2(String unit2) 
    {
        this.unit2 = unit2;
    }

    public String getUnit2() 
    {
        return unit2;
    }

    public void setUnit2Name(String unit2Name) 
    {
        this.unit2Name = unit2Name;
    }

    public String getUnit2Name() 
    {
        return unit2Name;
    }

    public void setConversionRate(BigDecimal conversionRate) 
    {
        this.conversionRate = conversionRate;
    }

    public BigDecimal getConversionRate() 
    {
        return conversionRate;
    }

    public void setQuantityOrdered(BigDecimal quantityOrdered) 
    {
        this.quantityOrdered = quantityOrdered;
    }

    public BigDecimal getQuantityOrdered() 
    {
        return quantityOrdered;
    }

    public void setQuantityOrdered2(BigDecimal quantityOrdered2) 
    {
        this.quantityOrdered2 = quantityOrdered2;
    }

    public BigDecimal getQuantityOrdered2() 
    {
        return quantityOrdered2;
    }

    public void setQuantityReceived(BigDecimal quantityReceived) 
    {
        this.quantityReceived = quantityReceived;
    }

    public BigDecimal getQuantityReceived() 
    {
        return quantityReceived;
    }

    public void setQuantityReceived2(BigDecimal quantityReceived2) 
    {
        this.quantityReceived2 = quantityReceived2;
    }

    public BigDecimal getQuantityReceived2() 
    {
        return quantityReceived2;
    }

    public void setUnitPrice(BigDecimal unitPrice) 
    {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getUnitPrice() 
    {
        return unitPrice;
    }

    public void setAmount(BigDecimal amount) 
    {
        this.amount = amount;
    }

    public BigDecimal getAmount() 
    {
        return amount;
    }

    public void setTaxRate(BigDecimal taxRate) 
    {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxRate() 
    {
        return taxRate;
    }

    public void setPaperWidth(String paperWidth)
    {
        // Deprecated: paperWidth migrated to lineAttrs.paper.width (V73)
    }

    public String getPaperWidth()
    {
        return getLineAttr("paper", "width");
    }

    public void setPaperWeight(String paperWeight)
    {
        // Deprecated: paperWeight migrated to lineAttrs.paper.weight (V73)
    }

    public String getPaperWeight()
    {
        return getLineAttr("paper", "weight");
    }

    public void setPaperType(String paperType)
    {
        // Deprecated: paperType migrated to lineAttrs.paper.type (V73)
    }

    public String getPaperType()
    {
        return getLineAttr("paper", "type");
    }

    public void setRollCount(Long rollCount)
    {
        // Deprecated: rollCount migrated to lineAttrs.paper.rollCount (V73)
    }

    public Long getRollCount()
    {
        String val = getLineAttr("paper", "rollCount");
        if (val == null || val.isEmpty()) return null;
        return Long.valueOf(val);
    }

    public Map<String, Object> getLineAttrs()
    {
        return lineAttrs;
    }

    public void setLineAttrs(Map<String, Object> lineAttrs)
    {
        this.lineAttrs = lineAttrs;
    }

    /** 从 lineAttrs 中按分组+属性名取字符串值 */
    @SuppressWarnings("unchecked")
    private String getLineAttr(String group, String key)
    {
        if (lineAttrs == null) return null;
        Object groupObj = lineAttrs.get(group);
        if (!(groupObj instanceof Map)) return null;
        Object val = ((Map<String, Object>) groupObj).get(key);
        return val != null ? val.toString() : null;
    }

    public void setSourceOrderCode(String sourceOrderCode) 
    {
        this.sourceOrderCode = sourceOrderCode;
    }

    public String getSourceOrderCode() 
    {
        return sourceOrderCode;
    }

    public void setExpectedDate(Date expectedDate) 
    {
        this.expectedDate = expectedDate;
    }

    public Date getExpectedDate() 
    {
        return expectedDate;
    }

    public void setArrivalNoticeId(Long arrivalNoticeId) 
    {
        this.arrivalNoticeId = arrivalNoticeId;
    }

    public Long getArrivalNoticeId() 
    {
        return arrivalNoticeId;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setQuantityReturned(BigDecimal quantityReturned)
    {
        this.quantityReturned = quantityReturned;
    }

    public BigDecimal getQuantityReturned()
    {
        return quantityReturned;
    }

    public void setCancelReason(String cancelReason)
    {
        this.cancelReason = cancelReason;
    }

    public String getCancelReason()
    {
        return cancelReason;
    }

    public void setCloseTime(Date closeTime)
    {
        this.closeTime = closeTime;
    }

    public Date getCloseTime()
    {
        return closeTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("lineId", getLineId())
            .append("factoryId", getFactoryId())
            .append("orderId", getOrderId())
            .append("itemId", getItemId())
            .append("itemCode", getItemCode())
            .append("itemName", getItemName())
            .append("specification", getSpecification())
            .append("unitOfMeasure", getUnitOfMeasure())
            .append("unitName", getUnitName())
            .append("unit2", getUnit2())
            .append("unit2Name", getUnit2Name())
            .append("conversionRate", getConversionRate())
            .append("quantityOrdered", getQuantityOrdered())
            .append("quantityOrdered2", getQuantityOrdered2())
            .append("quantityReceived", getQuantityReceived())
            .append("quantityReceived2", getQuantityReceived2())
            .append("unitPrice", getUnitPrice())
            .append("amount", getAmount())
            .append("taxRate", getTaxRate())
            .append("lineAttrs", getLineAttrs())
            .append("sourceOrderCode", getSourceOrderCode())
            .append("expectedDate", getExpectedDate())
            .append("arrivalNoticeId", getArrivalNoticeId())
            .append("status", getStatus())
            .append("quantityReturned", getQuantityReturned())
            .append("cancelReason", getCancelReason())
            .append("closeTime", getCloseTime())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
