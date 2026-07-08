package com.ruoyi.system.domain.mes.pur.vo;

import java.util.List;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;

/**
 * 采购订单详情 VO — getInfo 接口专用，聚合订单头VO + 订单行
 *
 * @author qixiaoxia
 * @date 2026-07-07
 */
public class PurOrderDetailVO
{
    /** 订单头（VO，含计算字段 receivedQuantity） */
    private PurOrderVO order;

    /** 订单行列表 */
    private List<PurOrderLine> lines;

    public PurOrderDetailVO() {}

    public PurOrderDetailVO(PurOrderVO order, List<PurOrderLine> lines)
    {
        this.order = order;
        this.lines = lines;
    }

    public PurOrderVO getOrder() { return order; }
    public void setOrder(PurOrderVO order) { this.order = order; }

    public List<PurOrderLine> getLines() { return lines; }
    public void setLines(List<PurOrderLine> lines) { this.lines = lines; }
}
