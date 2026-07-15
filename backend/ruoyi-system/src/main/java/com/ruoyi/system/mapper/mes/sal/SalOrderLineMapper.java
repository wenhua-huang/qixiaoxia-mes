package com.ruoyi.system.mapper.mes.sal;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;

/**
 * 销售订单明细行Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
public interface SalOrderLineMapper
{
    public SalOrderLine selectSalOrderLineByLineId(Long lineId);
    public List<SalOrderLine> selectSalOrderLineByOrderId(Long orderId);
    public int insertSalOrderLine(SalOrderLine line);
    public int updateSalOrderLine(SalOrderLine line);
    /** 全量替换时:按订单删全部行 */
    public int deleteSalOrderLineByOrderId(Long orderId);
    public int deleteSalOrderLineByLineIds(Long[] lineIds);

    /**
     * 已转工单数量:聚合 qxx_pro_workorder(排除已取消)。
     * 用于可转量校验与展示,非计数列,实时算。
     */
    public BigDecimal sumProducedQtyByLineId(Long lineId);
}
