package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductRecptMobileBody;
import com.ruoyi.system.domain.mes.wm.WmProductRecptMobileBody.MobileLineItem;
import com.ruoyi.system.domain.mes.wm.tx.ProductRecptTxBean;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptMapper;
import com.ruoyi.system.service.mes.wm.IWmProductRecptLineService;
import com.ruoyi.system.service.mes.wm.IWmProductRecptService;
import com.ruoyi.system.service.mes.wm.IWmStorageCoreService;

@Service
public class WmProductRecptServiceImpl implements IWmProductRecptService
{
    @Autowired
    private WmProductRecptMapper wmProductRecptMapper;

    @Autowired
    private IWmProductRecptLineService wmProductRecptLineService;

    @Autowired
    private IWmStorageCoreService storageCoreService;

    @Override
    public List<WmProductRecpt> selectWmProductRecptList(WmProductRecpt entity) {
        return wmProductRecptMapper.selectWmProductRecptList(entity);
    }

    @Override
    public List<WmProductRecpt> selectWmProductRecptAll() {
        return wmProductRecptMapper.selectWmProductRecptAll();
    }

    @Override
    public WmProductRecpt selectWmProductRecptByRecptId(Long recptId) {
        return wmProductRecptMapper.selectWmProductRecptByRecptId(recptId);
    }

    @Override
    public WmProductRecpt selectWmProductRecptDetail(Long recptId) {
        WmProductRecpt header = wmProductRecptMapper.selectWmProductRecptByRecptId(recptId);
        if (header == null) {
            return null;
        }
        header.setLines(loadRecptLines(recptId));
        return header;
    }

    @Override
    @Transactional
    public int insertWmProductRecpt(WmProductRecpt entity) {
        entity.setCreateBy(SecurityUtils.getUsername());
        entity.setCreateTime(DateUtils.getNowDate());
        return wmProductRecptMapper.insertWmProductRecpt(entity);
    }

    @Override
    @Transactional
    public int updateWmProductRecpt(WmProductRecpt entity) {
        entity.setUpdateBy(SecurityUtils.getUsername());
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmProductRecptMapper.updateWmProductRecpt(entity);
    }

    @Override
    @Transactional
    public int deleteWmProductRecptByRecptId(Long recptId) {
        return wmProductRecptMapper.deleteWmProductRecptByRecptId(recptId);
    }

    @Override
    @Transactional
    public int deleteWmProductRecptByRecptIds(Long[] recptIds) {
        return wmProductRecptMapper.deleteWmProductRecptByRecptIds(recptIds);
    }

    /**
     * 确认收货（DRAFT -> CONFIRMED）- 更新库存。
     * 注：本方法使用 @Transactional；库存变更内部通过 Redisson 锁 + TransactionTemplate 保证并发安全。
     */
    @Override
    @Transactional
    public void confirmProductRecpt(Long recptId) {
        WmProductRecpt header = selectWmProductRecptByRecptId(recptId);
        if (header == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"DRAFT".equals(header.getStatus())) {
            throw new RuntimeException("仅草稿状态可确认收货");
        }
        List<WmProductRecptLine> lines = loadRecptLines(recptId);
        if (lines.isEmpty()) {
            throw new RuntimeException("没有入库行，无法确认");
        }
        doConfirmProductRecpt(header, lines);
    }

    /** 确认收货核心逻辑（不入库查询，由调用方传入已加载的 header + lines） */
    private void doConfirmProductRecpt(WmProductRecpt header, List<WmProductRecptLine> lines) {
        Long recptId = header.getRecptId();
        List<ProductRecptTxBean> txBeans = new ArrayList<>();
        for (WmProductRecptLine line : lines) {
            ProductRecptTxBean b = new ProductRecptTxBean();
            b.setSourceDocType("wm_product_recpt");
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
            b.setWarehouseCode(header.getWarehouseCode());
            b.setWarehouseName(header.getWarehouseName());
            b.setLocationId(line.getLocationId());
            b.setAreaId(line.getAreaId());
            txBeans.add(b);
        }
        // 更新库存（内部使用 Redisson 锁 + TransactionTemplate）
        storageCoreService.processProductRecpt(txBeans);
        // 确认收货状态
        header.setStatus("CONFIRMED");
        header.setUpdateBy(SecurityUtils.getUsername());
        header.setUpdateTime(DateUtils.getNowDate());
        wmProductRecptMapper.updateWmProductRecpt(header);
    }

