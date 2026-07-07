package com.ruoyi.system.domain.mes.pur.vo;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.system.domain.mes.pur.PurOrder;

/**
 * 采购订单头 VO — 含计算字段 receivedQuantity（由 SQL 子查询或 MapStruct 后补充）
 *
 * @author qixiaoxia
 * @date 2026-07-07
 */
public class PurOrderVO extends PurOrder
{
    private static final long serialVersionUID = 1L;

    /** 已收货总数量(主单位) — SQL computed from qxx_wm_item_recpt_line */
    @Excel(name = "已收货总数量(主单位)")
    private BigDecimal receivedQuantity;

    public BigDecimal getReceivedQuantity()
    {
        return receivedQuantity;
    }

    public void setReceivedQuantity(BigDecimal receivedQuantity)
    {
        this.receivedQuantity = receivedQuantity;
    }
}
