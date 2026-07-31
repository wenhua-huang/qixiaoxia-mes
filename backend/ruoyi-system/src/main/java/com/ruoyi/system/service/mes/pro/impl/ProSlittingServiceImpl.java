package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.TransactionTypeEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProSlittingRecord;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.SlittingRequest;
import com.ruoyi.system.domain.mes.pro.SlittingRequest.ChildRollSpec;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmRollDetail;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProSlittingRecordMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmRollDetailMapper;
import com.ruoyi.system.service.mes.pro.IProSlittingService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IWmTransactionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 分切作业 Service（库存驱动 + 报工自动建卷）
 *
 * <p>核心流程（单锁单事务原子执行）：
 * <pre>
 * 1. Redis 锁 wm:slitting:{pickBatch}
 * 2. 事务内：
 *    a. 领料出库：SPLIT 事务扣减 material_stock(母卷物料).onhand
 *    b. 重量校验（领料量 vs 子卷+纸边，损耗≤3%）
 *    c. 自动建母卷 roll_detail(parent_roll_id=NULL, CONSUMED)
 *    d. 循环建子卷 roll_detail(parent_roll_id=母卷, IN_STOCK)
 *    e. 库存事务：ITEM_RECPT 入子卷 + MISC_RECPT 入纸边
 *    f. 报工(AUDITED) + 推进任务/工单/流转卡进度
 *    g. 物料追溯：母卷 ROLL ->[SLIT]-> 子卷 ROLL
 *    h. 创建分切记录
 * </pre>
 *
 * <p>母卷/子卷 roll_detail 在本接口内创建，不依赖预存数据。
 *
 * @author qixiaoxia
 * @date 2026-07-29
 */
@Service
public class ProSlittingServiceImpl implements IProSlittingService {

    private static final Logger log = LoggerFactory.getLogger(ProSlittingServiceImpl.class);

    /** 重量校验容差：损耗率 > 3% 拒绝执行 */
    private static final BigDecimal MAX_LOSS_RATE = new BigDecimal("3.0");
    /** 纸边量级合理性上限：纸边占领料量比例 > 20% 视为录入异常（纸边为边角料，不应接近主料量） */
    private static final BigDecimal EDGE_MAX_RATIO = new BigDecimal("20.0");

    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ProSlittingRecordMapper slittingMapper;
    @Autowired private WmRollDetailMapper rollDetailMapper;
    @Autowired private WmMaterialStockMapper materialStockMapper;
    @Autowired private ProFeedbackMapper feedbackMapper;
    @Autowired private ProTaskMapper taskMapper;
    @Autowired private ProWorkorderMapper workorderMapper;
    @Autowired private ProRouteProcessMapper routeProcessMapper;
    @Autowired private ProCardMapper cardMapper;
    @Autowired private ProMaterialTraceMapper traceMapper;
    @Autowired private AutoCodeGenerator autoCodeGenerator;
    @Autowired private IWmTransactionService transactionService;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    // ════════════════════════════════════════════════════════════════
    // 核心入口：执行分切（库存驱动，单接口完成领料+建卷+报工）
    // ════════════════════════════════════════════════════════════════

