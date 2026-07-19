package com.ruoyi.system.service.mes.pur.impl;

import java.util.List;
import com.ruoyi.common.enums.PurOrderStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.mes.pur.PurOrder;
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
        // 行状态强制跟随订单头状态，忽略客户端传入值（防止草稿单上新增行变成已下单）
        syncLineStatusWithOrder(purOrderLine);
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
        // 行状态不能超过订单头状态（行物料/数量等编辑不应擅自推进状态）
        checkLineStatusNotExceedOrder(purOrderLine);
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
     * 强制行状态跟随订单头状态（新增行场景）
     * 忽略客户端传入的 status，统一用订单头当前状态覆盖。
     */
    private void syncLineStatusWithOrder(PurOrderLine purOrderLine)
    {
        PurOrder order = purOrderMapper.selectPurOrderByOrderId(purOrderLine.getOrderId());
        if (order == null) {
            throw new ServiceException("采购订单不存在");
        }
        purOrderLine.setStatus(order.getStatus());
    }

    /**
     * 校验行状态不能超过订单头状态（修改行场景）
     *
     * 本校验只防"用户手工越级"—— 通过 REST edit 接口伪造 status 跳过审批/下单流程。
     * 所以仅对用户可手工传入的推进型状态（DRAFT/APPROVED/ORDERED）做 ordinal 比对。
     * RECEIVING/RECEIVED/CLOSED/CANCEL 均由业务子系统驱动：
     *   - RECEIVING/RECEIVED ← WmItemRecptServiceImpl 收货流程（行必然先于头推进）
     *   - CLOSED             ← terminatePurOrderLine / closePurOrder
     *   - CANCEL             ← cancelPurOrderLine / cancelPurOrder
     * 这些路径都由各自专用接口保证语义（已收数量、终态约束等），放行不校验。
     */
    private void checkLineStatusNotExceedOrder(PurOrderLine purOrderLine)
    {
        PurOrderStatus lineStatus = PurOrderStatus.fromCode(purOrderLine.getStatus());
        if (lineStatus == null) {
            return; // 未传或未知状态不校验
        }
        // 仅对用户手工可设的推进型状态做越级检查
        if (lineStatus != PurOrderStatus.DRAFT
                && lineStatus != PurOrderStatus.APPROVED
                && lineStatus != PurOrderStatus.ORDERED) {
            return;
        }
        PurOrder order = purOrderMapper.selectPurOrderByOrderId(purOrderLine.getOrderId());
        if (order == null) {
            throw new ServiceException("采购订单不存在");
        }
        PurOrderStatus orderStatus = PurOrderStatus.fromCode(order.getStatus());
        if (orderStatus == null) {
            return;
        }
        if (lineStatus.ordinal() > orderStatus.ordinal()) {
            throw new ServiceException(String.format(
                "行状态[%s]不能超过订单头状态[%s]", lineStatus.getInfo(), orderStatus.getInfo()));
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
