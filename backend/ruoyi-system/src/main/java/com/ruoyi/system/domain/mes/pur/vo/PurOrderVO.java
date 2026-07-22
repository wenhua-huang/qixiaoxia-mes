package com.ruoyi.system.domain.mes.pur.vo;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.system.domain.mes.pur.PurOrder;

/**
 * 采购订单头 VO — 含计算字段（由 SQL 子查询直接映射，不落表）
 * totalQuantity/totalAmount 继承自 PurOrder，语义为「原订购冻结值」，取消/关闭不改。
 * 异常流转（取消/部分收货）通过下面的计算字段表达差异。
 *
 * @author qixiaoxia
 * @date 2026-07-07
 */
public class PurOrderVO extends PurOrder
{
    private static final long serialVersionUID = 1L;

    /** 已收货总数量(主单位) — SUM(quantity_received) 全状态 */
    @Excel(name = "已收货总数量(主单位)")
    private BigDecimal receivedQuantity;

    /** 已收货总金额(元) — SUM(quantity_received × unit_price) 全状态 */
    @Excel(name = "已收货总金额(元)")
    private BigDecimal receivedAmount;

    /** 已取消总数量(主单位) — SUM(quantity_ordered WHERE status='CANCEL') */
    @Excel(name = "已取消总数量(主单位)")
    private BigDecimal cancelledQuantity;

    /** 已取消总金额(元) — SUM(amount WHERE status='CANCEL') */
    @Excel(name = "已取消总金额(元)")
    private BigDecimal cancelledAmount;

    public BigDecimal getReceivedQuantity()
    {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity)
    {
        this.receivedQuantity = receivedQuantity;
    }

    public BigDecimal getReceivedAmount()
    {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount)
    {
        this.receivedAmount = receivedAmount;
    }

    public BigDecimal getCancelledQuantity()
    {
        return cancelledQuantity;
    }

    public void setCancelledQuantity(BigDecimal cancelledQuantity)
    {
        this.cancelledQuantity = cancelledQuantity;
    }

    public BigDecimal getCancelledAmount()
    {
        return cancelledAmount;
    }

    public void setCancelledAmount(BigDecimal cancelledAmount)
    {
        this.cancelledAmount = cancelledAmount;
    }
}
