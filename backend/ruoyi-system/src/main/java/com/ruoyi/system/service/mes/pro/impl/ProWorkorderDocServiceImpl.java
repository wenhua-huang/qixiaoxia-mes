package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.ruoyi.common.enums.WmIssueConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;

import com.ruoyi.system.domain.mes.pro.*;
import com.ruoyi.system.domain.mes.wm.*;
import com.ruoyi.system.domain.mes.pur.*;
import com.ruoyi.system.domain.mes.pur.vo.*;
import com.ruoyi.system.mapper.mes.pro.*;
import com.ruoyi.system.mapper.mes.pur.*;
import com.ruoyi.system.mapper.mes.wm.*;
import com.ruoyi.system.service.mes.pro.*;
import com.ruoyi.system.service.mes.wm.*;
import com.ruoyi.system.service.mes.pur.*;

/**
 * 工单齐套分析 → 一键触发生成采购单/领料单/退料单/入库单 Service 实现
 *
 * @author qixiaoxia
 * @date 2026-06-30
 */
@Service
public class ProWorkorderDocServiceImpl implements IProWorkorderDocService
{
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProWorkorderDocServiceImpl.class);
    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private ProWorkorderMapper proWorkorderMapper;
    @Autowired private IProWorkorderBomService proWorkorderBomService;
    @Autowired private IProWorkorderService proWorkorderService;
    @Autowired private ProFeedbackMapper proFeedbackMapper;
    @Autowired private ProFeedbackConsumeMapper proConsumeMapper;
    @Autowired private IProTaskService proTaskService;
    @Autowired private ProRouteProcessMapper proRouteProcessMapper;
    @Autowired private ProDocGenerationLogMapper docLogMapper;

    @Autowired private IWmIssueHeaderService wmIssueHeaderService;
    @Autowired private IWmIssueLineService wmIssueLineService;
    @Autowired private IWmRtIssueService wmRtIssueService;
    @Autowired private IWmRtIssueLineService wmRtIssueLineService;
    @Autowired private IWmProductRecptService wmProductRecptService;
    @Autowired private IWmProductRecptLineService wmProductRecptLineService;
    @Autowired private WmProductRecptMapper wmProductRecptMapper;
    @Autowired private IWmMaterialStockService wmMaterialStockService;
    @Autowired private IWmWarehouseService wmWarehouseService;

    @Autowired private IPurOrderService purOrderService;
    @Autowired private IPurOrderLineService purOrderLineService;
    @Autowired private com.ruoyi.system.mapper.mes.md.MdItemVendorMapper mdItemVendorMapper;

    // ---- 单据类型常量 ----
    private static final String DOC_ISSUE = "ISSUE";
    private static final String DOC_RETURN = "RETURN";
    private static final String DOC_RECPT = "RECPT";
    private static final String DOC_PUR_ORDER = "PUR_ORDER";

    // ======================== 齐套看板 ========================

    @Override
    public ProWorkorderKitDashboardVO loadKitDashboard(Long workorderId)
    {
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) throw new ServiceException("工单不存在");

        ProWorkorderKitDashboardVO vo = new ProWorkorderKitDashboardVO();
        vo.setWorkorderId(wo.getWorkorderId());
        vo.setWorkorderCode(wo.getWorkorderCode());
        vo.setWorkorderName(wo.getWorkorderName());
        vo.setWorkorderStatus(wo.getStatus());
        vo.setPlanQuantity(wo.getQuantity());
        vo.setProductName(wo.getProductName());
        vo.setProductCode(wo.getProductCode());
        vo.setRequestDate(wo.getRequestDate() != null ? DateUtils.parseDateToStr("yyyy-MM-dd", wo.getRequestDate()) : null);

        // 1. 物料齐套分析
        List<Map<String, Object>> materialRows = checkMaterialReadiness(workorderId);
        vo.setMaterialRows(materialRows);
        int sufficient = 0, shortage = 0;
        for (Map<String, Object> row : materialRows) {
            if (Boolean.TRUE.equals(row.get("sufficient"))) sufficient++; else shortage++;
        }
        vo.setSufficientCount(sufficient);
        vo.setShortageCount(shortage);
        vo.setAllSufficient(shortage == 0);

        // 2. 采购单建议
        vo.setPurchaseRecommends(buildPurchaseRecommends(workorderId, materialRows));
        vo.setHasPurchaseRecommend(!vo.getPurchaseRecommends().isEmpty());

        // 3. 领料单状态
        List<ProKitIssueStatusVO> issueStatuses = buildIssueStatuses(workorderId);
        vo.setIssueStatuses(issueStatuses);
        vo.setHasIssueDocs(!issueStatuses.isEmpty());
        int draft = 0, confirmed = 0, posted = 0;
        for (ProKitIssueStatusVO is : issueStatuses) {
            String st = is.getStatus();
            // 兼容历史值：CONFIRMED→confirmed，POSTED→posted
            if (WmIssueConstants.STATUS_DRAFT.equals(st) || WmIssueConstants.STATUS_PENDING.equals(st)) draft++;
            else if (WmIssueConstants.STATUS_APPROVED.equals(st) || WmIssueConstants.STATUS_ALLOCATED.equals(st)
                    || "CONFIRMED".equals(st)) confirmed++;
            else if (WmIssueConstants.STATUS_PARTIAL_ISSUED.equals(st)
                    || WmIssueConstants.STATUS_ISSUED.equals(st)
                    || "POSTED".equals(st)) posted++;
        }
        vo.setIssueDraftCount(draft);
        vo.setIssueConfirmedCount(confirmed);
        vo.setIssuePostedCount(posted);

        // 4. 退料单状态
        List<ProKitReturnStatusVO> returnStatuses = buildReturnStatuses(workorderId);
        vo.setReturnStatuses(returnStatuses);
        boolean isCompleted = "COMPLETED".equals(wo.getStatus());
        vo.setReturnReady(isCompleted && hasUnconsumedMaterials(workorderId));
        vo.setHasPendingReturns(!returnStatuses.isEmpty());

        // 5. 产品入库建议
        vo.setReceiptRecommend(buildReceiptRecommend(workorderId, wo));
        vo.setReceiptReady(vo.getReceiptRecommend().isRecommended() && !vo.getReceiptRecommend().isAlreadyHasReceipt());

        return vo;
    }

    // ---- 1. 物料齐套（复用已有服务） ----
    private List<Map<String, Object>> checkMaterialReadiness(Long workorderId)
    {
        return proWorkorderService.checkMaterialReadiness(workorderId);
    }

    // ---- 2. 采购建议 ----
    private List<ProKitPurchaseRecommendVO> buildPurchaseRecommends(Long workorderId, List<Map<String, Object>> materialRows)
    {
        List<ProKitPurchaseRecommendVO> list = new ArrayList<>();
        for (Map<String, Object> row : materialRows) {
            if (Boolean.TRUE.equals(row.get("sufficient"))) continue;

            Long itemId = (Long) row.get("itemId");
            BigDecimal shortageQty = (BigDecimal) row.get("shortageQty");
            if (shortageQty == null || shortageQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            ProKitPurchaseRecommendVO rec = new ProKitPurchaseRecommendVO();
            rec.setItemId(itemId);
            rec.setItemCode((String) row.get("itemCode"));
            rec.setItemName((String) row.get("itemName"));
            rec.setUnitName((String) row.get("unitName"));
            rec.setShortageQty(shortageQty);
            rec.setRecommendedQty(shortageQty);

            // 检查是否有在途PO
            PurOrderLine lineQuery = new PurOrderLine();
            lineQuery.setItemId(itemId);
            List<PurOrderLine> lines = purOrderLineService.selectPurOrderLineList(lineQuery);
            boolean hasPending = false;
            for (PurOrderLine l : lines) {
                PurOrder po = purOrderService.selectPurOrderByOrderId(l.getOrderId());
                if (po != null && !"CANCEL".equals(po.getStatus()) && !"CLOSED".equals(po.getStatus())
                        && po.getWorkorderId() != null && po.getWorkorderId().equals(workorderId)) {
                    hasPending = true;
                    rec.setPendingPOInfo(po.getOrderCode() + "(" + po.getStatus() + ")");
                    break;
                }
            }
            rec.setHasPendingPO(hasPending);
            list.add(rec);
        }
        return list;
    }

    // ---- 3. 领料单状态 ----
    private List<ProKitIssueStatusVO> buildIssueStatuses(Long workorderId)
    {
        WmIssueHeader query = new WmIssueHeader();
        query.setWorkorderId(workorderId);
        List<WmIssueHeader> headers = wmIssueHeaderService.selectWmIssueHeaderList(query);
        List<ProKitIssueStatusVO> list = new ArrayList<>();
        for (WmIssueHeader h : headers) {
            ProKitIssueStatusVO vo = new ProKitIssueStatusVO();
            vo.setIssueId(h.getIssueId());
            vo.setIssueCode(h.getIssueCode());
            vo.setIssueName(h.getIssueName());
            vo.setProcessName(h.getWorkorderName()); // 头部存了工序名
            vo.setStatus(h.getStatus());
            vo.setTotalQuantity(h.getQuantityTotal());
            vo.setTaskId(h.getTaskId());
            list.add(vo);
        }
        return list;
    }

    // ---- 4. 退料单状态 ----
    private List<ProKitReturnStatusVO> buildReturnStatuses(Long workorderId)
    {
        WmRtIssue query = new WmRtIssue();
        query.setWorkorderId(workorderId);
        List<WmRtIssue> rts = wmRtIssueService.selectWmRtIssueList(query);
        List<ProKitReturnStatusVO> list = new ArrayList<>();
        for (WmRtIssue rt : rts) {
            ProKitReturnStatusVO vo = new ProKitReturnStatusVO();
            vo.setRtId(rt.getRtId());
            vo.setRtCode(rt.getRtCode());
            vo.setRtName(rt.getRtName());
            vo.setIssueCode(rt.getIssueCode());
            vo.setStatus(rt.getStatus());
            vo.setTotalQuantity(rt.getQuantityTotal());
            list.add(vo);
        }
        return list;
    }

    // ---- 5. 入库建议 ----
    private ProKitReceiptRecommendVO buildReceiptRecommend(Long workorderId, ProWorkorder wo)
    {
        ProKitReceiptRecommendVO vo = new ProKitReceiptRecommendVO();
        // 汇总已审核报工的合格数量
        ProFeedback fbQuery = new ProFeedback();
        fbQuery.setWorkorderId(workorderId);
        fbQuery.setStatus("AUDITED");
        List<ProFeedback> fbs = proFeedbackMapper.selectProFeedbackList(fbQuery);
        BigDecimal totalQualified = BigDecimal.ZERO;
        BigDecimal totalProduced = BigDecimal.ZERO;
        if (fbs != null) {
            for (ProFeedback fb : fbs) {
                if (fb.getQuantityQualified() != null) totalQualified = totalQualified.add(fb.getQuantityQualified());
                if (fb.getQuantityFeedback() != null) totalProduced = totalProduced.add(fb.getQuantityFeedback());
            }
        }
        vo.setProducedQty(totalProduced);
        vo.setQualifiedQty(totalQualified);

        // 检查是否已有入库单
        WmProductRecpt recptQuery = new WmProductRecpt();
        recptQuery.setWorkorderId(workorderId);
        List<WmProductRecpt> existing = wmProductRecptService.selectWmProductRecptList(recptQuery);
        boolean hasRecpt = existing != null && !existing.isEmpty();
        vo.setAlreadyHasReceipt(hasRecpt);
        if (hasRecpt) {
            vo.setExistingReceiptCode(existing.get(0).getRecptCode());
        }

        boolean recommended = totalQualified.compareTo(BigDecimal.ZERO) > 0;
        vo.setRecommended(recommended);
        return vo;
    }

    private boolean hasUnconsumedMaterials(Long workorderId)
    {
        // 简单判定：如果有已发料的领料单且工单已完成，则认为可能有未用完物料
        // 状态值由 POSTED 迁移为 ISSUED（见 WmIssueConstants）
        WmIssueHeader query = new WmIssueHeader();
        query.setWorkorderId(workorderId);
        query.setStatus(WmIssueConstants.STATUS_ISSUED);
        List<WmIssueHeader> issues = wmIssueHeaderService.selectWmIssueHeaderList(query);
        if (issues == null || issues.isEmpty()) {
            // 兼容历史 POSTED 数据
            query.setStatus("POSTED");
            issues = wmIssueHeaderService.selectWmIssueHeaderList(query);
        }
        return issues != null && !issues.isEmpty();
    }

    // ======================== 一键批量生成 ========================

    @Override
    public ProDocGenerationResultVO generateDocuments(ProDocGenerationRequestVO request)
    {
        Long workorderId = request.getWorkorderId();
        // 【Fix Finding #3/#4/#10】锁 key 统一为 "pro:workorder:doc-gen:" + wid，五个生成入口
        //   (generateDocuments / generateProductReceipt / generateMaterialReturn /
        //    generatePurchaseOrder / onFeedbackAudited) 全部共用同一 key，
        //   杜绝跨入口并发穿透造成的重复单据。
        return lockTemplate.execute("pro:workorder:doc-gen:" + workorderId, () -> {
            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.setTimeout(30);
            return tt.execute(status -> {
                String batch = UUID.randomUUID().toString();
                ProDocGenerationResultVO result = new ProDocGenerationResultVO();
                result.setGenerationBatch(batch);

                if (request.isGenerateIssue()) {
                    result.setIssues(generateIssueDocuments(workorderId, batch));
                }
                if (request.isGeneratePurOrder()) {
                    result.setPurOrders(generatePurOrderDocuments(workorderId, batch));
                }
                if (request.isGenerateReturn()) {
                    result.setReturns(generateReturnDocuments(workorderId, batch));
                }
                if (request.isGenerateReceipt()) {
                    // 批量入口无 feedbackId → 走手动补录语义 (未入库差额)
                    result.setReceipts(generateReceiptDocuments(workorderId, null, batch));
                }

                StringBuilder msg = new StringBuilder();
                msg.append("批次 ").append(batch.substring(0, 8)).append(": ");
                int total = 0;
                if (result.getIssues() != null) total += result.getIssues().size();
                if (result.getPurOrders() != null) total += result.getPurOrders().size();
                if (result.getReturns() != null) total += result.getReturns().size();
                if (result.getReceipts() != null) total += result.getReceipts().size();
                msg.append("共生成 ").append(total).append(" 张单据");
                result.setMessage(msg.toString());
                return result;
            });
        });
    }

    // ---- 生成领料单 ----
    private List<Map<String, Object>> generateIssueDocuments(Long workorderId, String batch)
    {
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        List<ProWorkorderBom> bomList = proWorkorderBomService.selectProWorkorderBomByWorkorderId(workorderId);
        if (bomList == null || bomList.isEmpty()) return new ArrayList<>();

        // 按 processId 分组
        Map<Long, List<ProWorkorderBom>> grouped = new LinkedHashMap<>();
        for (ProWorkorderBom bom : bomList) {
            Long pid = bom.getProcessId() != null ? bom.getProcessId() : 0L;
            grouped.computeIfAbsent(pid, k -> new ArrayList<>()).add(bom);
        }

        Long defaultWarehouseId = findDefaultWarehouse(wo.getFactoryId());
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<Long, List<ProWorkorderBom>> entry : grouped.entrySet()) {
            Long processId = entry.getKey();
            if (processId == 0L) continue;
            List<ProWorkorderBom> processBoms = entry.getValue();
            String processName = processBoms.get(0).getProcessName();

            // 找排产任务
            ProTask taskQuery = new ProTask();
            taskQuery.setWorkorderId(workorderId);
            taskQuery.setProcessId(processId);
            List<ProTask> tasks = proTaskService.selectProTaskList(taskQuery);
            ProTask task = (tasks != null && !tasks.isEmpty()) ? tasks.get(0) : null;

            // 幂等检查：查询已有领料单（WmIssueHeader 按 workorderId + taskId 查重）
            if (task != null) {
                WmIssueHeader existQuery = new WmIssueHeader();
                existQuery.setWorkorderId(workorderId);
                existQuery.setTaskId(task.getTaskId());
                List<WmIssueHeader> existings = wmIssueHeaderService.selectWmIssueHeaderList(existQuery);
                if (existings != null && !existings.isEmpty()) {
                    for (WmIssueHeader eh : existings) {
                        if (!hasLog(workorderId, DOC_ISSUE, eh.getIssueId())) {
                            insertLog(workorderId, DOC_ISSUE, eh.getIssueId(), eh.getIssueCode(), batch);
                        }
                    }
                    continue;
                }
            }

            // 创建领料单头
            String issueCode = genCode("LL");
            WmIssueHeader header = new WmIssueHeader();
            header.setFactoryId(wo.getFactoryId());
            header.setIssueCode(issueCode);
            header.setIssueName(wo.getWorkorderName() + "-" + (processName != null ? processName : "工序" + processId));
            header.setIssueType("PRODUCE");
            header.setWorkorderId(wo.getWorkorderId());
            header.setWorkorderCode(wo.getWorkorderCode());
            header.setWorkorderName(wo.getWorkorderName());
            if (task != null) {
                header.setTaskId(task.getTaskId());
                header.setTaskCode(task.getTaskCode());
                header.setWorkstationId(task.getWorkstationId());
                header.setWorkstationCode(task.getWorkstationCode());
                header.setWorkstationName(task.getWorkstationName());
            }
            header.setWarehouseId(defaultWarehouseId);
            header.setStatus("DRAFT");
            header.setIssueDate(new Date());
            wmIssueHeaderService.insertWmIssueHeader(header);

            // 创建行
            BigDecimal totalQty = BigDecimal.ZERO;
            for (ProWorkorderBom bom : processBoms) {
                WmIssueLine line = new WmIssueLine();
                line.setIssueId(header.getIssueId());
                line.setFactoryId(wo.getFactoryId());
                line.setItemId(bom.getItemId());
                line.setItemCode(bom.getItemCode());
                line.setItemName(bom.getItemName());
                line.setUnitOfMeasure(bom.getUnitOfMeasure());
                line.setUnitName(bom.getUnitName());
                line.setQuantityIssue(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO);
                line.setWarehouseId(defaultWarehouseId);
                wmIssueLineService.insertWmIssueLine(line);
                totalQty = totalQty.add(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO);
            }
            header.setQuantityTotal(totalQty);
            wmIssueHeaderService.updateWmIssueHeader(header);

            insertLog(workorderId, DOC_ISSUE, header.getIssueId(), issueCode, batch);

            Map<String, Object> info = new HashMap<>();
            info.put("issueId", header.getIssueId());
            info.put("issueCode", issueCode);
            info.put("processId", processId);
            info.put("processName", processName);
            info.put("lineCount", processBoms.size());
            result.add(info);
        }
        return result;
    }

    // ---- 生成采购单 ----
    private List<Map<String, Object>> generatePurOrderDocuments(Long workorderId, String batch)
    {
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        List<Map<String, Object>> materialRows = checkMaterialReadiness(workorderId);

        // 过滤缺料且无在途PO的项
        List<ProKitPurchaseRecommendVO> recommends = buildPurchaseRecommends(workorderId, materialRows);
        List<ProKitPurchaseRecommendVO> toPurchase = recommends.stream()
                .filter(r -> !r.isHasPendingPO()).collect(Collectors.toList());
        if (toPurchase.isEmpty()) return new ArrayList<>();

        // 幂等检查
        if (hasLog(workorderId, DOC_PUR_ORDER, 0L)) return new ArrayList<>();

        // 创建一张采购单，包含所有缺料行
        String orderCode = genCode("PO");
        PurOrder po = new PurOrder();
        po.setOrderCode(orderCode);
        po.setOrderName(wo.getWorkorderName() + "-工单缺料采购");
        po.setWorkorderId(workorderId);
        po.setWorkorderCode(wo.getWorkorderCode());
        po.setVendorId(0L);  // 供应商待定，采购员审核时填写
        po.setOrderDate(new Date());
        po.setStatus("DRAFT");
        po.setCurrency("CNY");
        purOrderService.insertPurOrder(po);

        BigDecimal totalQty = BigDecimal.ZERO;
        for (ProKitPurchaseRecommendVO rec : toPurchase) {
            PurOrderLine line = new PurOrderLine();
            line.setOrderId(po.getOrderId());
            line.setItemId(rec.getItemId());
            line.setItemCode(rec.getItemCode());
            line.setItemName(rec.getItemName());
            line.setUnitOfMeasure(rec.getUnitName());
            line.setUnitName(rec.getUnitName());
            line.setQuantityOrdered(rec.getRecommendedQty());
            line.setStatus("ORDERED");
            purOrderLineService.insertPurOrderLine(line);
            totalQty = totalQty.add(rec.getRecommendedQty());
        }
        po.setTotalQuantity(totalQty);
        purOrderService.updatePurOrder(po);

        insertLog(workorderId, DOC_PUR_ORDER, po.getOrderId(), orderCode, batch);

        Map<String, Object> info = new HashMap<>();
        info.put("orderId", po.getOrderId());
        info.put("orderCode", orderCode);
        info.put("lineCount", toPurchase.size());
        info.put("totalQuantity", totalQty);
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(info);
        return result;
    }

    // ---- 生成退料单 ----
    private List<Map<String, Object>> generateReturnDocuments(Long workorderId, String batch)
    {
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        List<Map<String, Object>> result = new ArrayList<>();

        // 查询已发料的领料单（状态值由 POSTED 迁移为 ISSUED，兼容历史数据）
        WmIssueHeader issueQuery = new WmIssueHeader();
        issueQuery.setWorkorderId(workorderId);
        issueQuery.setStatus(WmIssueConstants.STATUS_ISSUED);
        List<WmIssueHeader> issues = wmIssueHeaderService.selectWmIssueHeaderList(issueQuery);
        if (issues == null || issues.isEmpty()) {
            // 兼容历史 POSTED 数据
            issueQuery.setStatus("POSTED");
            issues = wmIssueHeaderService.selectWmIssueHeaderList(issueQuery);
        }
        if (issues == null || issues.isEmpty()) return result;

        Long defaultWarehouseId = findDefaultWarehouse(wo.getFactoryId());

        // 【Fix #10】提前加载消耗量：一次查询所有已审核报工 + 批量加载物料消耗
        Map<Long, BigDecimal> consumedByItem = new HashMap<>();
        ProFeedback fbPreload = new ProFeedback();
        fbPreload.setWorkorderId(workorderId);
        fbPreload.setStatus("AUDITED");
        List<ProFeedback> workorderFbs = proFeedbackMapper.selectProFeedbackList(fbPreload);
        if (workorderFbs != null) {
            for (ProFeedback wfb : workorderFbs) {
                List<ProFeedbackConsume> clist = proConsumeMapper.selectByFeedbackId(wfb.getRecordId());
                if (clist != null) {
                    for (ProFeedbackConsume c : clist) {
                        if (c.getItemId() != null && c.getQuantity() != null) {
                            consumedByItem.merge(c.getItemId(), c.getQuantity(), BigDecimal::add);
                        }
                    }
                }
            }
        }

        for (WmIssueHeader issue : issues) {
            // 幂等检查
            if (hasLog(workorderId, DOC_RETURN, issue.getIssueId())) continue;

            // 计算已领 vs 已消耗
            WmIssueLine lineQuery = new WmIssueLine();
            lineQuery.setIssueId(issue.getIssueId());
            List<WmIssueLine> issueLines = wmIssueLineService.selectWmIssueLineList(lineQuery);
            if (issueLines == null || issueLines.isEmpty()) continue;

            List<WmRtIssueLine> returnLines = new ArrayList<>();
            BigDecimal totalRtQty = BigDecimal.ZERO;

            for (WmIssueLine il : issueLines) {
                BigDecimal consumed = consumedByItem.getOrDefault(il.getItemId(), BigDecimal.ZERO);
                BigDecimal issued = il.getQuantityIssue() != null ? il.getQuantityIssue() : BigDecimal.ZERO;
                BigDecimal unused = issued.subtract(consumed);
                if (unused.compareTo(BigDecimal.ZERO) > 0) {
                    WmRtIssueLine rtLine = new WmRtIssueLine();
                    rtLine.setItemId(il.getItemId());
                    rtLine.setItemCode(il.getItemCode());
                    rtLine.setItemName(il.getItemName());
                    rtLine.setUnitOfMeasure(il.getUnitOfMeasure());
                    rtLine.setUnitName(il.getUnitName());
                    rtLine.setQuantityRt(unused);
                    rtLine.setWarehouseId(defaultWarehouseId);
                    returnLines.add(rtLine);
                    totalRtQty = totalRtQty.add(unused);
                }
            }

            if (returnLines.isEmpty()) continue;

            // 创建退料单
            String rtCode = genCode("RT");
            WmRtIssue rt = new WmRtIssue();
            rt.setFactoryId(wo.getFactoryId());
            rt.setRtCode(rtCode);
            rt.setRtName(wo.getWorkorderName() + "-退料");
            rt.setIssueId(issue.getIssueId());
            rt.setIssueCode(issue.getIssueCode());
            rt.setWorkorderId(workorderId);
            rt.setWorkorderCode(wo.getWorkorderCode());
            rt.setWorkorderName(wo.getWorkorderName());
            rt.setWorkstationId(issue.getWorkstationId());
            rt.setWarehouseId(defaultWarehouseId);
            rt.setRtDate(new Date());
            rt.setQuantityTotal(totalRtQty);
            rt.setStatus("DRAFT");
            wmRtIssueService.insertWmRtIssue(rt);

            for (WmRtIssueLine rl : returnLines) {
                rl.setRtId(rt.getRtId());
                rl.setFactoryId(wo.getFactoryId());
                wmRtIssueLineService.insertWmRtIssueLine(rl);
            }

            // 写幂等日志 (DuplicateKeyException 已由 insertLog 统一兜底)
            insertLog(workorderId, DOC_RETURN, rt.getRtId(), rtCode, batch);

            Map<String, Object> info = new HashMap<>();
            info.put("rtId", rt.getRtId());
            info.put("rtCode", rtCode);
            info.put("lineCount", returnLines.size());
            info.put("totalQuantity", totalRtQty);
            result.add(info);
        }
        return result;
    }

    // ---- 生成产品入库单 ----
    /**
     * 生成产品入库单。幂等键为 feedback 级 (source_feedback_id)：
     * <ul>
     *   <li>feedbackId != null (末工序报工审核自动触发)：
     *       数量 = min(fb.quantityQualified, planned - alreadyRecpt)，即封顶到计划量差额，
     *       避免与手动补录混用时超量入库 (Fix Finding #2)。
     *       同一 feedback 已生成过 <b>且未被取消</b> 则返回既有信息 (走 buildExistingRecptInfo)；
     *       若已取消则允许重新生成 (Fix Finding #5)。
     *       Index A (uk_doc_log_wo_type_feedback) 兜底并发。
     *   <li>feedbackId == null (手动补录入口，前端"生成入库单"按钮)：
     *       数量 = produced - alreadyRecpt (已生产未入库差额，含 DRAFT/CONFIRMED/POSTED)；
     *       靠应用层 + Redis 锁串行；Index A 对 NULL 值不去重，DB 不兜底 (锁保证串行)。
     * </ul>
     */
    private List<Map<String, Object>> generateReceiptDocuments(Long workorderId, Long feedbackId, String batch)
    {
        // 先做幂等检查 (Fix Finding #10：命中时避免无用工单/仓库查询)。
        // 【Fix #5】用 buildExistingRecptInfo 结果非空判定 (已过滤 CANCEL)，允许对已取消单据重新生成。
        List<Map<String, Object>> existing = buildExistingRecptInfo(workorderId, feedbackId);
        if (!existing.isEmpty()) return existing;

        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) {
            // 【Fix Finding #9】feedback 已 audit 但工单查不到 → 数据异常，需要告警
            log.warn("生成入库单时工单不存在, workorderId={}, feedbackId={}", workorderId, feedbackId);
            return new ArrayList<>();
        }

        BigDecimal planned = wo.getQuantity() != null ? wo.getQuantity() : BigDecimal.ZERO;
        BigDecimal produced = wo.getQuantityProduced() != null ? wo.getQuantityProduced() : BigDecimal.ZERO;
        BigDecimal alreadyRecpt = sumReceiptedQty(workorderId);

        BigDecimal qtyToRecpt;
        if (feedbackId != null) {
            // 自动路径：cap = min(本次合格数, 已生产量-已入库)
            // 用 produced 替代 planned 做上限：允许超产成品（produced > planned）正常入库，
            // 避免"账外物料"（产出存在但系统无入库凭证）；同时防止入库量超过实际产出（防重复计数）。
            // produced 在外层事务已由 addQuantityProduced 累加提交，REQUIRES_NEW 下可读到最新值。
            ProFeedback fb = proFeedbackMapper.selectProFeedbackByRecordId(feedbackId);
            if (fb == null || fb.getQuantityQualified() == null) return new ArrayList<>();
            BigDecimal qualified = fb.getQuantityQualified();
            BigDecimal remaining = produced.subtract(alreadyRecpt);
            qtyToRecpt = qualified.min(remaining);
        } else {
            // 手动路径：入库「已生产但未入库」的差额 (与自动路径语义对齐，均以 produced 为上限)
            qtyToRecpt = produced.subtract(alreadyRecpt);
        }

        if (qtyToRecpt.compareTo(BigDecimal.ZERO) <= 0) {
            // 【Fix Finding #8】自动路径合格数被封顶到 0 / 手动路径无差额: 记录审计日志便于运维排查
            log.info("跳过生成入库单 (差额为 0), workorderId={}, feedbackId={}, planned={}, produced={}, alreadyRecpt={}",
                    workorderId, feedbackId, planned, produced, alreadyRecpt);
            return new ArrayList<>();
        }

        Long defaultWarehouseId = findDefaultWarehouse(wo.getFactoryId());

        // 创建产品入库单
        String recptCode = genCode("PR");
        WmProductRecpt recpt = new WmProductRecpt();
        recpt.setRecptCode(recptCode);
        recpt.setRecptName(wo.getWorkorderName() + "-生产入库");
        recpt.setProduceId(wo.getProductId());
        recpt.setProduceCode(wo.getProductCode());
        recpt.setWorkorderId(workorderId);
        recpt.setWorkorderCode(wo.getWorkorderCode());
        recpt.setWarehouseId(defaultWarehouseId);
        recpt.setRecptDate(new Date());
        recpt.setTotalQuantity(qtyToRecpt);
        recpt.setTotalBox(0);
        recpt.setStatus("DRAFT");
        wmProductRecptService.insertWmProductRecpt(recpt);

        // 创建入库单行
        WmProductRecptLine recptLine = new WmProductRecptLine();
        recptLine.setRecptId(recpt.getRecptId());
        recptLine.setItemId(wo.getProductId());
        recptLine.setItemCode(wo.getProductCode());
        recptLine.setItemName(wo.getProductName());
        recptLine.setUnitOfMeasure(wo.getUnitOfMeasure());
        recptLine.setUnitName(wo.getUnitName());
        recptLine.setQuantityRecpt(qtyToRecpt);
        recptLine.setWarehouseId(defaultWarehouseId);
        wmProductRecptLineService.insertWmProductRecptLine(recptLine);

        // CANCEL 再生：撤销同一 feedbackId 下的旧 ACTIVE log，避免唯一索引冲突
        if (feedbackId != null) {
            docLogMapper.revokeBySourceFeedbackId(workorderId, DOC_RECPT, feedbackId);
        }
        // 写幂等日志 (DuplicateKeyException 已由 insertLog 统一兜底转 ServiceException)
        insertLog(workorderId, DOC_RECPT, recpt.getRecptId(), recptCode, feedbackId, batch);

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> info = new HashMap<>();
        info.put("recptId", recpt.getRecptId());
        info.put("recptCode", recptCode);
        info.put("totalQuantity", qtyToRecpt);
        result.add(info);
        return result;
    }

    // ---- 单独生成 ----

    @Override
    public Map<String, Object> buildPurOrderWizard(Long workorderId) {
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        List<Map<String, Object>> materialRows = checkMaterialReadiness(workorderId);

        // 收集缺料物料，为每个物料查找首选供应商
        List<Map<String, Object>> wizardLines = new ArrayList<>();
        Map<Long, List<Map<String, Object>>> vendorGroups = new LinkedHashMap<>();

        for (Map<String, Object> row : materialRows) {
            if (Boolean.TRUE.equals(row.get("sufficient"))) continue;
            BigDecimal shortage = (BigDecimal) row.get("shortageQty");
            if (shortage == null || shortage.compareTo(BigDecimal.ZERO) <= 0) continue;

            Long itemId = (Long) row.get("itemId");
            // 查该物料的首选供应商
            com.ruoyi.system.domain.mes.md.MdItemVendor ivQuery = new com.ruoyi.system.domain.mes.md.MdItemVendor();
            ivQuery.setItemId(itemId);
            ivQuery.setIsPreferred("Y");
            List<com.ruoyi.system.domain.mes.md.MdItemVendor> ivList = mdItemVendorMapper.selectList(ivQuery);
            com.ruoyi.system.domain.mes.md.MdItemVendor preferred = (ivList != null && !ivList.isEmpty()) ? ivList.get(0) : null;

            Map<String, Object> line = new HashMap<>();
            line.put("itemId", itemId);
            line.put("itemCode", row.get("itemCode"));
            line.put("itemName", row.get("itemName"));
            line.put("unitName", row.get("unitName"));
            line.put("unitOfMeasure", row.get("unitName")); // 物料齐套检查结果中 unitName 即单位编码
            line.put("shortageQty", shortage);
            line.put("recommendedQty", shortage);
            line.put("minOrderQty", preferred != null ? preferred.getMinOrderQty() : null);
            line.put("leadTimeDays", preferred != null ? preferred.getLeadTimeDays() : null);
            line.put("vendorId", preferred != null ? preferred.getVendorId() : null);
            line.put("vendorCode", preferred != null ? preferred.getVendorCode() : null);
            line.put("vendorName", preferred != null ? preferred.getVendorName() : null);
            line.put("hasVendor", preferred != null);
            line.put("checked", preferred != null); // 有供应商的默认勾选

            wizardLines.add(line);

            Long vid = preferred != null ? preferred.getVendorId() : 0L;
            vendorGroups.computeIfAbsent(vid, k -> new ArrayList<>()).add(line);
        }

        // 标记已生成 PO 的物料
        PurOrder poQuery = new PurOrder();
        poQuery.setWorkorderId(workorderId);
        List<PurOrderVO> existingPOs = purOrderService.selectPurOrderList(poQuery);
        Map<Long, String> itemPOStatus = new HashMap<>();
        if (existingPOs != null) {
            for (PurOrder po : existingPOs) {
                PurOrderLine lineQuery = new PurOrderLine();
                lineQuery.setOrderId(po.getOrderId());
                List<PurOrderLine> poLines = purOrderLineService.selectPurOrderLineList(lineQuery);
                if (poLines != null) {
                    for (PurOrderLine pl : poLines) {
                        itemPOStatus.putIfAbsent(pl.getItemId(), po.getOrderCode());
                    }
                }
            }
        }
        for (Map<String, Object> line : wizardLines) {
            Long itemId = (Long) line.get("itemId");
            String existingPO = itemPOStatus.get(itemId);
            line.put("hasExistingPO", existingPO != null);
            line.put("existingPOCode", existingPO);
            // 已有 PO 的物料默认不勾选
            if (existingPO != null) line.put("checked", false);
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("workorderId", workorderId);
        result.put("workorderCode", wo.getWorkorderCode());
        result.put("lines", wizardLines);
        result.put("vendorGroups", new ArrayList<>(vendorGroups.values()));
        result.put("vendorCount", vendorGroups.size());
        return result;
    }

    @Override
    public Map<String, Object> submitPurOrderWizard(Long workorderId, java.util.List<PurOrderWizardLineVO> lines) {
        // 【Fix Finding #3 + CLAUDE.md】移除方法级 @Transactional，先锁后事务（与其它四入口共用 pro:workorder:doc-gen: 锁）。
        return lockTemplate.execute("pro:workorder:doc-gen:" + workorderId, () -> {
            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.setTimeout(30);
            return tt.execute(status -> doSubmitPurOrderWizard(workorderId, lines));
        });
    }

    private Map<String, Object> doSubmitPurOrderWizard(Long workorderId, java.util.List<PurOrderWizardLineVO> lines) {
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (lines == null || lines.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("message", "无提交行"); return empty;
        }
        String batch = UUID.randomUUID().toString();

        // 按 vendorId 分组
        Map<Long, List<PurOrderWizardLineVO>> groups = new LinkedHashMap<>();
        for (PurOrderWizardLineVO line : lines) {
            Long vid = line.getVendorId() != null ? line.getVendorId() : 0L;
            groups.computeIfAbsent(vid, k -> new ArrayList<>()).add(line);
        }

        List<Map<String, Object>> created = new ArrayList<>();
        for (Map.Entry<Long, List<PurOrderWizardLineVO>> entry : groups.entrySet()) {
            Long vendorId = entry.getKey();
            List<PurOrderWizardLineVO> groupLines = entry.getValue();
            PurOrderWizardLineVO first = groupLines.get(0);

            String orderCode = genCode("PO");
            PurOrder po = new PurOrder();
            po.setOrderCode(orderCode);
            po.setOrderName(wo.getWorkorderName() + "-采购");
            po.setWorkorderId(workorderId);
            po.setWorkorderCode(wo.getWorkorderCode());
            po.setVendorId(vendorId > 0 ? vendorId : 0L);
            po.setVendorCode(vendorId > 0 ? first.getVendorCode() : null);
            po.setVendorName(vendorId > 0 ? first.getVendorName() : "待定");
            po.setOrderDate(new Date());
            po.setStatus("DRAFT");
            po.setCurrency("CNY");
            purOrderService.insertPurOrder(po);

            BigDecimal totalQty = BigDecimal.ZERO;
            for (PurOrderWizardLineVO l : groupLines) {
                PurOrderLine line = new PurOrderLine();
                line.setOrderId(po.getOrderId());
                line.setItemId(l.getItemId());
                line.setItemCode(l.getItemCode());
                line.setItemName(l.getItemName());
                line.setUnitOfMeasure(l.getUnitOfMeasure());
                line.setUnitName(l.getUnitName());
                BigDecimal qty = l.getQuantity() != null ? l.getQuantity() : BigDecimal.ZERO;
                line.setQuantityOrdered(qty);
                line.setStatus("ORDERED");
                purOrderLineService.insertPurOrderLine(line);
                totalQty = totalQty.add(qty);
            }
            po.setTotalQuantity(totalQty);
            purOrderService.updatePurOrder(po);
            insertLog(workorderId, DOC_PUR_ORDER, po.getOrderId(), orderCode, batch);

            Map<String, Object> info = new HashMap<>();
            info.put("orderId", po.getOrderId()); info.put("orderCode", orderCode);
            info.put("vendorId", vendorId); info.put("vendorName", vendorId > 0 ? first.getVendorName() : "待定");
            info.put("lineCount", groupLines.size()); info.put("totalQuantity", totalQty);
            created.add(info);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("purOrders", created);
        result.put("message", "共生成 " + created.size() + " 张采购单");
        return result;
    }

    @Override
    public Map<String, Object> generatePurchaseOrder(Long workorderId) {
        // 【Fix Finding #3】统一锁 key 为 pro:workorder:doc-gen: (五入口互斥)。
        // 【Fix Finding + CLAUDE.md】移除方法级 @Transactional，先锁后事务（避免事务先于锁开启）。
        return lockTemplate.execute("pro:workorder:doc-gen:" + workorderId, () -> {
            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.setTimeout(30);
            return tt.execute(status -> {
                String batch = UUID.randomUUID().toString();
                List<Map<String, Object>> pos = generatePurOrderDocuments(workorderId, batch);
                Map<String, Object> result = new HashMap<>();
                result.put("generationBatch", batch);
                result.put("purOrders", pos);
                return result;
            });
        });
    }

    @Override
    public WmProductRecpt generateProductReceipt(Long workorderId) {
        // 【Fix Finding #3】统一锁 key 为 pro:workorder:doc-gen: (五入口互斥)。
        return lockTemplate.execute("pro:workorder:doc-gen:" + workorderId, () -> {
            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.setTimeout(30);
            return tt.execute(status -> {
                String batch = UUID.randomUUID().toString();
                // Controller 手动入口 (前端"生成入库单"按钮) 无 feedbackId → 手动补录语义
                List<Map<String, Object>> recpts = generateReceiptDocuments(workorderId, null, batch);
                if (recpts.isEmpty()) return null;
                Long recptId = (Long) recpts.get(0).get("recptId");
                return wmProductRecptService.selectWmProductRecptByRecptId(recptId);
            });
        });
    }

    @Override
    public WmRtIssue generateMaterialReturn(Long workorderId) {
        // 【Fix Finding #4】统一锁 key 为 pro:workorder:doc-gen: (五入口互斥, 与其它 4 入口的退料生成路径互斥)。
        return lockTemplate.execute("pro:workorder:doc-gen:" + workorderId, () -> {
            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.setTimeout(30);
            return tt.execute(status -> {
                String batch = UUID.randomUUID().toString();
                List<Map<String, Object>> returns = generateReturnDocuments(workorderId, batch);
                if (returns.isEmpty()) return null;
                Long rtId = (Long) returns.get(0).get("rtId");
                return wmRtIssueService.selectWmRtIssueByRtId(rtId);
            });
        });
    }

    // ---- 报工审核后自动触发 ----

    @Override
    public List<Map<String, Object>> onFeedbackAudited(Long feedbackId)
    {
        ProFeedback fb = proFeedbackMapper.selectProFeedbackByRecordId(feedbackId);
        if (fb == null) return new ArrayList<>();

        // 判断是否末工序
        if (fb.getRouteId() == null || fb.getProcessId() == null) return new ArrayList<>();
        ProRouteProcess lastProcess = proRouteProcessMapper.selectLastProcessByRouteId(fb.getRouteId());
        if (lastProcess == null || !lastProcess.getProcessId().equals(fb.getProcessId()))
            return new ArrayList<>();

        // 职责边界 (Fix #1/#2/#4)：本方法只负责生成入库单/退料单，**不更新 workorder**。
        //  - addQuantityProduced / 完工判定 由外层 auditFeedback 同一事务负责 (autoCompleteWorkorderIfQualified)，
        //    避免外层 addQuantityProduced 与本内层 updateProWorkorder 争 workorder 行锁 (InnoDB 死锁)。
        //  - 本方法用 REQUIRES_NEW 独立事务：单据生成失败时只回滚单据，不影响外层审核提交。
        //  - 内层完全不写 workorder，故无死锁；单据生成全程幂等 (feedback 级 + Index A)。
        final Long wid = fb.getWorkorderId();
        return lockTemplate.execute("pro:workorder:doc-gen:" + wid, () -> {
            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tt.setTimeout(30);
            return tt.execute(status -> {
                String batch = UUID.randomUUID().toString();
                List<Map<String, Object>> result = new ArrayList<>();
                result.addAll(generateReceiptDocuments(wid, feedbackId, batch));
                result.addAll(generateReturnDocuments(wid, batch));
                return result;
            });
        });
    }

    /**
     * 自动完工判定：仅当 quantity_produced >= 计划量时，精准条件 UPDATE 置 COMPLETED。
     * <p>由 auditFeedback 在 addQuantityProduced 之后、同一外层事务内调用 (Fix Finding #1/#2)：
     * <ul>
     *   <li>同事务读 quantity_produced 可见 addQuantityProduced 未提交值，无读脏；
     *   <li>精准条件 UPDATE (WHERE status='PRODUCING')，无 full-entity 覆盖 (TOCTOU)；
     *   <li>幂等：并发多 feedback 同时达标时，仅一个 UPDATE 命中，返回 0 即跳过。
     * </ul>
     */
    @Override
    public void autoCompleteWorkorderIfQualified(Long workorderId)
    {
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) return;
        BigDecimal produced = wo.getQuantityProduced() != null ? wo.getQuantityProduced() : BigDecimal.ZERO;
        BigDecimal planned = wo.getQuantity() != null ? wo.getQuantity() : BigDecimal.ZERO;
        if (produced.compareTo(planned) < 0) return;
        proWorkorderMapper.completeWorkorderIfProducing(
                workorderId,
                new Date(),
                SecurityUtils.getUsername(),
                DateUtils.getNowDate());
    }

    // ======================== 工具方法 ========================

    private Long findDefaultWarehouse(Long factoryId)
    {
        WmWarehouse query = new WmWarehouse();
        if (factoryId != null) query.setFactoryId(factoryId);
        List<WmWarehouse> warehouses = wmWarehouseService.selectWmWarehouseList(query);
        if (warehouses != null && !warehouses.isEmpty()) return warehouses.get(0).getWarehouseId();
        List<WmWarehouse> all = wmWarehouseService.selectWmWarehouseAll();
        if (all != null && !all.isEmpty()) return all.get(0).getWarehouseId();
        return 1L;
    }

    private String genCode(String prefix)
    {
        String ts = DateUtils.dateTimeNow("yyyyMMddHHmmssSSS");
        return prefix + ts.substring(2);
    }

    // ---- 幂等日志操作 ----

    private boolean hasLog(Long workorderId, String docType, Long docId)
    {
        ProDocGenerationLog query = new ProDocGenerationLog();
        query.setWorkorderId(workorderId);
        query.setDocType(docType);
        List<ProDocGenerationLog> logs = docLogMapper.selectList(query);
        if (logs == null) return false;
        if (docId > 0) {
            return logs.stream().anyMatch(l -> docId.equals(l.getDocId()));
        }
        return !logs.isEmpty();
    }

    private void insertLog(Long workorderId, String docType, Long docId, String docCode, String batch)
    {
        insertLog(workorderId, docType, docId, docCode, null, batch);
    }

    /**
     * 写幂等日志。统一兜底 DuplicateKeyException (Fix Finding #4/#6)：
     * 五入口共用 pro:workorder:doc-gen: 锁串行执行，正常不冲突；锁失效等异常并发穿透时，
     * 转 ServiceException 让 GlobalExceptionHandler 返回友好提示，而非 HTTP 500。
     */
    private void insertLog(Long workorderId, String docType, Long docId, String docCode, Long sourceFeedbackId, String batch)
    {
        ProDocGenerationLog log = new ProDocGenerationLog();
        log.setWorkorderId(workorderId);
        log.setDocType(docType);
        log.setDocId(docId);
        log.setDocCode(docCode);
        log.setSourceFeedbackId(sourceFeedbackId);
        log.setGenerationBatch(batch);
        log.setStatus("ACTIVE");
        try {
            docLogMapper.insert(log);
        } catch (org.springframework.dao.DuplicateKeyException dup) {
            throw new ServiceException("单据正在生成中，请稍后重试");
        }
    }

    // ---- feedback 级入库单幂等辅助 ----

    /**
     * 查该 (workorder, feedback) 已生成过的 RECPT 类型 log 列表。
     * feedbackId == null 时用专用 mapper 精确匹配 source_feedback_id IS NULL，避免 Java 端过滤 (Fix Finding #11)。
     * 供 buildExistingRecptInfo 复用，避免重复查询 (Fix Finding #9)。
     */
    private List<ProDocGenerationLog> findReceiptLogsForFeedback(Long workorderId, Long feedbackId)
    {
        if (feedbackId == null) {
            // 手动补录：SQL 直接 source_feedback_id IS NULL，不拉全量再 Java 过滤
            List<ProDocGenerationLog> logs = docLogMapper.selectManualReceiptLogs(workorderId, DOC_RECPT);
            return logs != null ? logs : Collections.emptyList();
        }
        // 自动路径：selectList 会拼 source_feedback_id = ? 精确匹配
        ProDocGenerationLog query = new ProDocGenerationLog();
        query.setWorkorderId(workorderId);
        query.setDocType(DOC_RECPT);
        query.setSourceFeedbackId(feedbackId);
        List<ProDocGenerationLog> logs = docLogMapper.selectList(query);
        return logs != null ? logs : Collections.emptyList();
    }

    /**
     * 返回已存在入库单的信息 (幂等命中场景)。
     * 【Fix Finding #6】过滤 log 关联的入库单已被物理删除的脏 log。
     * 【Fix Finding #5】过滤 status='CANCEL' 的已取消入库单，允许用户对已取消的单据重新生成。
     * 只把 DRAFT/CONFIRMED/POSTED (与 sumQuantityByWorkorderId 语义一致) 视为「有效已存在单据」。
     */
    private List<Map<String, Object>> buildExistingRecptInfo(Long workorderId, Long feedbackId)
    {
        List<ProDocGenerationLog> logs = findReceiptLogsForFeedback(workorderId, feedbackId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProDocGenerationLog log : logs) {
            // 确认入库单行仍存在且未取消 (数据完整性检查)
            WmProductRecpt recpt = wmProductRecptService.selectWmProductRecptByRecptId(log.getDocId());
            if (recpt == null) continue;                      // log 指向已删单据 → 跳过
            String st = recpt.getStatus();
            if (!"DRAFT".equals(st) && !"CONFIRMED".equals(st) && !"POSTED".equals(st)) continue;  // 排除 CANCEL 等
            Map<String, Object> info = new HashMap<>();
            info.put("recptId", log.getDocId());
            info.put("recptCode", log.getDocCode());
            info.put("message", "已存在入库单");
            result.add(info);
        }
        return result;
    }

    /**
     * 汇总某工单已生成的入库单总量。
     * 语义：仅统计 status IN ('DRAFT','CONFIRMED','POSTED')；NULL 及其它状态 (含 CANCEL) 均排除。
     * 详见 WmProductRecptMapper.sumQuantityByWorkorderId XML 注释。
     */
    private BigDecimal sumReceiptedQty(Long workorderId)
    {
        BigDecimal sum = wmProductRecptMapper.sumQuantityByWorkorderId(workorderId);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
