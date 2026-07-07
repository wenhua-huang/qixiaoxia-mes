package com.ruoyi.system.service.mes.pur.impl;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pur.PurOrderMapper;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.pur.vo.PurOrderVO;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

/**
 * 采购订单头Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class PurOrderServiceImpl implements IPurOrderService 
{
    @Autowired
    private PurOrderMapper purOrderMapper;

    @Autowired
    private IPurOrderLineService purOrderLineService;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    /**
     * 查询采购订单头
     * 
     * @param orderId 采购订单头主键
     * @return 采购订单头
     */
    @Override
    public PurOrder selectPurOrderByOrderId(Long orderId)
    {
        return purOrderMapper.selectPurOrderByOrderId(orderId);
    }

    /**
     * 查询采购订单头列表
     * 
     * @param purOrder 采购订单头
     * @return 采购订单头
     */
    @Override
    public List<PurOrderVO> selectPurOrderList(PurOrder purOrder)
    {
        return purOrderMapper.selectPurOrderList(purOrder);
    }

    /**
     * 新增采购订单头
     * 
     * @param purOrder 采购订单头
     * @return 结果
     */
    @Override
    @Transactional
    public int insertPurOrder(PurOrder purOrder)
    {
        // 订单编码自动生成
        if (StringUtils.isEmpty(purOrder.getOrderCode())) {
            purOrder.setOrderCode(autoCodeGenerator.genSerialCode("PUR_ORDER_CODE", ""));
        }
        purOrder.setCreateTime(DateUtils.getNowDate());
        purOrder.setCreateBy(SecurityUtils.getUsername());
        return purOrderMapper.insertPurOrder(purOrder);
    }

    /**
     * 修改采购订单头
     * 
     * @param purOrder 采购订单头
     * @return 结果
     */
    @Override
    public int updatePurOrder(PurOrder purOrder)
    {
        purOrder.setUpdateTime(DateUtils.getNowDate());
        purOrder.setUpdateBy(SecurityUtils.getUsername());
        return purOrderMapper.updatePurOrder(purOrder);
    }

    /**
     * 批量删除采购订单头（级联删除行）
     *
     * @param orderIds 需要删除的采购订单头主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePurOrderByOrderIds(Long[] orderIds)
    {
        for (Long orderId : orderIds) {
            purOrderLineService.deletePurOrderLineByOrderId(orderId);
        }
        return purOrderMapper.deletePurOrderByOrderIds(orderIds);
    }

    /**
     * 删除采购订单头信息（级联删除行）
     *
     * @param orderId 采购订单头主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePurOrderByOrderId(Long orderId)
    {
        purOrderLineService.deletePurOrderLineByOrderId(orderId);
        return purOrderMapper.deletePurOrderByOrderId(orderId);
    }

    /**
     * 审批采购订单（DRAFT → APPROVED）
     * 校验：status == DRAFT，自动写入审批人
     */
    @Override
    @Transactional
    public int approvePurOrder(Long orderId)
    {
        PurOrder order = purOrderMapper.selectPurOrderByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!"DRAFT".equals(order.getStatus())) {
            throw new RuntimeException("只有草稿状态的采购订单才能审批，当前状态：" + order.getStatus());
        }
        order.setStatus("APPROVED");
        order.setApprover(SecurityUtils.getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        order.setUpdateBy(SecurityUtils.getUsername());
        return purOrderMapper.updatePurOrder(order);
    }

    /**
     * 下达采购订单（APPROVED → ORDERED）
     * 校验：status == APPROVED，自动设置下单日期，所有行状态 → ORDERED
     */
    @Override
    @Transactional
    public int orderPurOrder(Long orderId)
    {
        PurOrder order = purOrderMapper.selectPurOrderByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!"APPROVED".equals(order.getStatus())) {
            throw new RuntimeException("只有已审批的采购订单才能下达，当前状态：" + order.getStatus());
        }
        // 更新头：状态 + 下单日期
        order.setStatus("ORDERED");
        order.setOrderDate(DateUtils.getNowDate());
        order.setUpdateTime(DateUtils.getNowDate());
        order.setUpdateBy(SecurityUtils.getUsername());
        int result = purOrderMapper.updatePurOrder(order);
        // 批量更新所有行状态 → ORDERED
        PurOrderLine queryLine = new PurOrderLine();
        queryLine.setOrderId(orderId);
        List<PurOrderLine> lines = purOrderLineService.selectPurOrderLineList(queryLine);
        for (PurOrderLine line : lines) {
            line.setStatus("ORDERED");
            line.setUpdateTime(DateUtils.getNowDate());
            line.setUpdateBy(SecurityUtils.getUsername());
            purOrderLineService.updatePurOrderLine(line);
        }
        return result;
    }

    /**
     * 关闭采购订单（RECEIVED → CLOSED）
     * 校验：status == RECEIVED，全部行已收完
     */
    @Override
    @Transactional
    public int closePurOrder(Long orderId)
    {
        PurOrder order = purOrderMapper.selectPurOrderByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!"RECEIVED".equals(order.getStatus())) {
            throw new RuntimeException("只有已收货的采购订单才能关闭，当前状态：" + order.getStatus());
        }
        // 校验全部行已收完
        PurOrderLine queryLine = new PurOrderLine();
        queryLine.setOrderId(orderId);
        List<PurOrderLine> lines = purOrderLineService.selectPurOrderLineList(queryLine);
        for (PurOrderLine line : lines) {
            BigDecimal received = line.getQuantityReceived() != null ? line.getQuantityReceived() : BigDecimal.ZERO;
            BigDecimal ordered = line.getQuantityOrdered() != null ? line.getQuantityOrdered() : BigDecimal.ZERO;
            if (received.compareTo(ordered) < 0) {
                throw new RuntimeException("存在未收完的物料行(" + line.getItemName()
                    + ")，已收" + received + "/订购" + ordered);
            }
        }
        // 更新头 + 所有行 → CLOSED
        order.setStatus("CLOSED");
        order.setUpdateTime(DateUtils.getNowDate());
        order.setUpdateBy(SecurityUtils.getUsername());
        int result = purOrderMapper.updatePurOrder(order);
        for (PurOrderLine line : lines) {
            line.setStatus("CLOSED");
            line.setUpdateTime(DateUtils.getNowDate());
            line.setUpdateBy(SecurityUtils.getUsername());
            purOrderLineService.updatePurOrderLine(line);
        }
        return result;
    }
}
