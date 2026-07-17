package com.ruoyi.system.service.mes.pur.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pur.PurOrderMapper;
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

    @Autowired
    private PurOrderMapper purOrderMapper;

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
        // 校验：同一订单不允许重复物料
        checkDuplicateMaterial(purOrderLine.getOrderId(), purOrderLine.getItemId(), null);
        purOrderLine.setCreateTime(DateUtils.getNowDate());
        purOrderLine.setCreateBy(SecurityUtils.getUsername());
        int rows = purOrderLineMapper.insertPurOrderLine(purOrderLine);
        recalcOrderTotals(purOrderLine.getOrderId());
        return rows;
    }

    /**
     * 修改采购订单行
     * 
     * @param purOrderLine 采购订单行
     * @return 结果
     */
    @Override
    @Transactional
    public int updatePurOrderLine(PurOrderLine purOrderLine)
    {
        // 校验：修改物料时不能与其他行重复
        checkDuplicateMaterial(purOrderLine.getOrderId(), purOrderLine.getItemId(), purOrderLine.getLineId());
        purOrderLine.setUpdateTime(DateUtils.getNowDate());
        purOrderLine.setUpdateBy(SecurityUtils.getUsername());
        int rows = purOrderLineMapper.updatePurOrderLine(purOrderLine);
        recalcOrderTotals(purOrderLine.getOrderId());
        return rows;
    }

    /**
     * 批量删除采购订单行
     * 
     * @param lineIds 需要删除的采购订单行主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePurOrderLineByLineIds(Long[] lineIds)
    {
        // 先收集受影响的 orderId，删除后重算
        java.util.Set<Long> orderIds = new java.util.HashSet<>();
        for (Long lineId : lineIds) {
            PurOrderLine line = purOrderLineMapper.selectPurOrderLineByLineId(lineId);
            if (line != null && line.getOrderId() != null) {
                orderIds.add(line.getOrderId());
            }
        }
        int rows = purOrderLineMapper.deletePurOrderLineByLineIds(lineIds);
        for (Long orderId : orderIds) {
            recalcOrderTotals(orderId);
        }
        return rows;
    }

    /**
     * 删除采购订单行信息
     *
     * @param lineId 采购订单行主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePurOrderLineByLineId(Long lineId)
    {
        PurOrderLine line = purOrderLineMapper.selectPurOrderLineByLineId(lineId);
        int rows = purOrderLineMapper.deletePurOrderLineByLineId(lineId);
        if (line != null && line.getOrderId() != null) {
            recalcOrderTotals(line.getOrderId());
        }
        return rows;
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

    /**
     * 校验同一订单不允许重复物料
     *
     * @param orderId 订单ID
     * @param itemId  物料ID
     * @param excludeLineId 排除的行ID（修改场景传自身ID，新增场景传null）
     */
    private void checkDuplicateMaterial(Long orderId, Long itemId, Long excludeLineId)
    {
        if (orderId == null || itemId == null) {
            return;
        }
        PurOrderLine query = new PurOrderLine();
        query.setOrderId(orderId);
        query.setItemId(itemId);
        List<PurOrderLine> existing = purOrderLineMapper.selectPurOrderLineList(query);
        for (PurOrderLine line : existing) {
            if (excludeLineId == null || !excludeLineId.equals(line.getLineId())) {
                throw new ServiceException(String.format(
                    "该订单已存在物料[%s]，同一订单不允许添加重复物料", line.getItemCode()));
            }
        }
    }

    /**
     * 重算订单头总数量/总金额
     */
    private void recalcOrderTotals(Long orderId)
    {
        if (orderId != null) {
            purOrderMapper.recalcOrderTotals(orderId);
        }
    }
}
