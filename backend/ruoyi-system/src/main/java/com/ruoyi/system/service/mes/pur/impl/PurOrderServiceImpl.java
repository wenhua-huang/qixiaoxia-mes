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
import com.ruoyi.common.enums.PurOrderStatus;

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
    public PurOrderVO selectPurOrderByOrderId(Long orderId)
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
        // 采购人默认当前登录用户
        if (StringUtils.isEmpty(purOrder.getPurchaser())) {
            purOrder.setPurchaser(SecurityUtils.getUsername());
        }
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
        if (!PurOrderStatus.DRAFT.is(order.getStatus())) {
            throw new RuntimeException("只有草稿状态的采购订单才能审批，当前状态：" + order.getStatus());
        }
        order.setStatus(PurOrderStatus.APPROVED.getCode());
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
        if (!PurOrderStatus.APPROVED.is(order.getStatus())) {
            throw new RuntimeException("只有已审批的采购订单才能下达，当前状态：" + order.getStatus());
        }
        // 更新头：状态 + 下单日期
        order.setStatus(PurOrderStatus.ORDERED.getCode());
        order.setOrderDate(DateUtils.getNowDate());
        order.setUpdateTime(DateUtils.getNowDate());
        order.setUpdateBy(SecurityUtils.getUsername());
        int result = purOrderMapper.updatePurOrder(order);
        // 批量更新所有行状态 → ORDERED
        PurOrderLine queryLine = new PurOrderLine();
        queryLine.setOrderId(orderId);
        List<PurOrderLine> lines = purOrderLineService.selectPurOrderLineList(queryLine);
        for (PurOrderLine line : lines) {
            line.setStatus(PurOrderStatus.ORDERED.getCode());
            line.setUpdateTime(DateUtils.getNowDate());
            line.setUpdateBy(SecurityUtils.getUsername());
            purOrderLineService.updatePurOrderLine(line);
        }
        return result;
    }

    /**
     * 关闭采购订单（RECEIVED -> CLOSED 正常关闭，RECEIVING -> CLOSED 强制关闭）
     * 校验：status ∈ {RECEIVED, RECEIVING}，所有行到达终态(RECEIVED/CLOSED/CANCEL)
     * 强制关闭(有CLOSED/CANCEL行)时需填 closeReason
     */
    @Override
    @Transactional
    public int closePurOrder(Long orderId, String closeReason)
    {
        PurOrder order = purOrderMapper.selectPurOrderByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!PurOrderStatus.RECEIVED.is(order.getStatus())
                && !PurOrderStatus.RECEIVING.is(order.getStatus())) {
            throw new RuntimeException("只有已收货或收货中的采购订单才能关闭，当前状态：" + order.getStatus());
        }
        // 校验所有行到达终态
        PurOrderLine queryLine = new PurOrderLine();
        queryLine.setOrderId(orderId);
        List<PurOrderLine> lines = purOrderLineService.selectPurOrderLineList(queryLine);
        if (lines.isEmpty()) {
            throw new RuntimeException("采购订单没有订单行，无法关闭");
        }
        boolean hasForceClose = false;
        for (PurOrderLine line : lines) {
            if (!PurOrderStatus.RECEIVED.is(line.getStatus())
                    && !PurOrderStatus.CLOSED.is(line.getStatus())
                    && !PurOrderStatus.CANCEL.is(line.getStatus())) {
                throw new RuntimeException("存在未完成的物料行(" + line.getItemName()
                    + ")，请先终止收货或取消该行");
            }
            if (PurOrderStatus.CLOSED.is(line.getStatus()) || PurOrderStatus.CANCEL.is(line.getStatus())) {
                hasForceClose = true;
            }
        }
        if (hasForceClose && StringUtils.isEmpty(closeReason)) {
            throw new RuntimeException("存在终止/取消的行，请填写关闭原因");
        }
        // 更新头 -> CLOSED
        order.setStatus(PurOrderStatus.CLOSED.getCode());
        if (StringUtils.isNotEmpty(closeReason)) {
            order.setCloseReason(closeReason);
        }
        order.setCloseTime(DateUtils.getNowDate());
        order.setCloseBy(SecurityUtils.getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        order.setUpdateBy(SecurityUtils.getUsername());
        int result = purOrderMapper.updatePurOrder(order);
        // RECEIVED 行 -> CLOSED；CANCEL/CLOSED 行不动
        for (PurOrderLine line : lines) {
            if (PurOrderStatus.RECEIVED.is(line.getStatus())) {
                line.setStatus(PurOrderStatus.CLOSED.getCode());
                line.setCloseTime(DateUtils.getNowDate());
                line.setUpdateTime(DateUtils.getNowDate());
                line.setUpdateBy(SecurityUtils.getUsername());
                purOrderLineService.updatePurOrderLine(line);
            }
        }
        return result;
    }

    /**
     * 取消采购订单（DRAFT/APPROVED/ORDERED -> CANCEL）
     * 校验：所有行已收货数量为0
     */
    @Override
    @Transactional
    public int cancelPurOrder(Long orderId, String cancelReason)
    {
        PurOrder order = purOrderMapper.selectPurOrderByOrderId(orderId);
        if (order == null) {
            throw new RuntimeException("采购订单不存在");
        }
        if (!PurOrderStatus.DRAFT.is(order.getStatus())
                && !PurOrderStatus.APPROVED.is(order.getStatus())
                && !PurOrderStatus.ORDERED.is(order.getStatus())) {
            throw new RuntimeException("只有草稿/已审批/已下单的采购订单才能取消，当前状态：" + order.getStatus());
        }
        // 校验所有行已收货数量为0
        PurOrderLine queryLine = new PurOrderLine();
        queryLine.setOrderId(orderId);
        List<PurOrderLine> lines = purOrderLineService.selectPurOrderLineList(queryLine);
        for (PurOrderLine line : lines) {
            BigDecimal received = line.getQuantityReceived() != null ? line.getQuantityReceived() : BigDecimal.ZERO;
            if (received.compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("物料行(" + line.getItemName()
                    + ")已有收货记录(" + received + ")，不能取消，请改用终止收货");
            }
        }
        // 更新头 -> CANCEL
        order.setStatus(PurOrderStatus.CANCEL.getCode());
        order.setCancelReason(cancelReason);
        order.setCancelTime(DateUtils.getNowDate());
        order.setCancelBy(SecurityUtils.getUsername());
        order.setUpdateTime(DateUtils.getNowDate());
        order.setUpdateBy(SecurityUtils.getUsername());
        int result = purOrderMapper.updatePurOrder(order);
        // 所有行 -> CANCEL
        for (PurOrderLine line : lines) {
            line.setStatus(PurOrderStatus.CANCEL.getCode());
            line.setCancelReason(cancelReason);
            line.setCloseTime(DateUtils.getNowDate());
            line.setUpdateTime(DateUtils.getNowDate());
            line.setUpdateBy(SecurityUtils.getUsername());
            purOrderLineService.updatePurOrderLine(line);
        }
        return result;
    }

    /**
     * 取消采购订单行（ORDERED/RECEIVING -> CANCEL）
     * 校验：已收货数量为0
     */
    @Override
    @Transactional
    public int cancelPurOrderLine(Long lineId, String cancelReason)
    {
        PurOrderLine line = purOrderLineService.selectPurOrderLineByLineId(lineId);
        if (line == null) {
            throw new RuntimeException("采购订单行不存在");
        }
        if (!PurOrderStatus.ORDERED.is(line.getStatus())
                && !PurOrderStatus.RECEIVING.is(line.getStatus())) {
            throw new RuntimeException("只有已下单/收货中的行才能取消，当前状态：" + line.getStatus());
        }
        BigDecimal received = line.getQuantityReceived() != null ? line.getQuantityReceived() : BigDecimal.ZERO;
        if (received.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("该行已有收货记录(" + received + ")，不能取消，请改用终止收货");
        }
        // 行 -> CANCEL
        line.setStatus(PurOrderStatus.CANCEL.getCode());
        line.setCancelReason(cancelReason);
        line.setCloseTime(DateUtils.getNowDate());
        line.setUpdateTime(DateUtils.getNowDate());
        line.setUpdateBy(SecurityUtils.getUsername());
        int result = purOrderLineService.updatePurOrderLine(line);
        // 联动检查：若所有行都为 CANCEL 且全部 qtyReceived==0，头自动转 CANCEL
        checkAndAutoCancelOrder(line.getOrderId(), cancelReason);
        return result;
    }

    /**
     * 终止收货采购订单行（RECEIVING -> CLOSED）
     * 校验：已收货数量 > 0 且 < 订购数量
     */
    @Override
    @Transactional
    public int terminatePurOrderLine(Long lineId, String closeReason)
    {
        PurOrderLine line = purOrderLineService.selectPurOrderLineByLineId(lineId);
        if (line == null) {
            throw new RuntimeException("采购订单行不存在");
        }
        if (!PurOrderStatus.RECEIVING.is(line.getStatus())) {
            throw new RuntimeException("只有收货中的行才能终止收货，当前状态：" + line.getStatus());
        }
        BigDecimal received = line.getQuantityReceived() != null ? line.getQuantityReceived() : BigDecimal.ZERO;
        BigDecimal ordered = line.getQuantityOrdered() != null ? line.getQuantityOrdered() : BigDecimal.ZERO;
        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("该行没有收货记录，请改用取消");
        }
        if (received.compareTo(ordered) >= 0) {
            throw new RuntimeException("该行已全部收完，无需终止收货");
        }
        // 行 -> CLOSED（保留已收数量，放弃未收数量）
        line.setStatus(PurOrderStatus.CLOSED.getCode());
        line.setCancelReason(closeReason);
        line.setCloseTime(DateUtils.getNowDate());
        line.setUpdateTime(DateUtils.getNowDate());
        line.setUpdateBy(SecurityUtils.getUsername());
        return purOrderLineService.updatePurOrderLine(line);
    }

    /**
     * 联动检查：若订单所有行都为 CANCEL 且全部 qtyReceived==0，头自动转 CANCEL
     */
    private void checkAndAutoCancelOrder(Long orderId, String cancelReason)
    {
        PurOrder order = purOrderMapper.selectPurOrderByOrderId(orderId);
        if (order == null) return;
        if (!PurOrderStatus.ORDERED.is(order.getStatus())
                && !PurOrderStatus.RECEIVING.is(order.getStatus())) return;
        PurOrderLine queryLine = new PurOrderLine();
        queryLine.setOrderId(orderId);
        List<PurOrderLine> lines = purOrderLineService.selectPurOrderLineList(queryLine);
        if (lines.isEmpty()) return;
        boolean allCancel = true;
        for (PurOrderLine line : lines) {
            if (!PurOrderStatus.CANCEL.is(line.getStatus())) {
                allCancel = false;
                break;
            }
        }
        if (allCancel) {
            order.setStatus(PurOrderStatus.CANCEL.getCode());
            order.setCancelReason(cancelReason);
            order.setCancelTime(DateUtils.getNowDate());
            order.setCancelBy(SecurityUtils.getUsername());
            order.setUpdateTime(DateUtils.getNowDate());
            order.setUpdateBy(SecurityUtils.getUsername());
            purOrderMapper.updatePurOrder(order);
        }
    }
}
