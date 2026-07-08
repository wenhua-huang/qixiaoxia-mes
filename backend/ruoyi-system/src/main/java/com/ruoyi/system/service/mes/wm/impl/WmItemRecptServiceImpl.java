package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.enums.PurOrderStatus;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.wm.ItemRecptReceiveBody;
import com.ruoyi.system.domain.mes.wm.WmBatch;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.tx.ItemRecptTxBean;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.mapper.mes.pur.PurOrderLineMapper;
import com.ruoyi.system.service.mes.pur.IPurOrderLineService;
import com.ruoyi.system.service.mes.pur.IPurOrderService;
import com.ruoyi.system.service.mes.wm.IWmBatchService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptLineService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;

@Service
public class WmItemRecptServiceImpl implements IWmItemRecptService
{
    @Autowired
    private WmItemRecptMapper wmItemRecptMapper;

    @Autowired
    private IWmItemRecptLineService wmItemRecptLineService;

    @Autowired
    private IPurOrderService purOrderService;

    @Autowired
    private IPurOrderLineService purOrderLineService;

    @Autowired
    private PurOrderLineMapper purOrderLineMapper;

    @Autowired
    private IWmStorageCoreService storageCoreService;

    @Autowired
    private IWmBatchService wmBatchService;

    @Override
    public List<WmItemRecpt> selectWmItemRecptList(WmItemRecpt entity) {
        return wmItemRecptMapper.selectWmItemRecptList(entity);
    }

    @Override
    public List<WmItemRecpt> selectWmItemRecptAll() {
        return wmItemRecptMapper.selectWmItemRecptAll();
    }

    @Override
    public WmItemRecpt selectWmItemRecptByRecptId(Long recptId) {
        return wmItemRecptMapper.selectWmItemRecptByRecptId(recptId);
    }

    @Override
    public WmItemRecpt selectWmItemRecptDetail(Long recptId) {
        WmItemRecpt header = wmItemRecptMapper.selectWmItemRecptByRecptId(recptId);
        if (header == null) {
            return null;
        }
        header.setLines(loadRecptLines(recptId));
        return header;
    }

    @Override
    @Transactional
    public int insertWmItemRecpt(WmItemRecpt entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmItemRecptMapper.insertWmItemRecpt(entity);
    }

