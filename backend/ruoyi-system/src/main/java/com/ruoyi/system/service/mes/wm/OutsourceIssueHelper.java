package com.ruoyi.system.service.mes.wm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.wm.OutsourceRequest;
import com.ruoyi.system.domain.mes.wm.WmOutsourceIssueLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.mapper.mes.wm.WmOutsourceOrderMapper;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.service.mes.pro.IProRouteProcessService;
import com.ruoyi.system.service.mes.pro.IProRouteProductService;

/**
 * 外协发料辅助器：工单开工/单据生成时，将外协工序(is_outsource=1)的物料发料
 * 从「生产领料单(PRODUCE)」分流到「通用外协订单(qxx_wm_outsource_order)」。
 *
 * 设计动机：外协工序物料发往外部厂商，应走外协订单(含厂商、外协收货回写报工全链路)，
 * 而非厂内生产领料。本类封装「判定外协工序 + 构造 OutsourceRequest + 幂等调用 createOutsource」，
 * 供 ProWorkorderServiceImpl 与 ProWorkorderDocServiceImpl 复用，避免逻辑重复。
 *
 * @author qixiaoxia
 */
@Component
public class OutsourceIssueHelper {
    private static final Logger log = LoggerFactory.getLogger(OutsourceIssueHelper.class);

    @Autowired
    private IProRouteProductService proRouteProductService;
    @Autowired
    private IProRouteProcessService proRouteProcessService;
    @Autowired
    private WmOutsourceOrderMapper outsourceOrderMapper;
    @Autowired
    private WmMaterialStockMapper wmMaterialStockMapper;
    /** @Lazy 防 IOutsourceService 实现链间接回引 pro.workorder 造成循环依赖 */
    @Autowired
    @Lazy
    private IOutsourceService outsourceService;

    /**
     * 解析工单工艺路线的外协工序索引：routeProductId → routeId → 路线工序列表 → processId→ProRouteProcess。
     * 工单无路线(routeProductId 为空)时返回空 map（所有工序视为自制）。
     *
     * @return processId → ProRouteProcess 映射（仅外协工序 is_outsource=1）；无路线返回空 map
     */
    public Map<Long, ProRouteProcess> resolveOutsourceProcessMap(ProWorkorder wo) {
        Map<Long, ProRouteProcess> result = new LinkedHashMap<>();
        if (wo == null || wo.getRouteProductId() == null) {
            return result;
        }
        ProRouteProduct rp = proRouteProductService.selectProRouteProductByRecordId(wo.getRouteProductId());
        if (rp == null || rp.getRouteId() == null) {
            return result;
        }
        List<ProRouteProcess> rps = proRouteProcessService.selectProRouteProcessByRouteId(rp.getRouteId());
        if (rps == null) {
            return result;
        }
        for (ProRouteProcess p : rps) {
            if ("1".equals(p.getIsOutsource()) && p.getProcessId() != null) {
                result.put(p.getProcessId(), p);
            }
        }
        return result;
    }

