package com.ruoyi.system.domain.mes.pur.vo;

import java.util.List;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;

/**
 * 采购订单详情 VO — getInfo 接口专用，聚合订单头 + 订单行
 *
 * @author qixiaoxia
 * @date 2026-07-07
 */
public class PurOrderDetailVO
{
    /** 订单头（纯实体，不含计算字段） */
    private PurOrder order;

    /** 订单行列表 */
    private List<PurOrderLine> lines;

    public PurOrderDetailVO() {}

    public PurOrderDetailVO(PurOrder order, List<PurOrderLine> lines)
    {
        this.order = order;
        this.lines = lines;
    }

    public PurOrder getOrder() { return order; }
    public void setOrder(PurOrder order) { this.order = order; }

    public List<PurOrderLine> getLines() { return lines; }
    public void setLines(List<PurOrderLine> lines) { this.lines = lines; }
}
