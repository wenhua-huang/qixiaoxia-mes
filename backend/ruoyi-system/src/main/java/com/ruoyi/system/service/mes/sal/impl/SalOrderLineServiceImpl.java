package com.ruoyi.system.service.mes.sal.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.mes.sal.SalOrderLine;
import com.ruoyi.system.mapper.mes.sal.SalOrderLineMapper;
import com.ruoyi.system.service.mes.sal.ISalOrderLineService;

/**
 * 销售订单明细行Service实现
 *
 * @author qixiaoxia
 * @date 2026-07-15
 */
@Service
public class SalOrderLineServiceImpl implements ISalOrderLineService
{
    @Autowired
    private SalOrderLineMapper salOrderLineMapper;

    @Override
    public SalOrderLine selectSalOrderLineByLineId(Long lineId)
    {
        return salOrderLineMapper.selectSalOrderLineByLineId(lineId);
    }

    @Override
    public List<SalOrderLine> selectSalOrderLineByOrderId(Long orderId)
    {
        return salOrderLineMapper.selectSalOrderLineByOrderId(orderId);
    }

    @Override
    public int insertSalOrderLine(SalOrderLine line)
    {
        return salOrderLineMapper.insertSalOrderLine(line);
    }

    @Override
    public int updateSalOrderLine(SalOrderLine line)
    {
        return salOrderLineMapper.updateSalOrderLine(line);
    }

    @Override
    public int deleteSalOrderLineByOrderId(Long orderId)
    {
        return salOrderLineMapper.deleteSalOrderLineByOrderId(orderId);
    }

    @Override
    public int deleteSalOrderLineByLineIds(Long[] lineIds)
    {
        return salOrderLineMapper.deleteSalOrderLineByLineIds(lineIds);
    }

    @Override
    public BigDecimal sumProducedQtyByLineId(Long lineId)
    {
        return salOrderLineMapper.sumProducedQtyByLineId(lineId);
    }
}