    @Override
    @Transactional
    public int updateWmItemRecpt(WmItemRecpt entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmItemRecptMapper.updateWmItemRecpt(entity);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptByRecptId(Long recptId) {
        return wmItemRecptMapper.deleteWmItemRecptByRecptId(recptId);
    }

    @Override
    @Transactional
    public int deleteWmItemRecptByRecptIds(Long[] recptIds) {
        return wmItemRecptMapper.deleteWmItemRecptByRecptIds(recptIds);
    }

    /**
     * 确认收货（DRAFT → CONFIRMED）— 库存更新 + PO回写在同一事务中。
     *
     * 注：本方法使用 @Transactional；库存变更内部通过 WmTransactionServiceImpl.processTransaction()
     * 使用 Redisson 锁 + TransactionTemplate 保证并发安全。外层事务用于保证 header/lines 读写的原子性。
     */
    @Override
    @Transactional
    public void confirmItemRecpt(Long recptId) {
        WmItemRecpt header = selectWmItemRecptByRecptId(recptId);
        if (header == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"DRAFT".equals(header.getStatus())) {
            throw new RuntimeException("仅草稿状态可确认收货");
        }

        List<WmItemRecptLine> lines = loadRecptLines(recptId);
        if (lines.isEmpty()) {
            throw new RuntimeException("没有入库行，无法确认");
        }

        doConfirmItemRecpt(header, lines);
    }

    /** 确认收货核心逻辑（不入库查询，由调用方传入已加载的 header + lines） */
    private void doConfirmItemRecpt(WmItemRecpt header, List<WmItemRecptLine> lines) {
        Long recptId = header.getRecptId();
        List<ItemRecptTxBean> txBeans = new ArrayList<>();
        for (WmItemRecptLine line : lines) {
            ItemRecptTxBean b = new ItemRecptTxBean();
            b.setSourceDocType("wm_item_recpt");
            b.setSourceDocId(recptId);
            b.setSourceDocCode(header.getRecptCode());
            b.setSourceDocLineId(line.getLineId());
            b.setItemId(line.getItemId());
            b.setItemCode(line.getItemCode());
            b.setItemName(line.getItemName());
            b.setSpecification(line.getSpecification());
            b.setUnitOfMeasure(line.getUnitOfMeasure());
            b.setUnitName(line.getUnitName());
            b.setTransactionQuantity(line.getQuantityRecpt());
            b.setBatchId(line.getBatchId());
            b.setBatchCode(line.getBatchCode());
            b.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            b.setWarehouseCode(line.getWarehouseCode());
            b.setWarehouseName(line.getWarehouseName());
            b.setLocationId(line.getLocationId());
            b.setAreaId(line.getAreaId());
            b.setVendorId(header.getVendorId());
            b.setVendorCode(header.getVendorCode());
            txBeans.add(b);
        }

        // 1. 更新库存（内部使用 Redisson 锁 + TransactionTemplate）
        storageCoreService.processItemRecpt(txBeans);

        // 2. 确认收货状态
        header.setStatus("CONFIRMED");
        header.setUpdateTime(DateUtils.getNowDate());
        wmItemRecptMapper.updateWmItemRecpt(header);

        // 3. 回写 PO（采购入库时）
        writebackPoOnConfirm(header);
    }

    /**
     * 过账入库（CONFIRMED → POSTED）
     * 回写 PO 行已收数量 + 判断 PO 是否全部收完。
     *
     * 注：本方法使用 @Transactional；库存数据已在 confirmItemRecpt 阶段通过 Redisson 锁更新完毕，
     * 此处仅做状态变更 + PO 回写，不涉及库存变更。
     */
    @Override
    @Transactional
    public void postItemRecpt(Long recptId) {
        WmItemRecpt header = selectWmItemRecptByRecptId(recptId);
        if (header == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"CONFIRMED".equals(header.getStatus())) {
            throw new RuntimeException("仅已确认的入库单可过账");
        }
        doPostItemRecpt(header);
    }

    /** 过账入库核心逻辑（不入库查询，由调用方传入已加载的 header） */
    private void doPostItemRecpt(WmItemRecpt header) {
        // 过账入库
        header.setStatus("POSTED");
        header.setUpdateTime(DateUtils.getNowDate());
        wmItemRecptMapper.updateWmItemRecpt(header);
        // 回写 PO 已收数量（采购入库时）
        if (header.getPurOrderId() != null && header.getPurOrderId() > 0) {
            writebackPoOnPost(header);
        }
    }

    /**
     * 一键收货（移动端）：创建入库单头+行+确认收货+过账，单个事务原子完成。
     *
     * 注：本方法使用 @Transactional 保证多步操作的原子性（header+lines+confirm+post）。
     * 库存变更通过 WmTransactionServiceImpl.processTransaction() 内部使用 Redisson 锁 + TransactionTemplate 保证并发安全。
     */
    @Override
    @Transactional
    public void receiveWithLines(ItemRecptReceiveBody body) {
        WmItemRecpt header = body.getHeader();
        List<WmItemRecptLine> lines = body.getLines();
        if (header == null) throw new RuntimeException("入库单头信息不能为空");
        if (lines == null || lines.isEmpty()) throw new RuntimeException("入库单行不能为空");

        // 1. 创建入库单头 — 若 header 未指定仓库，取第一行仓库
        if (header.getWarehouseId() == null) {
            WmItemRecptLine firstLine = lines.get(0);
            header.setWarehouseId(firstLine.getWarehouseId());
            header.setWarehouseCode(firstLine.getWarehouseCode());
            header.setWarehouseName(firstLine.getWarehouseName());
        }
        // 强制设为草稿，防止客户端传入非 DRAFT 状态
        header.setStatus("DRAFT");
        header.setCreateTime(DateUtils.getNowDate());
        header.setCreateBy(SecurityUtils.getUsername());
        wmItemRecptMapper.insertWmItemRecpt(header);

        // 2. 创建入库单行 + 汇总 totalQuantity
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmItemRecptLine line : lines) {
            line.setRecptId(header.getRecptId());
            if (line.getWarehouseId() == null) line.setWarehouseId(header.getWarehouseId());
            if (line.getWarehouseCode() == null) line.setWarehouseCode(header.getWarehouseCode());
            if (line.getWarehouseName() == null) line.setWarehouseName(header.getWarehouseName());
            // 自动生成批次号（无 batchCode 时，根据物料+供应商+生产日期+有效期匹配或新建）
            if (line.getBatchCode() == null) {
                WmBatch batchParam = new WmBatch();
                batchParam.setItemId(line.getItemId());
                batchParam.setItemCode(line.getItemCode());
                batchParam.setItemName(line.getItemName());
                batchParam.setSpecification(line.getSpecification());
                batchParam.setVendorId(header.getVendorId());
                batchParam.setVendorCode(header.getVendorCode());
                batchParam.setVendorName(header.getVendorName());
                batchParam.setProduceDate(line.getProduceDate());
                batchParam.setExpireDate(line.getExpireDate());
                batchParam.setLotNumber(line.getLotNumber());
                WmBatch generated = wmBatchService.getOrGenerateBatchCode(batchParam);
                if (generated != null) {
                    line.setBatchId(generated.getBatchId());
                    line.setBatchCode(generated.getBatchCode());
                }
            }
            line.setCreateTime(DateUtils.getNowDate());
            line.setCreateBy(SecurityUtils.getUsername());
            wmItemRecptLineService.insertWmItemRecptLine(line);
            if (line.getQuantityRecpt() != null) {
                totalQty = totalQty.add(line.getQuantityRecpt());
            }
        }
        // 回写头部的入库总数量
        header.setTotalQuantity(totalQty);
        header.setUpdateTime(DateUtils.getNowDate());
        wmItemRecptMapper.updateWmItemRecpt(header);

        // 3. 确认收货 + 过账入库（直接传入已加载对象，避免重复 DB 查询）
        doConfirmItemRecpt(header, lines);
        doPostItemRecpt(header);
    }