    /**
     * 为单个外协工序构造并发送外协发料单（草稿模式：不扣料，发料行 FIFO 预填仓库/批次，用户可改）。
     * 幂等：若该 (workorderId, processId) 已存在外协订单，跳过不重复建单。
     * 无 BOM 行的外协工序(无料可发)跳过，createOutsource 要求 issueLines 非空。
     *
     * @param wo 工单
     * @param rp 外协工序（路线工序，含 processId/vendorId）
     * @param processBoms 该工序的 BOM 行（用于构造发料明细）
     * @param defaultWarehouseId 默认仓库（FIFO 无结果时的兜底，可 null）
     * @return 外协单编码；null=无 BOM 跳过
     */
    public String issueOutsourceForProcess(ProWorkorder wo, ProRouteProcess rp,
                                            List<ProWorkorderBom> processBoms, Long defaultWarehouseId) {
        if (processBoms == null || processBoms.isEmpty()) {
            return null;
        }
        // 幂等：该工单+工序已有外协订单则直接返回其编码
        WmOutsourceOrder existing = findExistingOrder(wo.getWorkorderId(), rp.getProcessId());
        if (existing != null) {
            return existing.getOrderCode();
        }
        // 批量预取所有 BOM 物料的可用库存，按 itemId 分组并在内存中 FIFO 取最早批次，消除逐行 N+1
        List<Long> bomItemIds = processBoms.stream()
                .map(ProWorkorderBom::getItemId).filter(java.util.Objects::nonNull).distinct()
                .collect(Collectors.toList());
        Map<Long, WmMaterialStock> fifoByItem = batchFifoStocks(bomItemIds);

        List<WmOutsourceIssueLine> lines = new ArrayList<>();
        for (ProWorkorderBom bom : processBoms) {
            WmOutsourceIssueLine line = new WmOutsourceIssueLine();
            line.setItemId(bom.getItemId());
            line.setItemCode(bom.getItemCode());
            line.setItemName(bom.getItemName());
            line.setSpecification(bom.getItemSpc());
            line.setUnitOfMeasure(bom.getUnitOfMeasure());
            line.setUnitName(bom.getUnitName());
            line.setQuantity(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO);
            // FIFO 预填仓库/批次：从批量预取结果取该物料最早可用批次，查不到则用默认仓库兜底
            applyFifoStock(line, fifoByItem.get(bom.getItemId()), defaultWarehouseId);
            lines.add(line);
        }
        OutsourceRequest req = new OutsourceRequest();
        req.setWorkorderId(wo.getWorkorderId());
        req.setWorkorderCode(wo.getWorkorderCode());
        req.setProcessId(rp.getProcessId());
        req.setProcessCode(rp.getProcessCode());
        req.setProcessName(rp.getProcessName());
        // routeId：解析出用于 createOutsource 内部判断末工序
        req.setRouteId(resolveRouteId(wo));
        // vendorId 从路线工序节点回填（is_outsource=1 的节点配置了厂商）
        req.setVendorId(rp.getVendorId());
        req.setCardId(null);
        // sourceType 按工序码派生：SLITTING→SLITTING，其余 GENERIC，避免分切单被标成通用
        req.setSourceType(com.ruoyi.system.service.mes.wm.impl.OutsourceServiceImpl.resolveSourceType(null, rp.getProcessCode()));
        req.setIssueLines(lines);
        req.setDraft(true); // 草稿模式：不扣料，用户确认发料行后 executeOutsource 执行扣料
        try {
            WmOutsourceOrder created = outsourceService.createOutsource(req);
            return created != null ? created.getOrderCode() : null;
        } catch (com.ruoyi.common.exception.ServiceException dup) {
            // 并发：另一线程已创建同工序外协单（uk_wo_process），重查返回已有编码
            if (dup.getMessage() != null && dup.getMessage().contains("已存在外协单")) {
                WmOutsourceOrder concurrent = findExistingOrder(wo.getWorkorderId(), rp.getProcessId());
                return concurrent != null ? concurrent.getOrderCode() : null;
            }
            throw dup;
        }
    }

    /**
     * 批量 FIFO 预取：一次性查所有物料的可用库存（NORMAL、quantity_available>0），
     * 按 itemId 分组，每组取 create_time 最早的一条。消除逐行 N+1 查询。
     */
    private Map<Long, WmMaterialStock> batchFifoStocks(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            List<WmMaterialStock> all = wmMaterialStockMapper.selectByItemIds(itemIds);
            if (all == null || all.isEmpty()) {
                return new LinkedHashMap<>();
            }
            return all.stream()
                    .filter(s -> s.getQuantityAvailable() != null
                            && s.getQuantityAvailable().signum() > 0
                            && "NORMAL".equals(s.getQualityStatus()))
                    .collect(Collectors.groupingBy(
                            WmMaterialStock::getItemId,
                            Collectors.minBy(Comparator.comparing(
                                    s -> s.getCreateTime() != null ? s.getCreateTime() : new Date(0)))))
                    .entrySet().stream()
                    .filter(e -> e.getValue().isPresent())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().get(),
                            (a, b) -> a,
                            LinkedHashMap::new));
        } catch (Exception e) {
            log.debug("外协草稿批量 FIFO 预取跳过, itemIds={}", itemIds, e);
            return new LinkedHashMap<>();
        }
    }

    /**
     * 将预取的 FIFO 库存应用到发料行：填 warehouseId/batchId 等；无库存时用默认仓库兜底。
     */
    private void applyFifoStock(WmOutsourceIssueLine line, WmMaterialStock stock, Long defaultWarehouseId) {
        if (stock != null) {
            line.setWarehouseId(stock.getWarehouseId());
            line.setWarehouseCode(stock.getWarehouseCode());
            line.setWarehouseName(stock.getWarehouseName());
            line.setBatchId(stock.getBatchId());
            line.setBatchCode(stock.getBatchCode());
            return;
        }
        if (defaultWarehouseId != null) {
            line.setWarehouseId(defaultWarehouseId);
        }
    }

    /** 按 (workorderId, processId) 查已有外协订单，取最新一条 */
    private WmOutsourceOrder findExistingOrder(Long workorderId, Long processId) {
        WmOutsourceOrder q = new WmOutsourceOrder();
        q.setWorkorderId(workorderId);
        q.setProcessId(processId);
        List<WmOutsourceOrder> existing = outsourceOrderMapper.selectOutsourceOrderList(q);
        if (existing == null || existing.isEmpty()) return null;
        return existing.stream()
                .max(java.util.Comparator.comparing(WmOutsourceOrder::getOrderId))
                .orElse(null);
    }

    private Long resolveRouteId(ProWorkorder wo) {
        if (wo == null || wo.getRouteProductId() == null) {
            return null;
        }
        ProRouteProduct rp = proRouteProductService.selectProRouteProductByRecordId(wo.getRouteProductId());
        return rp != null ? rp.getRouteId() : null;
    }
}
