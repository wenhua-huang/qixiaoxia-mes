package com.ruoyi.system.domain.mes.sal;

import java.util.List;

/**
 * 销售订单创建/修改请求（含订单头 + 明细行）
 * 遵循 CLAUDE.md:主从表单接口封装,前端一次提交,后端一个事务落库
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
public class SalOrderCreateRequest
{
    /** 销售订单头 */
    private SalOrder order;

    /** 销售订单明细行列表 */
    private List<SalOrderLine> lines;

    public SalOrder getOrder()
    {
        return order;
    }

    public void setOrder(SalOrder order)
    {
        this.order = order;
    }

    public List<SalOrderLine> getLines()
    {
        return lines;
    }

    public void setLines(List<SalOrderLine> lines)
    {
        this.lines = lines;
    }
}