    /**
     * 过账入库（CONFIRMED -> POSTED）。
     * 库存数据已在 confirmProductRecpt 阶段通过 Redisson 锁更新完毕，此处仅做状态变更。
     */
    @Override
    @Transactional
    public void postProductRecpt(Long recptId) {
        WmProductRecpt header = selectWmProductRecptByRecptId(recptId);
        if (header == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"CONFIRMED".equals(header.getStatus())) {
            throw new RuntimeException("仅已确认的入库单可过账");
        }
        header.setStatus("POSTED");
        header.setUpdateBy(SecurityUtils.getUsername());
        header.setUpdateTime(DateUtils.getNowDate());
        wmProductRecptMapper.updateWmProductRecpt(header);
    }

    /**
     * 移动端确认入库 — 更新行数量 + 确认 + 更新库存，单接口原子完成。
     * 仅支持 DRAFT 状态。使用 @Transactional 保证行更新和确认在同一事务中。
     */
    @Override
    @Transactional
    public void mobileConfirmProductRecpt(Long recptId, WmProductRecptMobileBody body) {
        WmProductRecpt header = selectWmProductRecptByRecptId(recptId);
        if (header == null) {
            throw new RuntimeException("入库单不存在");
        }
        if (!"DRAFT".equals(header.getStatus())) {
            throw new RuntimeException("仅草稿状态可确认收货");
        }
        List<MobileLineItem> bodyLines = body.getLines();
        if (bodyLines == null || bodyLines.isEmpty()) {
            throw new RuntimeException("请至少填写一条入库数量");
        }

        // 加载数据库中的行，按 lineId 建立索引
        List<WmProductRecptLine> dbLines = loadRecptLines(recptId);
        if (dbLines == null || dbLines.isEmpty()) {
            throw new RuntimeException("入库单没有行数据");
        }
        // 校验：用户必须提交所有行（移动端不能跳过行）
        if (bodyLines.size() != dbLines.size()) {
            throw new RuntimeException("请提交所有入库行的实收数量（当前" + bodyLines.size() + "行，共" + dbLines.size() + "行）");
        }
        Map<Long, WmProductRecptLine> lineMap = dbLines.stream()
                .collect(Collectors.toMap(WmProductRecptLine::getLineId, l -> l, (a, b) -> a));

        BigDecimal totalQty = BigDecimal.ZERO;
        int totalBox = 0;
        List<WmProductRecptLine> updatedLines = new ArrayList<>();
        for (MobileLineItem item : bodyLines) {
            WmProductRecptLine line = lineMap.get(item.getLineId());
            if (line == null) {
                throw new RuntimeException("行ID " + item.getLineId() + " 不属于该入库单");
            }
            BigDecimal qty = item.getQuantityRecpt();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("行ID " + item.getLineId() + " 实收数量不能为负数");
            }
            line.setQuantityRecpt(qty);
            // 数量为 0 的行跳过库存更新（仅更新行记录，不产生 stock transaction）
            if (qty.compareTo(BigDecimal.ZERO) == 0) {
                line.setQuantityBox(0);
                wmProductRecptLineService.updateWmProductRecptLine(line);
                continue;
            }
            line.setQuantityBox(item.getQuantityBox() != null ? item.getQuantityBox() : 0);
            if (item.getWarehouseId() != null) {
                line.setWarehouseId(item.getWarehouseId());
            }
            if (item.getBatchCode() != null) {
                line.setBatchCode(item.getBatchCode());
            }
            wmProductRecptLineService.updateWmProductRecptLine(line);
            totalQty = totalQty.add(qty);
            totalBox += (item.getQuantityBox() != null ? item.getQuantityBox() : 0);
            updatedLines.add(line);
        }

        // 更新 header
        if (body.getWarehouseId() != null) {
            header.setWarehouseId(body.getWarehouseId());
            // 清除旧的 warehouseCode/Name，防止 tx bean 中 warehouseId 与 code/name 不一致
            header.setWarehouseCode(null);
            header.setWarehouseName(null);
        }
        if (body.getRemark() != null) {
            header.setRemark(body.getRemark());
        }
        header.setTotalQuantity(totalQty);
        header.setTotalBox(totalBox);
        header.setUpdateBy(SecurityUtils.getUsername());
        header.setUpdateTime(DateUtils.getNowDate());
        wmProductRecptMapper.updateWmProductRecpt(header);

        // 确认收货 — 更新库存（仅处理用户提交的行，避免未提交行产生零数量库存事务）
        doConfirmProductRecpt(header, updatedLines);
    }

    /** 加载入库单下所有行 */
    private List<WmProductRecptLine> loadRecptLines(Long recptId) {
        WmProductRecptLine query = new WmProductRecptLine();
        query.setRecptId(recptId);
        return wmProductRecptLineService.selectWmProductRecptLineList(query);
    }
}
