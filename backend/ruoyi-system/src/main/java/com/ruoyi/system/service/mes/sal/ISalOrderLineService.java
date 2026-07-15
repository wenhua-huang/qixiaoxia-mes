package com.ruoyi.system.service.mes.sal;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;

/**
 * 销售订单明细行Service接口
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
public interface ISalOrderLineService
{
    public SalOrderLine selectSalOrderLineByLineId(Long lineId);
    public List<SalOrderLine> selectSalOrderLineByOrderId(Long orderId);
    public int insertSalOrderLine(SalOrderLine line);
    public int updateSalOrderLine(SalOrderLine line);
    public int deleteSalOrderLineByOrderId(Long orderId);
    public int deleteSalOrderLineByLineIds(Long[] lineIds);

    /** 已转工单数量(聚合 qxx_pro_workorder,排除已取消) */
    public BigDecimal sumProducedQtyByLineId(Long lineId);
}
