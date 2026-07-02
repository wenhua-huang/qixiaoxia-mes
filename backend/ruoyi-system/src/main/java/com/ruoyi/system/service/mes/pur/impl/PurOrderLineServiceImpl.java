package com.ruoyi.system.service.mes.pur.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pur.PurOrderLineMapper;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;

/**
 * 采购订单行Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class PurOrderLineServiceImpl implements IPurOrderLineService 
{
    @Autowired
    private PurOrderLineMapper purOrderLineMapper;

    /**
     * 查询采购订单行
     * 
     * @param lineId 采购订单行主键
     * @return 采购订单行
     */
    @Override
    public PurOrderLine selectPurOrderLineByLineId(Long lineId)
    {
        return purOrderLineMapper.selectPurOrderLineByLineId(lineId);
    }

    /**
     * 查询采购订单行列表
     * 
     * @param purOrderLine 采购订单行
     * @return 采购订单行
     */
    @Override
    public List<PurOrderLine> selectPurOrderLineList(PurOrderLine purOrderLine)
    {
        return purOrderLineMapper.selectPurOrderLineList(purOrderLine);
    }

    /**
     * 新增采购订单行
     * 
     * @param purOrderLine 采购订单行
     * @return 结果
     */
    @Override
    @Transactional
    public int insertPurOrderLine(PurOrderLine purOrderLine)
    {
        purOrderLine.setCreateTime(DateUtils.getNowDate());
        purOrderLine.setCreateBy(SecurityUtils.getUsername());
        return purOrderLineMapper.insertPurOrderLine(purOrderLine);
    }

    /**
     * 修改采购订单行
     * 
     * @param purOrderLine 采购订单行
     * @return 结果
     */
    @Override
    public int updatePurOrderLine(PurOrderLine purOrderLine)
    {
        purOrderLine.setUpdateTime(DateUtils.getNowDate());
        purOrderLine.setUpdateBy(SecurityUtils.getUsername());
        return purOrderLineMapper.updatePurOrderLine(purOrderLine);
    }

    /**
     * 批量删除采购订单行
     * 
     * @param lineIds 需要删除的采购订单行主键
     * @return 结果
     */
    @Override
    public int deletePurOrderLineByLineIds(Long[] lineIds)
    {
        return purOrderLineMapper.deletePurOrderLineByLineIds(lineIds);
    }

    /**
     * 删除采购订单行信息
     *
     * @param lineId 采购订单行主键
     * @return 结果
     */
    @Override
    public int deletePurOrderLineByLineId(Long lineId)
    {
        return purOrderLineMapper.deletePurOrderLineByLineId(lineId);
    }

    /**
     * 按订单ID删除所有行（级联删除）
     *
     * @param orderId 采购订单ID
     * @return 结果
     */
    @Override
    public int deletePurOrderLineByOrderId(Long orderId)
    {
        return purOrderLineMapper.deletePurOrderLineByOrderId(orderId);
    }
}