    @Override
    public ProSlittingRecord executeSlitting(SlittingRequest request) {
        validateRequest(request);
        // 锁按物料+仓库维度，防同一批次库存并发扣减
        String lockKey = "wm:slitting:" + request.getSourceItemId() + ":" + request.getSourceWarehouseId();
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> txTemplate.execute(status -> doSlitting(request)));
    }

    private void validateRequest(SlittingRequest req) {
        if (req.getSourceItemId() == null) throw new ServiceException("领料母卷物料不能为空");
        if (req.getSourceWarehouseId() == null) throw new ServiceException("领料出库仓库不能为空");
        if (req.getPickQty() == null || req.getPickQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("领料数量必须大于0");
        }
        if (req.getChildRolls() == null || req.getChildRolls().isEmpty()) {
            throw new ServiceException("至少录入一条子卷规格");
        }
        for (int i = 0; i < req.getChildRolls().size(); i++) {
            ChildRollSpec c = req.getChildRolls().get(i);
            if (c.getActualWeight() == null || c.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("第" + (i + 1) + "条子卷重量必须大于0");
            }
        }
        validateEdge(req);
    }

    /** 纸边校验：① 纸边物料不能等于领料母卷物料（否则纸边又入回同物料，库存失真）
     *  ② 纸边为边角料，量级合理性——不应超过领料量的 20% */
    private void validateEdge(SlittingRequest req) {
        if (req.getEdgeItemId() == null) return;  // 未填纸边，跳过
        if (req.getEdgeItemId().equals(req.getSourceItemId())) {
            throw new ServiceException("纸边物料不能与领料母卷物料相同，请选择边角料/废料物料");
        }
        if (req.getEdgeWeight() != null && req.getEdgeWeight().compareTo(BigDecimal.ZERO) > 0
                && req.getPickQty() != null && req.getPickQty().compareTo(BigDecimal.ZERO) > 0) {
            // 纸边单位 kg，领料单位 吨，统一换算到吨比较
            BigDecimal edgeTon = req.getEdgeWeight().divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
            BigDecimal ratio = edgeTon.divide(req.getPickQty(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (ratio.compareTo(EDGE_MAX_RATIO) > 0) {
                throw new ServiceException("纸边重量(" + req.getEdgeWeight() + "kg)占领料量" + ratio.setScale(2, RoundingMode.HALF_UP)
                        + "%过高，请检查单位（纸边单位为kg，领料为吨）");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 事务体：拆分步骤（每步独立方法，保证可读性）
    // ════════════════════════════════════════════════════════════════

    private ProSlittingRecord doSlitting(SlittingRequest req) {
        String operator = SecurityUtils.getUsername();
        BigDecimal pickQty = req.getPickQty();

        // a. 重量校验（领料前先校验，避免扣了库存再回滚）
        BigDecimal childTotal = sumChildWeights(req.getChildRolls());
        BigDecimal edgeWeightKg = req.getEdgeWeight() != null ? req.getEdgeWeight() : BigDecimal.ZERO;
        BigDecimal edgeWeightTon = edgeWeightKg.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        BigDecimal loss = pickQty.subtract(childTotal).subtract(edgeWeightTon);
        if (loss.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("子卷+纸边总重量(" + childTotal.add(edgeWeightTon)
                    + "吨)超过领料量(" + pickQty + "吨)，请检查录入数据");
        }
        BigDecimal lossRate = pickQty.compareTo(BigDecimal.ZERO) > 0
                ? loss.divide(pickQty, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        if (lossRate.compareTo(MAX_LOSS_RATE) > 0) {
            throw new ServiceException("损耗率" + lossRate + "%超过" + MAX_LOSS_RATE + "%上限，请检查录入数据");
        }

        // b. 生成分切批次号
        String slitBatchNo = autoCodeGenerator.genSerialCode("SLITTING_CODE", null);

        // c. 报工先行（AUDITED）—— 其 recordId 作为后续库存事务的 sourceDocId（幂等键）
        WmRollDetail stubParent = buildStubParentForFeedback(req, pickQty);
        ProFeedback fb = createAndPersistFeedback(req, stubParent, req.getChildRolls(), operator);
        Long sourceDocId = fb.getRecordId();

        // d. 领料出库：SPLIT 事务扣减 material_stock(母卷物料)
        WmTransaction pickTx = buildPickTransaction(req, pickQty.negate(), slitBatchNo, sourceDocId);
        transactionService.processTransaction(pickTx);

        // e. 自动建母卷 roll_detail（领料即消耗，status=CONSUMED）
        WmRollDetail parentRoll = buildParentRoll(req, pickQty, slitBatchNo, operator);
        rollDetailMapper.insertWmRollDetail(parentRoll);

        // f. 创建子卷
        List<WmRollDetail> childRolls = createChildRolls(req, parentRoll, slitBatchNo, operator);

        // g. 子卷入库 + 纸边入库
        processInventoryTransactions(req, parentRoll, childRolls, childTotal, edgeWeightKg, slitBatchNo, sourceDocId);

        // h. 物料追溯
        writeSlitTraces(parentRoll, childRolls, sourceDocId, req.getWorkorderId());

        // i. 创建分切记录
        ProSlittingRecord record = buildSlittingRecord(req, parentRoll, childTotal, edgeWeightTon,
                loss, lossRate, slitBatchNo, sourceDocId, operator, pickQty);
        slittingMapper.insertProSlittingRecord(record);
        record.setChildRolls(childRolls);

        log.info("分切成功: 批次号={}, 领料{}吨, 母卷={}, 子卷={}卷, 操作人={}",
                slitBatchNo, pickQty, parentRoll.getRollCode(), childRolls.size(), operator);
        return record;
    }

    /** 构建仅用于报工的母卷桩（report 时物料来自领料 source），不落库 roll_detail。
     *  仅取 itemId/code/name/actualWeight 供 buildFeedback 使用，其余字段由 buildFeedback 自行设置。 */
    private WmRollDetail buildStubParentForFeedback(SlittingRequest req, BigDecimal pickQty) {
        WmRollDetail stub = new WmRollDetail();
        stub.setItemId(req.getSourceItemId());
        stub.setItemCode(req.getSourceItemCode());
        stub.setItemName(req.getSourceItemName());
        stub.setActualWeight(pickQty);
        return stub;
    }

    // ════════════════════════════════════════════════════════════════
    // 母卷创建（报工自动建卷，领料即消耗）
    // ════════════════════════════════════════════════════════════════

    private WmRollDetail buildParentRoll(SlittingRequest req, BigDecimal pickQty,
                                          String slitBatchNo, String operator) {
        WmRollDetail roll = new WmRollDetail();
        roll.setRollCode(autoCodeGenerator.genSerialCode("ROLL_CODE", null));
        roll.setItemId(req.getSourceItemId());
        roll.setItemCode(req.getSourceItemCode());
        roll.setItemName(req.getSourceItemName());
        roll.setActualWeight(pickQty);
        roll.setOriginalQuantity(pickQty);
        roll.setRemainingQuantity(BigDecimal.ZERO);
        roll.setUnitOfMeasure("TON");
        roll.setWarehouseId(req.getSourceWarehouseId());
        roll.setWarehouseCode(req.getSourceWarehouseCode());
        roll.setWarehouseName(req.getSourceWarehouseName());
        roll.setBatchId(req.getSourceBatchId());
        roll.setBatchCode(req.getSourceBatchCode());
        roll.setStatus("CONSUMED");  // 领料即消耗
        roll.setSlitBatchNo(slitBatchNo);
        roll.setLastWorkorderId(req.getWorkorderId());
        roll.setLastWorkorderCode(req.getWorkorderCode());
        roll.setCreateTime(DateUtils.getNowDate());
        roll.setCreateBy(operator);
        return roll;
    }

    // ════════════════════════════════════════════════════════════════
    // 库存事务
    // ════════════════════════════════════════════════════════════════

    private WmTransaction buildPickTransaction(SlittingRequest req, BigDecimal negQty, String slitBatchNo, Long sourceDocId) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.SPLIT.getCode());  // 分切领料出库
        tx.setSourceDocType("FEEDBACK");
        tx.setSourceDocId(sourceDocId);
        tx.setSourceDocCode(slitBatchNo);
        tx.setSourceLineId(0L);
        tx.setItemId(req.getSourceItemId());
        tx.setItemCode(req.getSourceItemCode());
        tx.setItemName(req.getSourceItemName());
        tx.setUnitOfMeasure("TON");
        tx.setUnitName("吨");
        tx.setQuantity(negQty);
        tx.setBatchId(req.getSourceBatchId());
        tx.setBatchCode(req.getSourceBatchCode());
        tx.setWarehouseId(req.getSourceWarehouseId());
        tx.setWarehouseCode(req.getSourceWarehouseCode());
        tx.setWarehouseName(req.getSourceWarehouseName());
        tx.setTransactionTime(new Date());
        return tx;
    }

    private void processInventoryTransactions(SlittingRequest req, WmRollDetail parentRoll,
                                               List<WmRollDetail> childRolls, BigDecimal childTotalWeight,
                                               BigDecimal edgeWeightKg, String slitBatchNo, Long sourceDocId) {
        // 子卷入库（汇总到子卷物料库存，用首子卷物料做汇总）
        WmRollDetail firstChild = childRolls.get(0);
        WmTransaction recptTx = buildChildRecptTransaction(firstChild, childTotalWeight, slitBatchNo, sourceDocId);
        transactionService.processTransaction(recptTx);

        // 纸边入库
        if (req.getEdgeItemId() != null && edgeWeightKg != null && edgeWeightKg.compareTo(BigDecimal.ZERO) > 0) {
            WmTransaction edgeTx = buildEdgeTransaction(req, edgeWeightKg, parentRoll, slitBatchNo, sourceDocId);
            transactionService.processTransaction(edgeTx);
        }
    }

    private WmTransaction buildChildRecptTransaction(WmRollDetail child, BigDecimal totalWeight, String slitBatchNo, Long sourceDocId) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.ITEM_RECPT.getCode());
        tx.setSourceDocType("FEEDBACK");
        tx.setSourceDocId(sourceDocId);
        tx.setSourceDocCode(slitBatchNo);
        tx.setSourceLineId(1L);
        tx.setItemId(child.getItemId());
        tx.setItemCode(child.getItemCode());
        tx.setItemName(child.getItemName());
        tx.setUnitOfMeasure(child.getUnitOfMeasure());
        tx.setUnitName("吨");
        tx.setQuantity(totalWeight);
        tx.setBatchId(child.getBatchId());
        tx.setBatchCode(child.getBatchCode());
        tx.setWarehouseId(child.getWarehouseId());
        tx.setWarehouseCode(child.getWarehouseCode());
        tx.setWarehouseName(child.getWarehouseName());
        tx.setTransactionTime(new Date());
        return tx;
    }

    private WmTransaction buildEdgeTransaction(SlittingRequest req, BigDecimal edgeWeightKg,
                                                WmRollDetail parent, String slitBatchNo, Long sourceDocId) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.MISC_RECPT.getCode());
        tx.setSourceDocType("FEEDBACK");
        tx.setSourceDocId(sourceDocId);
        tx.setSourceDocCode(slitBatchNo);
        tx.setSourceLineId(2L);
        tx.setItemId(req.getEdgeItemId());
        tx.setItemCode(req.getEdgeItemCode());
        tx.setItemName(req.getEdgeItemName());
        tx.setUnitOfMeasure("KG");
        tx.setUnitName("千克");
        tx.setQuantity(edgeWeightKg);
        tx.setWarehouseId(parent.getWarehouseId());
        tx.setWarehouseCode(parent.getWarehouseCode());
        tx.setWarehouseName(parent.getWarehouseName());
        tx.setTransactionTime(new Date());
        return tx;
    }

    // ════════════════════════════════════════════════════════════════
    // 子卷创建
    // ════════════════════════════════════════════════════════════════

    private List<WmRollDetail> createChildRolls(SlittingRequest req, WmRollDetail parent,
                                                 String slitBatchNo, String operator) {
        List<WmRollDetail> childRolls = new ArrayList<>();
        for (ChildRollSpec spec : req.getChildRolls()) {
            WmRollDetail child = buildChildRoll(spec, parent, slitBatchNo, operator);
            rollDetailMapper.insertWmRollDetail(child);
            childRolls.add(child);
        }
        return childRolls;
    }

    private WmRollDetail buildChildRoll(ChildRollSpec spec, WmRollDetail parent,
                                        String slitBatchNo, String operator) {
        WmRollDetail child = new WmRollDetail();
        child.setRollCode(autoCodeGenerator.genSerialCode("ROLL_CODE", null));
        child.setItemId(spec.getItemId() != null ? spec.getItemId() : parent.getItemId());
        child.setItemCode(spec.getItemCode() != null ? spec.getItemCode() : parent.getItemCode());
        child.setItemName(spec.getItemName() != null ? spec.getItemName() : parent.getItemName());
        child.setBatchId(parent.getBatchId());
        child.setBatchCode(parent.getBatchCode());
        child.setRecptId(parent.getRecptId());
        child.setRecptDetailId(parent.getRecptDetailId());
        child.setVendorId(parent.getVendorId());
        child.setVendorCode(parent.getVendorCode());
        child.setVendorName(parent.getVendorName());
        child.setVendorRollNo(parent.getVendorRollNo());
        child.setParentRollId(parent.getRollId());
        child.setActualWidth(spec.getActualWidth());
        child.setActualWeightGsm(spec.getActualWeightGsm() != null ? spec.getActualWeightGsm() : parent.getActualWeightGsm());
        child.setActualLength(spec.getActualLength());
        child.setActualWeight(spec.getActualWeight());
        child.setUnitOfMeasure(parent.getUnitOfMeasure());
        child.setOriginalQuantity(spec.getActualWeight());
        child.setRemainingQuantity(spec.getActualWeight());
        child.setWarehouseId(parent.getWarehouseId());
        child.setWarehouseCode(parent.getWarehouseCode());
        child.setWarehouseName(parent.getWarehouseName());
        child.setLocationId(parent.getLocationId());
        child.setAreaId(parent.getAreaId());
        child.setStatus("IN_STOCK");
        child.setSlitBatchNo(slitBatchNo);
        child.setCreateTime(DateUtils.getNowDate());
        child.setCreateBy(operator);
        return child;
    }

    // ════════════════════════════════════════════════════════════════
    // 物料追溯
    // ════════════════════════════════════════════════════════════════

    private void writeSlitTraces(WmRollDetail parent, List<WmRollDetail> childRolls,
                                 Long feedbackId, Long workorderId) {
        String operator = SecurityUtils.getUsername();
        for (WmRollDetail child : childRolls) {
            try {
                ProMaterialTrace trace = new ProMaterialTrace();
                trace.setTraceType("SLIT");
                trace.setParentType("ROLL");
                trace.setParentId(parent.getRollId());
                trace.setChildType("ROLL");
                trace.setChildId(child.getRollId());
                trace.setQuantity(child.getActualWeight());
                trace.setUnitOfMeasure("TON");
                trace.setUnitName("吨");
                trace.setWorkorderId(workorderId);
                trace.setFeedbackId(feedbackId);
                trace.setTraceTime(new Date());
                trace.setCreateTime(DateUtils.getNowDate());
                trace.setCreateBy(operator);
                traceMapper.insertProMaterialTrace(trace);
            } catch (Exception e) {
                log.error("分切追溯写入失败, parentRoll={}, childRoll={}", parent.getRollId(), child.getRollId(), e);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 报工创建 + 进度推进
    // ════════════════════════════════════════════════════════════════

    private ProFeedback createAndPersistFeedback(SlittingRequest req, WmRollDetail parentRoll,
                                                  List<ChildRollSpec> childSpecs, String operator) {
        ProFeedback fb = buildFeedback(req, parentRoll, childSpecs, operator);
        feedbackMapper.insertProFeedback(fb);
        // 分切不绑定生产任务（无 taskId），不推进任务进度。
        // 仅当显式传了真实工单（非 null）才推进工单产出 + 流转卡。
        if (req.getWorkorderId() != null && isLastProcess(req)) {
            workorderMapper.addQuantityProduced(req.getWorkorderId(), nvl(fb.getQuantityFeedback()));
        }
        if (req.getCardId() != null) {
            advanceCard(fb, isLastProcess(req));
        }
        return fb;
    }

    private ProFeedback buildFeedback(SlittingRequest req, WmRollDetail parentRoll,
                                       List<ChildRollSpec> childSpecs, String operator) {
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackType("INTERNAL");
        fb.setFeedbackCode(autoCodeGenerator.genSerialCode("FEEDBACK_CODE", null));
        fb.setWorkorderId(req.getWorkorderId() != null ? req.getWorkorderId() : 0L);
        fb.setWorkorderCode(req.getWorkorderCode());
        fb.setProcessId(req.getProcessId() != null ? req.getProcessId() : 0L);
        fb.setProcessCode(req.getProcessCode());
        fb.setProcessName(req.getProcessName());
        fb.setCardId(req.getCardId());
        fb.setRouteId(req.getRouteId() != null ? req.getRouteId() : 0L);
        fb.setWorkstationId(req.getWorkstationId() != null ? req.getWorkstationId() : 0L);
        fb.setItemId(parentRoll.getItemId());
        fb.setItemCode(parentRoll.getItemCode());
        fb.setItemName(parentRoll.getItemName());
        fb.setUnitOfMeasure("ROLL");
        fb.setUnitName("卷");
        BigDecimal childCount = new BigDecimal(childSpecs.size());
        fb.setQuantity(childCount);
        fb.setQuantityFeedback(childCount);
        fb.setQuantityQualified(childCount);
        fb.setQuantityUnqualified(BigDecimal.ZERO);
        fb.setQuantityUncheck(BigDecimal.ZERO);
        fb.setQuantityLaborScrap(BigDecimal.ZERO);
        fb.setQuantityMaterialScrap(BigDecimal.ZERO);
        fb.setQuantityOtherScrap(BigDecimal.ZERO);
        fb.setUserName(operator);
        fb.setFeedbackChannel("PC");
        fb.setFeedbackTime(DateUtils.getNowDate());
        fb.setStatus("AUDITED");
        fb.setCreateTime(DateUtils.getNowDate());
        fb.setCreateBy(operator);
        return fb;
    }

    private boolean isLastProcess(SlittingRequest req) {
        if (req.getRouteId() == null || req.getProcessId() == null) return false;
        ProRouteProcess last = routeProcessMapper.selectLastProcessByRouteId(req.getRouteId());
        return last != null && last.getProcessId().equals(req.getProcessId());
    }

    private void tryAutoCompleteTask(Long taskId) {
        ProTask task = taskMapper.selectProTaskByTaskId(taskId);
        if (task == null || !ProConstants.TASK_STATUS_PRODUCING.equals(task.getStatus())) return;
        BigDecimal produced = nvl(task.getQuantityProduced());
        BigDecimal planned = nvl(task.getQuantity());
        if (planned.compareTo(BigDecimal.ZERO) > 0 && produced.compareTo(planned) >= 0) {
            task.setStatus(ProConstants.TASK_STATUS_COMPLETED);
            task.setUpdateTime(DateUtils.getNowDate());
            task.setUpdateBy(SecurityUtils.getUsername());
            taskMapper.updateProTask(task);
        }
    }

    private void advanceCard(ProFeedback fb, boolean isLast) {
        try {
            com.ruoyi.system.domain.mes.pro.ProCard cardUpd = new com.ruoyi.system.domain.mes.pro.ProCard();
            cardUpd.setCardId(fb.getCardId());
            cardUpd.setCurrentProcessId(fb.getProcessId());
            cardUpd.setCurrentProcessName(fb.getProcessName());
            if (isLast) {
                com.ruoyi.system.domain.mes.pro.ProCard card = cardMapper.selectProCardByCardId(fb.getCardId());
                if (card != null) {
                    BigDecimal produced = nvl(feedbackMapper.sumAuditedQualifiedByCardAndProcess(
                            fb.getCardId(), fb.getProcessId()));
                    BigDecimal planned = nvl(card.getQuantityTransfered());
                    if (produced.compareTo(planned) >= 0) cardUpd.setStatus("COMPLETED");
                }
            }
            cardUpd.setUpdateBy(SecurityUtils.getUsername());
            cardUpd.setUpdateTime(DateUtils.getNowDate());
            cardMapper.updateProCard(cardUpd);
        } catch (Exception e) {
            log.error("流转卡状态推进失败, cardId={}", fb.getCardId(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 分切记录构建
    // ════════════════════════════════════════════════════════════════

    private ProSlittingRecord buildSlittingRecord(SlittingRequest req, WmRollDetail parentRoll,
                                                   BigDecimal childTotal, BigDecimal edgeWeightTon,
                                                   BigDecimal loss, BigDecimal lossRate,
                                                   String slitBatchNo, Long feedbackId, String operator,
                                                   BigDecimal pickQty) {
        ProSlittingRecord record = new ProSlittingRecord();
        record.setSlitBatchNo(slitBatchNo);
        record.setFeedbackId(feedbackId);
        record.setWorkorderId(req.getWorkorderId());
        record.setWorkorderCode(req.getWorkorderCode());
        record.setProcessId(req.getProcessId());
        record.setProcessCode(req.getProcessCode());
        record.setProcessName(req.getProcessName());
        record.setCardId(req.getCardId());
        // 领料来源
        record.setSourceItemId(req.getSourceItemId());
        record.setSourceItemCode(req.getSourceItemCode());
        record.setSourceItemName(req.getSourceItemName());
        record.setSourceWarehouseId(req.getSourceWarehouseId());
        record.setSourceWarehouseCode(req.getSourceWarehouseCode());
        record.setSourceWarehouseName(req.getSourceWarehouseName());
        record.setPickQty(pickQty);
        record.setPickTime(DateUtils.getNowDate());
        record.setPickBy(operator);
        // 母卷（自动建的）
        record.setParentRollId(parentRoll.getRollId());
        record.setParentRollCode(parentRoll.getRollCode());
        record.setParentItemId(parentRoll.getItemId());
        record.setParentItemCode(parentRoll.getItemCode());
        record.setParentItemName(parentRoll.getItemName());
        record.setParentWeight(pickQty);
        // 子卷汇总
        record.setChildCount(req.getChildRolls().size());
        record.setChildTotalWeight(childTotal);
        // 纸边
        record.setEdgeItemId(req.getEdgeItemId());
        record.setEdgeItemCode(req.getEdgeItemCode());
        record.setEdgeItemName(req.getEdgeItemName());
        record.setEdgeWeight(req.getEdgeWeight());
        record.setLossWeight(loss);
        record.setLossRate(lossRate);
        record.setOperator(operator);
        record.setWorkstationId(req.getWorkstationId());
        record.setSlitTime(DateUtils.getNowDate());
        record.setStatus("EXECUTED");
        record.setRemark(req.getRemark());
        record.setCreateTime(DateUtils.getNowDate());
        record.setCreateBy(operator);
        return record;
    }

    // ════════════════════════════════════════════════════════════════
    // 查询方法
    // ════════════════════════════════════════════════════════════════

    @Override
    public List<WmMaterialStock> listAvailableStock(Long itemId) {
        // 查该物料在库库存（FIFO 排序，只读不加行锁）
        return materialStockMapper.selectAvailableBatches(itemId, null, "NORMAL");
    }

    @Override
    public List<ProSlittingRecord> selectList(ProSlittingRecord query) {
        return slittingMapper.selectProSlittingRecordList(query);
    }

    @Override
    public ProSlittingRecord selectBySlitId(Long slitId) {
        ProSlittingRecord record = slittingMapper.selectProSlittingRecordBySlitId(slitId);
        if (record != null) {
            WmRollDetail childQuery = new WmRollDetail();
            childQuery.setSlitBatchNo(record.getSlitBatchNo());
            record.setChildRolls(rollDetailMapper.selectWmRollDetailList(childQuery));
        }
        return record;
    }

    // ════════════════════════════════════════════════════════════════
    // 工具方法
    // ════════════════════════════════════════════════════════════════

    private BigDecimal sumChildWeights(List<ChildRollSpec> childRolls) {
        BigDecimal total = BigDecimal.ZERO;
        for (ChildRollSpec c : childRolls) {
            total = total.add(nvl(c.getActualWeight()));
        }
        return total;
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
