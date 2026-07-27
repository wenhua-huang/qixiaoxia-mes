package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.enums.WmProductSalesConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesBox;
import com.ruoyi.system.domain.mes.wm.WmProductSalesShipment;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesBoxMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesShipmentMapper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IWmProductSalesShipmentService;

/**
 * 销售出库-发运单业务层（多次发运 + 装箱关联 + 签收回单）
 *
 * <p>发运单状态机：SHIPPING(待发运) → IN_TRANSIT(在途) → RECEIVED(已签收)；CANCELED(已取消)
 * <p>头表 ship_status：UN_SHIPPED → PARTIAL_SHIPPED → SHIPPED → RECEIVED
 *
 * <p>并发安全：createShipment/delete/receive/cancel 均走 Redis 锁 wm:salesout:lock:{salesId} + TransactionTemplate，
 * 防止并发发运导致头表 shipped_quantity 丢失更新。
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
@Service
public class WmProductSalesShipmentServiceImpl implements IWmProductSalesShipmentService
{
    @Autowired private WmProductSalesShipmentMapper shipmentMapper;
    @Autowired private WmProductSalesBoxMapper boxMapper;
    @Autowired private WmProductSalesMapper salesMapper;
    @Autowired private AutoCodeGenerator autoCodeGenerator;
    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    // ════════════════════ 基础查询 ════════════════════

    @Override
    public List<WmProductSalesShipment> selectWmProductSalesShipmentList(WmProductSalesShipment entity) {
        return shipmentMapper.selectWmProductSalesShipmentList(entity);
    }

    @Override
    public List<WmProductSalesShipment> selectWmProductSalesShipmentAll() {
        return shipmentMapper.selectWmProductSalesShipmentAll();
    }

    @Override
    public WmProductSalesShipment selectWmProductSalesShipmentByShipmentId(Long shipmentId) {
        WmProductSalesShipment ship = shipmentMapper.selectWmProductSalesShipmentByShipmentId(shipmentId);
        if (ship != null) {
            ship.setBoxes(boxMapper.selectBoxesByShipmentId(shipmentId));
        }
        return ship;
    }

    @Override
    public List<WmProductSalesShipment> selectShipmentsBySalesId(Long salesId) {
        return shipmentMapper.selectShipmentsBySalesId(salesId);
    }

    // ════════════════════ 新增发运（核心） ════════════════════

    @Override
    public int createShipment(WmProductSalesShipment entity) {
        if (entity.getSalesId() == null) throw new ServiceException("出库单ID不能为空");
        Long salesId = entity.getSalesId();
        lockTemplate.execute("wm:salesout:lock:" + salesId, 10,
                () -> txTemplate.execute(status -> doCreateShipment(entity, salesId)));
        return 1;
    }

    private Long doCreateShipment(WmProductSalesShipment entity, Long salesId) {
        WmProductSales header = salesMapper.selectWmProductSalesBySalesId(salesId);
        if (header == null) throw new ServiceException("出库单不存在");
        if (!WmProductSalesConstants.isShippable(header.getStatus())) {
            throw new ServiceException("当前状态[" + header.getStatus() + "]不允许发运，请先完成过账出库");
        }
        if (!WmProductSalesConstants.isShippableShipStatus(header.getShipStatus())) {
            throw new ServiceException("发运已完成[" + header.getShipStatus() + "]，不可再发运");
        }
        // 编码 + 初始状态
        if (entity.getShipmentCode() == null || entity.getShipmentCode().isEmpty()) {
            entity.setShipmentCode(autoCodeGenerator.genSerialCode(WmProductSalesConstants.CODE_RULE_SHIP, ""));
        }
        entity.setStatus(WmProductSalesConstants.SHIPMENT_STATUS_IN_TRANSIT);
        if (entity.getActualShipDate() == null) entity.setActualShipDate(new Date());
        // 关联装箱：校验箱可用 + 计算本次发运量/箱数
        List<WmProductSalesBox> linkedBoxes = resolveLinkedBoxes(entity, salesId);
        if (linkedBoxes.isEmpty()) {
            throw new ServiceException("本次发运未关联任何已装箱，请先在「装箱明细」完成装箱或勾选本次要发运的箱");
        }
        BigDecimal shippedQty = sumBoxQuantity(linkedBoxes);
        if (shippedQty.signum() <= 0) {
            throw new ServiceException("本次发运数量必须大于 0");
        }
        entity.setShippedQuantity(shippedQty);
        entity.setBoxCount((long) linkedBoxes.size());
        entity.setCreateTime(DateUtils.getNowDate());
        entity.setCreateBy(SecurityUtils.getUsername());
        shipmentMapper.insertWmProductSalesShipment(entity);
        // 回写箱：shipment_id + status=SHIPPED
        markBoxesShipped(linkedBoxes, entity.getShipmentId());
        // 回写头表：shipped_quantity 累加 + 推导 ship_status
        updateHeaderAfterShip(header, shippedQty);
        return entity.getShipmentId();
    }

    /** 解析本次关联装箱（entity.boxes 非空则用之；否则按出库单所有 PACKED 箱全发） */
    private List<WmProductSalesBox> resolveLinkedBoxes(WmProductSalesShipment entity, Long salesId) {
        List<WmProductSalesBox> boxes = entity.getBoxes();
        if (boxes != null && !boxes.isEmpty()) {
            List<WmProductSalesBox> linked = new ArrayList<>();
            for (WmProductSalesBox b : boxes) {
                if (b.getBoxId() == null) continue;
                WmProductSalesBox exist = boxMapper.selectWmProductSalesBoxByBoxId(b.getBoxId());
                if (exist == null) throw new ServiceException("箱[" + b.getBoxNo() + "]不存在");
                if (!exist.getSalesId().equals(salesId)) {
                    throw new ServiceException("箱[" + exist.getBoxNo() + "]不属于本出库单");
                }
                if (WmProductSalesConstants.BOX_STATUS_SHIPPED.equals(exist.getStatus())) {
                    throw new ServiceException("箱[" + exist.getBoxNo() + "]已发运，不可重复发运");
                }
                linked.add(exist);
            }
            return linked;
        }
        // 未指定箱：取该单所有 PACKED 箱
        List<WmProductSalesBox> all = boxMapper.selectBoxesBySalesId(salesId);
        List<WmProductSalesBox> packed = new ArrayList<>();
        for (WmProductSalesBox b : all) {
            if (WmProductSalesConstants.BOX_STATUS_PACKED.equals(b.getStatus())) packed.add(b);
        }
        return packed;
    }

    private BigDecimal sumBoxQuantity(List<WmProductSalesBox> boxes) {
        BigDecimal sum = BigDecimal.ZERO;
        for (WmProductSalesBox b : boxes) {
            if (b.getQuantity() != null) sum = sum.add(b.getQuantity());
        }
        return sum;
    }

    private void markBoxesShipped(List<WmProductSalesBox> boxes, Long shipmentId) {
        Date now = DateUtils.getNowDate();
        String user = SecurityUtils.getUsername();
        for (WmProductSalesBox b : boxes) {
            boxMapper.markShipped(b.getBoxId(), shipmentId, user, now);
        }
    }

    /** 累加 shipped_quantity 并推导 ship_status；不发运不扣库存（过账已扣） */
    private void updateHeaderAfterShip(WmProductSales header, BigDecimal shippedThisTime) {
        BigDecimal totalShipped = nz(header.getShippedQuantity()).add(shippedThisTime);
        header.setShippedQuantity(totalShipped);
        BigDecimal totalNeed = nz(header.getTotalQuantity());
        // 推导 ship_status
        String newShipStatus;
        if (totalNeed.signum() > 0 && totalShipped.compareTo(totalNeed) >= 0) {
            newShipStatus = WmProductSalesConstants.SHIP_STATUS_SHIPPED;
            // 全部发运完成时同步主状态到 SHIPPED
            header.setStatus(WmProductSalesConstants.STATUS_SHIPPED);
        } else {
            newShipStatus = WmProductSalesConstants.SHIP_STATUS_PARTIAL_SHIPPED;
        }
        header.setShipStatus(newShipStatus);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        salesMapper.updateWmProductSales(header);
    }

    // ════════════════════ 修改 ════════════════════

    @Override
    public int updateWmProductSalesShipment(WmProductSalesShipment entity) {
        WmProductSalesShipment exist = shipmentMapper.selectWmProductSalesShipmentByShipmentId(entity.getShipmentId());
        if (exist == null) throw new ServiceException("发运单不存在");
        if (!WmProductSalesConstants.SHIPMENT_STATUS_SHIPPING.equals(exist.getStatus())
                && !WmProductSalesConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(exist.getStatus())) {
            throw new ServiceException("当前状态[" + exist.getStatus() + "]不允许修改");
        }
        // 系统计算字段不允许前端覆盖（shippedQuantity/boxCount 由装箱关联算出；status 走 receive/cancel 专用接口）
        entity.setShippedQuantity(null);
        entity.setBoxCount(null);
        entity.setStatus(null);
        entity.setSalesId(null);
        entity.setShipmentCode(null);
        entity.setUpdateTime(DateUtils.getNowDate());
        entity.setUpdateBy(SecurityUtils.getUsername());
        return shipmentMapper.updateWmProductSalesShipment(entity);
    }

    // ════════════════════ 删除（仅 SHIPPING/IN_TRANSIT 可删，回滚箱 + 头表） ════════════════════

    @Override
    public int deleteWmProductSalesShipmentByShipmentId(Long shipmentId) {
        WmProductSalesShipment exist = shipmentMapper.selectWmProductSalesShipmentByShipmentId(shipmentId);
        if (exist == null) return 0;
        if (WmProductSalesConstants.SHIPMENT_STATUS_RECEIVED.equals(exist.getStatus())) {
            throw new ServiceException("已签收的发运单不可删除（需走销售退货）");
        }
        Long salesId = exist.getSalesId();
        lockTemplate.execute("wm:salesout:lock:" + salesId, 10,
                () -> txTemplate.execute(status -> doDeleteShipment(exist, salesId)));
        return 1;
    }

    @Override
    public int deleteWmProductSalesShipmentByShipmentIds(Long[] shipmentIds) {
        for (Long id : shipmentIds) deleteWmProductSalesShipmentByShipmentId(id);
        return shipmentIds.length;
    }

    private Long doDeleteShipment(WmProductSalesShipment ship, Long salesId) {
        // 回滚关联箱：shipment_id=null, status=PACKED（显式 SQL，<if test!=null> 无法置 null）
        List<WmProductSalesBox> boxes = boxMapper.selectBoxesByShipmentId(ship.getShipmentId());
        Date now = DateUtils.getNowDate();
        String user = SecurityUtils.getUsername();
        for (WmProductSalesBox b : boxes) {
            boxMapper.rollbackToPacked(b.getBoxId(), user, now);
        }
        // 回写头表：shipped_quantity 扣减 + 重算 ship_status
        WmProductSales header = salesMapper.selectWmProductSalesBySalesId(salesId);
        if (header != null) {
            BigDecimal newShipped = nz(header.getShippedQuantity()).subtract(nz(ship.getShippedQuantity()));
            if (newShipped.signum() < 0) newShipped = BigDecimal.ZERO;
            header.setShippedQuantity(newShipped);
            // 重算 ship_status：无发运→UN_SHIPPED；全部发完且剩余发运单全签收→RECEIVED；全部发完→SHIPPED；否则 PARTIAL
            String newShipStatus;
            BigDecimal totalNeed = nz(header.getTotalQuantity());
            boolean fullyShipped = totalNeed.signum() > 0 && newShipped.compareTo(totalNeed) >= 0;
            if (newShipped.signum() == 0) {
                newShipStatus = WmProductSalesConstants.SHIP_STATUS_UN_SHIPPED;
            } else if (fullyShipped && allShipmentsReceived(salesId, ship.getShipmentId())) {
                newShipStatus = WmProductSalesConstants.SHIP_STATUS_RECEIVED;
            } else if (fullyShipped) {
                newShipStatus = WmProductSalesConstants.SHIP_STATUS_SHIPPED;
            } else {
                newShipStatus = WmProductSalesConstants.SHIP_STATUS_PARTIAL_SHIPPED;
            }
            header.setShipStatus(newShipStatus);
            // 主状态如因全发运改为 SHIPPED，删除后回退为 POSTED
            if (WmProductSalesConstants.STATUS_SHIPPED.equals(header.getStatus())) {
                header.setStatus(WmProductSalesConstants.STATUS_POSTED);
            }
            header.setUpdateTime(now);
            header.setUpdateBy(user);
            salesMapper.updateWmProductSales(header);
        }
        shipmentMapper.deleteWmProductSalesShipmentByShipmentId(ship.getShipmentId());
        return ship.getShipmentId();
    }

    // ════════════════════ 签收 ════════════════════

    @Override
    public int receive(Long shipmentId, WmProductSalesShipment info) {
        WmProductSalesShipment exist = shipmentMapper.selectWmProductSalesShipmentByShipmentId(shipmentId);
        if (exist == null) throw new ServiceException("发运单不存在");
        if (!WmProductSalesConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(exist.getStatus())
                && !WmProductSalesConstants.SHIPMENT_STATUS_SHIPPING.equals(exist.getStatus())) {
            throw new ServiceException("当前状态[" + exist.getStatus() + "]不可签收");
        }
        Long salesId = exist.getSalesId();
        lockTemplate.execute("wm:salesout:lock:" + salesId, 10,
                () -> txTemplate.execute(status -> doReceive(exist, info, salesId)));
        return 1;
    }

    private Long doReceive(WmProductSalesShipment exist, WmProductSalesShipment info, Long salesId) {
        Date receivedTime = info.getReceivedTime() != null ? info.getReceivedTime() : new Date();
        if (receivedTime.after(new Date())) {
            throw new ServiceException("签收时间不能晚于当前时间");
        }
        exist.setStatus(WmProductSalesConstants.SHIPMENT_STATUS_RECEIVED);
        exist.setReceivedTime(receivedTime);
        if (info.getReceivedBy() != null) exist.setReceivedBy(info.getReceivedBy());
        if (info.getReceivedRemark() != null) exist.setReceivedRemark(info.getReceivedRemark());
        if (info.getAttachmentUrl() != null) exist.setAttachmentUrl(info.getAttachmentUrl());
        exist.setUpdateTime(DateUtils.getNowDate());
        exist.setUpdateBy(SecurityUtils.getUsername());
        shipmentMapper.updateWmProductSalesShipment(exist);
        // 全部数量发完且全部发运单都已签收 → 头表 ship_status=RECEIVED
        WmProductSales header = salesMapper.selectWmProductSalesBySalesId(salesId);
        if (header != null
                && WmProductSalesConstants.SHIP_STATUS_SHIPPED.equals(header.getShipStatus())
                && allShipmentsReceived(salesId)) {
            header.setShipStatus(WmProductSalesConstants.SHIP_STATUS_RECEIVED);
            header.setUpdateTime(DateUtils.getNowDate());
            header.setUpdateBy(SecurityUtils.getUsername());
            salesMapper.updateWmProductSales(header);
        }
        return exist.getShipmentId();
    }

    private boolean allShipmentsReceived(Long salesId) {
        return allShipmentsReceived(salesId, null);
    }

    /** 判断该出库单所有有效发运单是否都已签收；excludeShipmentId 用于删除前预判（把待删的排除） */
    private boolean allShipmentsReceived(Long salesId, Long excludeShipmentId) {
        List<WmProductSalesShipment> all = shipmentMapper.selectShipmentsBySalesId(salesId);
        if (all == null || all.isEmpty()) return false;
        boolean hasEffective = false;
        for (WmProductSalesShipment s : all) {
            if (excludeShipmentId != null && excludeShipmentId.equals(s.getShipmentId())) continue;
            if (WmProductSalesConstants.SHIPMENT_STATUS_CANCELED.equals(s.getStatus())) continue;
            hasEffective = true;
            if (!WmProductSalesConstants.SHIPMENT_STATUS_RECEIVED.equals(s.getStatus())) {
                return false;
            }
        }
        return hasEffective;
    }

    // ════════════════════ 取消发运 ════════════════════

    @Override
    public int cancel(Long shipmentId) {
        WmProductSalesShipment exist = shipmentMapper.selectWmProductSalesShipmentByShipmentId(shipmentId);
        if (exist == null) throw new ServiceException("发运单不存在");
        if (!WmProductSalesConstants.isCancelableShipment(exist.getStatus())) {
            throw new ServiceException("发运单已发出，不可取消（请删除或走销售退货）");
        }
        Long salesId = exist.getSalesId();
        lockTemplate.execute("wm:salesout:lock:" + salesId, 10,
                () -> txTemplate.execute(status -> {
                    exist.setStatus(WmProductSalesConstants.SHIPMENT_STATUS_CANCELED);
                    exist.setUpdateTime(DateUtils.getNowDate());
                    exist.setUpdateBy(SecurityUtils.getUsername());
                    shipmentMapper.updateWmProductSalesShipment(exist);
                    return null;
                }));
        return 1;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