    /**
     * 确认收货时回写 PO — 标记到货 + 更新 PO 状态 → RECEIVING
     * 使用 itemId→PO行 Map 做 O(1) 查找，避免 O(n×m) 嵌套循环。
     */
    private void writebackPoOnConfirm(WmItemRecpt header) {
        Long purOrderId = header.getPurOrderId();
        if (purOrderId == null || purOrderId <= 0) return;

        List<PurOrderLine> allPoLines = loadPoLinesByOrderId(purOrderId);
        List<WmItemRecptLine> recptLines = loadRecptLines(header.getRecptId());

        // 构建 itemId → PurOrderLine Map（O(1) 查找，避免嵌套循环）
        java.util.Map<Long, PurOrderLine> poLineByItemId = buildPoLineItemIdMap(allPoLines);

        String username = SecurityUtils.getUsername();
        for (WmItemRecptLine recptLine : recptLines) {
            PurOrderLine poLine = poLineByItemId.get(recptLine.getItemId());
            if (poLine != null) {
                poLine.setArrivalNoticeId(header.getRecptId());
                poLine.setStatus(PurOrderStatus.RECEIVING.getCode());
                poLine.setUpdateTime(DateUtils.getNowDate());
                poLine.setUpdateBy(username);
                purOrderLineService.updatePurOrderLine(poLine);
            }
        }

        // PO 状态：ORDERED → RECEIVING
        PurOrder purOrder = purOrderService.selectPurOrderByOrderId(purOrderId);
        if (purOrder != null && PurOrderStatus.ORDERED.is(purOrder.getStatus())) {
            purOrder.setStatus(PurOrderStatus.RECEIVING.getCode());
            purOrder.setUpdateTime(DateUtils.getNowDate());
            purOrderService.updatePurOrder(purOrder);
        }
    }

