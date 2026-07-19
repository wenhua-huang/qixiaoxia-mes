package com.ruoyi.system.domain.mes.pur;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 采购订单头对象 qxx_pur_order
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class PurOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 采购订单ID */
    private Long orderId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 采购订单编码(唯一) */
    @Excel(name = "采购订单编码(唯一)")
    private String orderCode;

    /** 采购订单名称 */
    @Excel(name = "采购订单名称")
    private String orderName;

    /** 供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL) */
    @Excel(name = "供应商ID(关联qxx_md_vendor,vendor_type=MATERIAL)")
    private Long vendorId;

    /** 供应商编码 */
    @Excel(name = "供应商编码")
    private String vendorCode;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String vendorName;

    /** 采购类型:PAPER-纸张,AUX-辅料(绳子/胶水/油墨),PACK-包材(纸箱),OTHER-其他 */
    @Excel(name = "采购类型:PAPER-纸张,AUX-辅料(绳子/胶水/油墨),PACK-包材(纸箱),OTHER-其他")
    private String purchaseType;

    /** 下单日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "下单日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date orderDate;

    /** 预计到货日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "预计到货日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date expectedDate;

    /** 采购员(申购人) */
    @Excel(name = "采购员(申购人)")
    private String purchaser;

    /** 审批人 */
    @Excel(name = "审批人")
    private String approver;

    /** 采购总数量(主单位) */
    @Excel(name = "采购总数量(主单位)")
    private BigDecimal totalQuantity;

    /** 采购总金额(元) */
    @Excel(name = "采购总金额(元)")
    private BigDecimal totalAmount;

    /** 币种:CNY-人民币,USD-美元 */
    @Excel(name = "币种:CNY-人民币,USD-美元")
    private String currency;

    /** 关联客户订单号(如PO#ORD66003MT) */
    @Excel(name = "关联客户订单号(如PO#ORD66003MT)")
    private String sourceOrderCode;

    /** 关联工单ID */
    private Long workorderId;

    /** 关联工单编码 */
    private String workorderCode;

    /** 状态:DRAFT-草稿,APPROVED-已审批,ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消 */
    @Excel(name = "状态:DRAFT-草稿,APPROVED-已审批,ORDERED-已下单,RECEIVING-收货中,RECEIVED-已收货,CLOSED-已关闭,CANCEL-已取消")
    private String status;

    /** 取消原因 */
    private String cancelReason;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    /** 取消人 */
    private String cancelBy;

    /** 关闭原因(强制关闭时填写) */
    private String closeReason;

    /** 关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closeTime;

    /** 关闭人 */
    private String closeBy;

    public void setOrderId(Long orderId) 
    {
        this.orderId = orderId;
    }

    public Long getOrderId() 
    {
        return orderId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
    }

    public void setOrderCode(String orderCode) 
    {
        this.orderCode = orderCode;
    }

    public String getOrderCode() 
    {
        return orderCode;
    }

    public void setOrderName(String orderName) 
    {
        this.orderName = orderName;
    }

    public String getOrderName() 
    {
        return orderName;
    }

    public void setVendorId(Long vendorId) 
    {
        this.vendorId = vendorId;
    }

    public Long getVendorId() 
    {
        return vendorId;
    }

    public void setVendorCode(String vendorCode) 
    {
        this.vendorCode = vendorCode;
    }

    public String getVendorCode() 
    {
        return vendorCode;
    }

    public void setVendorName(String vendorName) 
    {
        this.vendorName = vendorName;
    }

    public String getVendorName() 
    {
        return vendorName;
    }

    public void setPurchaseType(String purchaseType) 
    {
        this.purchaseType = purchaseType;
    }

    public String getPurchaseType() 
    {
        return purchaseType;
    }

    public void setOrderDate(Date orderDate) 
    {
        this.orderDate = orderDate;
    }

    public Date getOrderDate() 
    {
        return orderDate;
    }

    public void setExpectedDate(Date expectedDate) 
    {
        this.expectedDate = expectedDate;
    }

    public Date getExpectedDate() 
    {
        return expectedDate;
    }

    public void setPurchaser(String purchaser) 
    {
        this.purchaser = purchaser;
    }

    public String getPurchaser() 
    {
        return purchaser;
    }

    public void setApprover(String approver) 
    {
        this.approver = approver;
    }

    public String getApprover() 
    {
        return approver;
    }

    public void setTotalQuantity(BigDecimal totalQuantity)
    {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalQuantity()
    {
        return totalQuantity;
    }

    public void setTotalAmount(BigDecimal totalAmount) 
    {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalAmount() 
    {
        return totalAmount;
    }

    public void setCurrency(String currency) 
    {
        this.currency = currency;
    }

    public String getCurrency() 
    {
        return currency;
    }

    public void setSourceOrderCode(String sourceOrderCode)
    {
        this.sourceOrderCode = sourceOrderCode;
    }

    public String getSourceOrderCode()
    {
        return sourceOrderCode;
    }

    public void setWorkorderId(Long workorderId)
    {
        this.workorderId = workorderId;
    }

    public Long getWorkorderId()
    {
        return workorderId;
    }

    public void setWorkorderCode(String workorderCode)
    {
        this.workorderCode = workorderCode;
    }

    public String getWorkorderCode()
    {
        return workorderCode;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setCancelReason(String cancelReason)
    {
        this.cancelReason = cancelReason;
    }

    public String getCancelReason()
    {
        return cancelReason;
    }

    public void setCancelTime(Date cancelTime)
    {
        this.cancelTime = cancelTime;
    }

    public Date getCancelTime()
    {
        return cancelTime;
    }

    public void setCancelBy(String cancelBy)
    {
        this.cancelBy = cancelBy;
    }

    public String getCancelBy()
    {
        return cancelBy;
    }

    public void setCloseReason(String closeReason)
    {
        this.closeReason = closeReason;
    }

    public String getCloseReason()
    {
        return closeReason;
    }

    public void setCloseTime(Date closeTime)
    {
        this.closeTime = closeTime;
    }

    public Date getCloseTime()
    {
        return closeTime;
    }

    public void setCloseBy(String closeBy)
    {
        this.closeBy = closeBy;
    }

    public String getCloseBy()
    {
        return closeBy;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("orderId", getOrderId())
            .append("factoryId", getFactoryId())
            .append("orderCode", getOrderCode())
            .append("orderName", getOrderName())
            .append("vendorId", getVendorId())
            .append("vendorCode", getVendorCode())
            .append("vendorName", getVendorName())
            .append("purchaseType", getPurchaseType())
            .append("orderDate", getOrderDate())
            .append("expectedDate", getExpectedDate())
            .append("purchaser", getPurchaser())
            .append("approver", getApprover())
            .append("totalQuantity", getTotalQuantity())
            .append("totalAmount", getTotalAmount())
            .append("currency", getCurrency())
            .append("sourceOrderCode", getSourceOrderCode())
            .append("workorderId", getWorkorderId())
            .append("workorderCode", getWorkorderCode())
            .append("status", getStatus())
            .append("cancelReason", getCancelReason())
            .append("cancelTime", getCancelTime())
            .append("cancelBy", getCancelBy())
            .append("closeReason", getCloseReason())
            .append("closeTime", getCloseTime())
            .append("closeBy", getCloseBy())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
