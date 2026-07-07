package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pur.PurOrder;
import com.ruoyi.system.domain.mes.pur.PurOrderLine;
import com.ruoyi.system.domain.mes.wm.ItemRecptReceiveBody;
import com.ruoyi.system.domain.mes.wm.WmBatch;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.tx.ItemRecptTxBean;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
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
     * 确认收货（DRAFT → CONFIRMED）— 库存更新 + PO回写在同一事务中
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

        // 加载行 → 构建 TxBean → 更新库存
        WmItemRecptLine lineQuery = new WmItemRecptLine();
        lineQuery.setRecptId(recptId);
        List<WmItemRecptLine> lines = wmItemRecptLineService.selectWmItemRecptLineList(lineQuery);
        if (lines.isEmpty()) {
            throw new RuntimeException("没有入库行，无法确认");
        }

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
            b.setVendorCode(header.getVendorId() != null ? header.getVendorId().toString() : null);
            txBeans.add(b);
        }

        // 1. 更新库存（在同一事务中）
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
     * 回写 PO 行已收数量 + 判断 PO 是否全部收完
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
     * 一键收货（移动端）：创建入库单头+行+确认收货，单个事务原子完成。
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
        header.setCreateTime(DateUtils.getNowDate());
        header.setCreateBy(SecurityUtils.getUsername());
        wmItemRecptMapper.insertWmItemRecpt(header);
        Long recptId = header.getRecptId();

        // 2. 创建入库单行 + 汇总 totalQuantity
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmItemRecptLine line : lines) {
            line.setRecptId(recptId);
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

        // 3. 确认收货 + 过账入库（库存更新 + PO quantityReceived回写 + PO完成判断）
        confirmItemRecpt(recptId);
        postItemRecpt(recptId);
    }

    /**
     * 确认收货时回写 PO — 标记到货 + 更新 PO 状态 → RECEIVING
     * 批量加载 PO 行（避免 N+1 查询）
     */
    private void writebackPoOnConfirm(WmItemRecpt header) {
        Long purOrderId = header.getPurOrderId();
        if (purOrderId == null || purOrderId <= 0) return;

        // 批量加载所有 PO 行（一次查询）
        PurOrderLine allPoLinesQuery = new PurOrderLine();
        allPoLinesQuery.setOrderId(purOrderId);
        List<PurOrderLine> allPoLines = purOrderLineService.selectPurOrderLineList(allPoLinesQuery);

        // 加载入库行
        WmItemRecptLine lineQuery = new WmItemRecptLine();
        lineQuery.setRecptId(header.getRecptId());
        List<WmItemRecptLine> recptLines = wmItemRecptLineService.selectWmItemRecptLineList(lineQuery);

        String username = SecurityUtils.getUsername();
        for (WmItemRecptLine recptLine : recptLines) {
            for (PurOrderLine poLine : allPoLines) {
                if (recptLine.getItemId().equals(poLine.getItemId())) {
                    poLine.setArrivalNoticeId(header.getRecptId());
                    poLine.setStatus("RECEIVING");
                    poLine.setUpdateTime(DateUtils.getNowDate());
                    poLine.setUpdateBy(username);
                    purOrderLineService.updatePurOrderLine(poLine);
                }
            }
        }

        // PO 状态：ORDERED → RECEIVING
        PurOrder purOrder = purOrderService.selectPurOrderByOrderId(purOrderId);
        if (purOrder != null && "ORDERED".equals(purOrder.getStatus())) {
            purOrder.setStatus("RECEIVING");
            purOrder.setUpdateTime(DateUtils.getNowDate());
            purOrderService.updatePurOrder(purOrder);
        }
    }

    /**
     * 过账时回写 PO — 累加已收数量 + 判断是否全部收完
     * 批量加载 PO 行（避免 N+1 查询）
     */
    private void writebackPoOnPost(WmItemRecpt header) {
        Long purOrderId = header.getPurOrderId();

        // 批量加载所有 PO 行（一次查询）
        PurOrderLine allPoLinesQuery = new PurOrderLine();
        allPoLinesQuery.setOrderId(purOrderId);
        List<PurOrderLine> allPoLines = purOrderLineService.selectPurOrderLineList(allPoLinesQuery);

        // 加载入库行
        WmItemRecptLine lineQuery = new WmItemRecptLine();
        lineQuery.setRecptId(header.getRecptId());
        List<WmItemRecptLine> recptLines = wmItemRecptLineService.selectWmItemRecptLineList(lineQuery);

        String username = SecurityUtils.getUsername();
        boolean allReceived = true;
        for (WmItemRecptLine recptLine : recptLines) {
            for (PurOrderLine poLine : allPoLines) {
                if (recptLine.getItemId().equals(poLine.getItemId())) {
                    BigDecimal currentReceived = poLine.getQuantityReceived() != null
                        ? poLine.getQuantityReceived() : BigDecimal.ZERO;
                    BigDecimal recptQty = recptLine.getQuantityRecpt() != null
                        ? recptLine.getQuantityRecpt() : BigDecimal.ZERO;
                    poLine.setQuantityReceived(currentReceived.add(recptQty));
                    BigDecimal ordered = poLine.getQuantityOrdered() != null
                        ? poLine.getQuantityOrdered() : BigDecimal.ZERO;
                    if (poLine.getQuantityReceived().compareTo(ordered) >= 0) {
                        poLine.setStatus("RECEIVED");
                    }
                    poLine.setUpdateTime(DateUtils.getNowDate());
                    poLine.setUpdateBy(username);
                    purOrderLineService.updatePurOrderLine(poLine);
                }
            }
        }

        // 判断 PO 是否全部收完（重新从DB加载最新状态）
        allPoLines = purOrderLineService.selectPurOrderLineList(allPoLinesQuery);
        for (PurOrderLine poLine : allPoLines) {
            if (!"RECEIVED".equals(poLine.getStatus())) {
                allReceived = false;
                break;
            }
        }
        if (allReceived && !allPoLines.isEmpty()) {
            PurOrder purOrder = purOrderService.selectPurOrderByOrderId(purOrderId);
            if (purOrder != null) {
                purOrder.setStatus("RECEIVED");
                purOrder.setUpdateTime(DateUtils.getNowDate());
                purOrderService.updatePurOrder(purOrder);
            }
        }
    }
}