    /**
     * 过账时回写 PO — 累加已收数量 + 判断是否全部收完。
     * 使用 itemId→PO行 Map 做 O(1) 查找，track allReceived 避免二次 DB 查询。
     */
    private void writebackPoOnPost(WmItemRecpt header) {
        Long purOrderId = header.getPurOrderId();
        if (purOrderId == null || purOrderId <= 0) return;

        List<PurOrderLine> allPoLines = loadPoLinesByOrderId(purOrderId);
        List<WmItemRecptLine> recptLines = loadRecptLines(header.getRecptId());

        // 构建 itemId → PurOrderLine Map（O(1) 查找）
        java.util.Map<Long, PurOrderLine> poLineByItemId = buildPoLineItemIdMap(allPoLines);

        String username = SecurityUtils.getUsername();
        Date now = DateUtils.getNowDate();
        // 1. 原子递增 quantityReceived（并发安全：UPDATE SET qty = qty + delta）
        for (WmItemRecptLine recptLine : recptLines) {
            PurOrderLine poLine = poLineByItemId.get(recptLine.getItemId());
            if (poLine != null) {
                BigDecimal recptQty = recptLine.getQuantityRecpt() != null
                    ? recptLine.getQuantityRecpt() : BigDecimal.ZERO;
                if (recptQty.compareTo(BigDecimal.ZERO) > 0) {
                    purOrderLineMapper.addQuantityReceived(poLine.getLineId(), recptQty);
                }
            }
        }
        // 2. 原子递增后重新加载，判断各行是否已收完
        allPoLines = loadPoLinesByOrderId(purOrderId);
        boolean allReceived = true;
        for (PurOrderLine poLine : allPoLines) {
            BigDecimal received = poLine.getQuantityReceived() != null
                ? poLine.getQuantityReceived() : BigDecimal.ZERO;
            BigDecimal ordered = poLine.getQuantityOrdered() != null
                ? poLine.getQuantityOrdered() : BigDecimal.ZERO;
            if (received.compareTo(ordered) >= 0) {
                poLine.setStatus(PurOrderStatus.RECEIVED.getCode());
            }
            poLine.setUpdateTime(now);
            poLine.setUpdateBy(username);
            purOrderLineService.updatePurOrderLine(poLine);
            if (!PurOrderStatus.RECEIVED.is(poLine.getStatus())) {
                allReceived = false;
            }
        }
        if (allReceived && !allPoLines.isEmpty()) {
            PurOrder purOrder = purOrderService.selectPurOrderByOrderId(purOrderId);
            if (purOrder != null) {
                purOrder.setStatus(PurOrderStatus.RECEIVED.getCode());
                purOrder.setUpdateTime(DateUtils.getNowDate());
                purOrderService.updatePurOrder(purOrder);
            }
        }
    }

    /** 加载 PO 订单下所有行 */
    private List<PurOrderLine> loadPoLinesByOrderId(Long purOrderId) {
        PurOrderLine query = new PurOrderLine();
        query.setOrderId(purOrderId);
        return purOrderLineService.selectPurOrderLineList(query);
    }

    /** 加载入库单下所有行 */
    private List<WmItemRecptLine> loadRecptLines(Long recptId) {
        WmItemRecptLine query = new WmItemRecptLine();
        query.setRecptId(recptId);
        return wmItemRecptLineService.selectWmItemRecptLineList(query);
    }

    /**
     * 构建 itemId → PurOrderLine Map（O(1) 查找）。
     * DB 层已通过 V49 uk_order_item(order_id, item_id) 保证同一 PO 内物料唯一。
     */
    private java.util.Map<Long, PurOrderLine> buildPoLineItemIdMap(List<PurOrderLine> poLines) {
        java.util.Map<Long, PurOrderLine> map = new java.util.LinkedHashMap<>();
        for (PurOrderLine poLine : poLines) {
            Long itemId = poLine.getItemId();
            if (itemId != null) {
                map.put(itemId, poLine);
            }
        }
        return map;
    }
}