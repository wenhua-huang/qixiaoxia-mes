package com.ruoyi.system.service.mes.pro.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.TransactionTypeEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.core.domain.model.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.system.domain.mes.md.MdVendor;
import com.ruoyi.system.domain.mes.pro.OutsourceReceiveRequest;
import com.ruoyi.system.domain.mes.pro.OutsourceResultRequest;
import com.ruoyi.system.domain.mes.pro.ProCard;
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
import com.ruoyi.system.mapper.mes.md.MdVendorMapper;
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
import com.ruoyi.system.service.mes.pro.IProWorkorderDocService;
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
 * 分切作业 Service（厂内 INTERNAL + 外协 OUTSOURCE 统一模型）
 *
 * <p>厂内（一步完成）：领料出库(SPLIT扣库存) → 自动建母卷(CONSUMED)/子卷(IN_STOCK)
 * → 子卷/纸边入库(ITEM_RECPT/MISC_RECPT) → 报工(AUDITED) → SLIT追溯 → 记录(EXECUTED)。
 *
 * <p>外协（三步）：
 * <ol>
 *   <li>executeSlitting(OUTSOURCE)：按卷号发料，母卷 OUTSOURCED + SPLIT扣库存，记录 ISSUED</li>
 *   <li>recordOutsourceResult：厂商录子卷，子卷 OUTSOURCED + SLIT追溯，记录 SLITTING</li>
 *   <li>receiveOutsource：我方收货，子卷 IN_STOCK + ITEM_RECPT入库，母卷 CONSUMED + 报工，记录 RECEIVED</li>
 * </ol>
 *
 * <p>所有状态变更走 Redis 锁 + TransactionTemplate（先锁后事务），禁止 @Transactional。
 *
 * @author qixiaoxia
 * @date 2026-07-29
 */
@Service
public class ProSlittingServiceImpl implements IProSlittingService {

    private static final Logger log = LoggerFactory.getLogger(ProSlittingServiceImpl.class);

    /** 重量校验容差：损耗率 > 3% 拒绝执行 */
    private static final BigDecimal MAX_LOSS_RATE = new BigDecimal("3.0");
    /** 纸边量级合理性上限：纸边占领料量比例 > 20% 视为录入异常 */
    private static final BigDecimal EDGE_MAX_RATIO = new BigDecimal("20.0");

    private static final String MODE_INTERNAL = "INTERNAL";
    private static final String MODE_OUTSOURCE = "OUTSOURCE";

    // 流转卡状态（qxx_pro_card.status）
    private static final String STATUS_CARD_OUTSOURCING = "OUTSOURCING";
    private static final String STATUS_CARD_ACTIVE = "ACTIVE";
    private static final String STATUS_CARD_COMPLETED = "COMPLETED";

    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ProSlittingRecordMapper slittingMapper;
    @Autowired private WmRollDetailMapper rollDetailMapper;
    @Autowired private WmMaterialStockMapper materialStockMapper;
    @Autowired private ProFeedbackMapper feedbackMapper;
    @Autowired private ProWorkorderMapper workorderMapper;
    @Autowired private ProRouteProcessMapper routeProcessMapper;
    @Autowired private ProCardMapper cardMapper;
    @Autowired private ProTaskMapper proTaskMapper;
    @Autowired private ProMaterialTraceMapper traceMapper;
    @Autowired private MdVendorMapper vendorMapper;
    @Autowired private AutoCodeGenerator autoCodeGenerator;
    @Autowired private IWmTransactionService transactionService;
    @Autowired private IProWorkorderDocService workorderDocService;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    // ════════════════════════════════════════════════════════════════
    // 入口：按 slitMode 分流
    // ════════════════════════════════════════════════════════════════

    @Override
    public ProSlittingRecord executeSlitting(SlittingRequest request) {
        String mode = request.getSlitMode() == null ? MODE_INTERNAL : request.getSlitMode();
        if (MODE_OUTSOURCE.equals(mode)) {
            return startOutsource(request);
        }
        return executeInternal(request);
    }

    // ════════════════════════════════════════════════════════════════
    // 厂内分切（原有逻辑，一步完成）
    // ════════════════════════════════════════════════════════════════

    private ProSlittingRecord executeInternal(SlittingRequest request) {
        validateRequest(request);
        String lockKey = "wm:slitting:" + request.getSourceItemId() + ":" + request.getSourceWarehouseId();
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> txTemplate.execute(status -> doInternalSlitting(request)));
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

    private void validateEdge(SlittingRequest req) {
        if (req.getEdgeItemId() == null) return;
        if (req.getEdgeItemId().equals(req.getSourceItemId())) {
            throw new ServiceException("纸边物料不能与领料母卷物料相同，请选择边角料/废料物料");
        }
        if (req.getEdgeWeight() != null && req.getEdgeWeight().compareTo(BigDecimal.ZERO) > 0
                && req.getPickQty() != null && req.getPickQty().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal edgeTon = req.getEdgeWeight().divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
            BigDecimal ratio = edgeTon.divide(req.getPickQty(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            if (ratio.compareTo(EDGE_MAX_RATIO) > 0) {
                throw new ServiceException("纸边重量(" + req.getEdgeWeight() + "kg)占领料量" + ratio.setScale(2, RoundingMode.HALF_UP)
                        + "%过高，请检查单位（纸边单位为kg，领料为吨）");
            }
        }
    }

    private ProSlittingRecord doInternalSlitting(SlittingRequest req) {
        String operator = SecurityUtils.getUsername();
        BigDecimal pickQty = req.getPickQty();

        BigDecimal childTotal = sumChildWeights(req.getChildRolls());
        BigDecimal edgeWeightKg = req.getEdgeWeight() != null ? req.getEdgeWeight() : BigDecimal.ZERO;
        BigDecimal edgeWeightTon = edgeWeightKg.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        validateWeightBalance(pickQty, childTotal, edgeWeightTon);

        String slitBatchNo = genCodeWithFallback("SLITTING_CODE", "SL");
        WmRollDetail stubParent = buildStubParentForFeedback(req, pickQty);
        ProFeedback fb = createAndPersistFeedback(req, stubParent, req.getChildRolls(), operator, "INTERNAL");
        Long sourceDocId = fb.getRecordId();

        WmTransaction pickTx = buildPickTransaction(req, pickQty.negate(), slitBatchNo, sourceDocId);
        transactionService.processTransaction(pickTx);

        WmRollDetail parentRoll = buildParentRoll(req, pickQty, slitBatchNo, operator);
        rollDetailMapper.insertWmRollDetail(parentRoll);

        List<WmRollDetail> childRolls = createChildRolls(req, parentRoll, slitBatchNo, operator);
        processInventoryTransactions(req, parentRoll, childRolls, childTotal, edgeWeightKg, slitBatchNo, sourceDocId);
        writeSlitTraces(parentRoll, childRolls, sourceDocId, req.getWorkorderId());

        BigDecimal loss = pickQty.subtract(childTotal).subtract(edgeWeightTon);
        BigDecimal lossRate = lossRate(loss, pickQty);
        ProSlittingRecord record = buildSlittingRecord(req, parentRoll, childTotal, edgeWeightTon,
                loss, lossRate, slitBatchNo, sourceDocId, operator, pickQty, "EXECUTED");
        slittingMapper.insertProSlittingRecord(record);
        record.setChildRolls(childRolls);

        log.info("厂内分切成功: 批次号={}, 领料{}吨, 母卷={}, 子卷={}卷, 操作人={}",
                slitBatchNo, pickQty, parentRoll.getRollCode(), childRolls.size(), operator);
        return record;
    }

    private void validateWeightBalance(BigDecimal pickQty, BigDecimal childTotal, BigDecimal edgeWeightTon) {
        BigDecimal loss = pickQty.subtract(childTotal).subtract(edgeWeightTon);
        if (loss.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("子卷+纸边总重量(" + childTotal.add(edgeWeightTon)
                    + "吨)超过领料量(" + pickQty + "吨)，请检查录入数据");
        }
        if (lossRate(loss, pickQty).compareTo(MAX_LOSS_RATE) > 0) {
            throw new ServiceException("损耗率" + lossRate(loss, pickQty) + "%超过" + MAX_LOSS_RATE + "%上限，请检查录入数据");
        }
    }

    private BigDecimal lossRate(BigDecimal loss, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return loss.divide(base, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    private WmRollDetail buildStubParentForFeedback(SlittingRequest req, BigDecimal pickQty) {
        WmRollDetail stub = new WmRollDetail();
        stub.setItemId(req.getSourceItemId());
        stub.setItemCode(req.getSourceItemCode());
        stub.setItemName(req.getSourceItemName());
        stub.setActualWeight(pickQty);
        return stub;
    }

    private WmRollDetail buildParentRoll(SlittingRequest req, BigDecimal pickQty,
                                          String slitBatchNo, String operator) {
        WmRollDetail roll = new WmRollDetail();
        roll.setRollCode(genCodeWithFallback("ROLL_CODE", "RL"));
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
        roll.setStatus("CONSUMED");
        roll.setSlitBatchNo(slitBatchNo);
        roll.setLastWorkorderId(req.getWorkorderId());
        roll.setLastWorkorderCode(req.getWorkorderCode());
        roll.setCreateTime(DateUtils.getNowDate());
        roll.setCreateBy(operator);
        return roll;
    }

    private WmTransaction buildPickTransaction(SlittingRequest req, BigDecimal negQty, String slitBatchNo, Long sourceDocId) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.SPLIT.getCode());
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
        // 按物料聚合入库（不同子卷可能对应不同成品物料，不能全部挂到 firstChild.itemId）
        Map<Long, List<WmRollDetail>> byItem = new LinkedHashMap<>();
        for (WmRollDetail c : childRolls) {
            byItem.computeIfAbsent(c.getItemId(), k -> new ArrayList<>()).add(c);
        }
        for (List<WmRollDetail> group : byItem.values()) {
            WmRollDetail first = group.get(0);
            BigDecimal groupWeight = group.stream()
                    .map(c -> nvl(c.getActualWeight()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            WmTransaction recptTx = buildChildRecptTransaction(first, groupWeight, slitBatchNo, sourceDocId);
            transactionService.processTransaction(recptTx);
        }

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

    private List<WmRollDetail> createChildRolls(SlittingRequest req, WmRollDetail parent,
                                                 String slitBatchNo, String operator) {
        List<WmRollDetail> childRolls = new ArrayList<>();
        for (ChildRollSpec spec : req.getChildRolls()) {
            WmRollDetail child = buildChildRoll(spec, parent, slitBatchNo, operator, "IN_STOCK");
            rollDetailMapper.insertWmRollDetail(child);
            childRolls.add(child);
        }
        return childRolls;
    }

    private WmRollDetail buildChildRoll(ChildRollSpec spec, WmRollDetail parent,
                                        String slitBatchNo, String operator, String status) {
        WmRollDetail child = new WmRollDetail();
        child.setRollCode(genCodeWithFallback("ROLL_CODE", "RL"));
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
        child.setStatus(status);
        child.setSlitBatchNo(slitBatchNo);
        child.setCreateTime(DateUtils.getNowDate());
        child.setCreateBy(operator);
        return child;
    }

    private void writeSlitTraces(WmRollDetail parent, List<WmRollDetail> childRolls,
                                 Long feedbackId, Long workorderId) {
        String operator = SecurityUtils.getUsername();
        for (WmRollDetail child : childRolls) {
            // 追溯写入失败必须向上抛出回滚收货事务，禁止静默丢边（否则母卷已收货但追溯断链）
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
        }
    }

    /**
     * 库存级分切转换追溯：MATERIAL_STOCK(母卷) ─SLIT→ MATERIAL_STOCK(子卷)。
     * 与 ROLL→ROLL 的 writeSlitTraces 互补，让从库存维度追溯能直接看到「这卷子料是哪卷母卷切出来的」。
     * 母卷 materialStockId 在发料时记录于 WmRollDetail；子卷 materialStockId 在收货入库后回填。
     */
    private void writeStockSlitTraces(WmRollDetail parent, List<WmTransaction> recptTxList,
                                      Long feedbackId, Long workorderId) {
        if (parent.getMaterialStockId() == null || recptTxList == null || recptTxList.isEmpty()) return;
        String operator = SecurityUtils.getUsername();
        for (WmTransaction tx : recptTxList) {
            if (tx == null || tx.getMaterialStockId() == null) continue;
            if (parent.getMaterialStockId().equals(tx.getMaterialStockId())) continue;
            // 追溯写入失败必须向上抛出回滚收货事务，禁止静默丢边
            ProMaterialTrace trace = new ProMaterialTrace();
            trace.setTraceType("SLIT");
            trace.setParentType("MATERIAL_STOCK");
            trace.setParentId(parent.getMaterialStockId());
            trace.setChildType("MATERIAL_STOCK");
            trace.setChildId(tx.getMaterialStockId());
            trace.setQuantity(tx.getQuantity());
            trace.setUnitOfMeasure(tx.getUnitOfMeasure() != null ? tx.getUnitOfMeasure() : "TON");
            trace.setUnitName(tx.getUnitName() != null ? tx.getUnitName() : "吨");
            trace.setWorkorderId(workorderId);
            trace.setFeedbackId(feedbackId);
            trace.setTransactionId(tx.getTransactionId());
            trace.setTraceTime(new Date());
            trace.setCreateTime(DateUtils.getNowDate());
            trace.setCreateBy(operator);
            traceMapper.insertProMaterialTrace(trace);
        }
    }

    private ProFeedback createAndPersistFeedback(SlittingRequest req, WmRollDetail parentRoll,
                                                  List<ChildRollSpec> childSpecs, String operator,
                                                  String feedbackType) {
        ProFeedback fb = buildFeedback(req, parentRoll, childSpecs, operator, feedbackType);
        feedbackMapper.insertProFeedback(fb);
        if (req.getWorkorderId() != null && isLastProcess(req)) {
            workorderMapper.addQuantityProduced(req.getWorkorderId(), nvl(fb.getQuantityFeedback()));
            // 末道工序报工后，若累计合格量已达计划量则自动结案（与 ProFeedbackServiceImpl.auditFeedback 一致）
            workorderDocService.autoCompleteWorkorderIfQualified(req.getWorkorderId());
        }
        if (req.getCardId() != null) {
            advanceCard(fb, isLastProcess(req), "OUTSOURCE".equals(feedbackType));
        }
        return fb;
    }

    private ProFeedback buildFeedback(SlittingRequest req, WmRollDetail parentRoll,
                                       List<ChildRollSpec> childSpecs, String operator, String feedbackType) {
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackType(feedbackType);
        fb.setFeedbackCode(genCodeWithFallback("FEEDBACK_CODE", "FB"));
        fb.setWorkorderId(req.getWorkorderId() != null ? req.getWorkorderId() : 0L);
        fb.setWorkorderCode(req.getWorkorderCode());
        fb.setProcessId(req.getProcessId() != null ? req.getProcessId() : 0L);
        fb.setProcessCode(req.getProcessCode());
        fb.setProcessName(req.getProcessName());
        fb.setCardId(req.getCardId());
        fb.setRouteId(req.getRouteId() != null ? req.getRouteId() : 0L);
        fb.setWorkstationId(req.getWorkstationId() != null ? req.getWorkstationId() : 0L);
        // 外协分切：冗余厂商及外协厂工厂ID，与外协 8 表约定一致（多工厂报工数据隔离）
        if ("OUTSOURCE".equals(feedbackType) && req.getVendorId() != null) {
            fb.setVendorId(req.getVendorId());
            fb.setVendorCode(req.getVendorCode());
            fb.setVendorName(req.getVendorName());
            MdVendor vendor = vendorMapper.selectMdVendorByVendorId(req.getVendorId());
            if (vendor != null) fb.setOutsourceFactoryId(vendor.getOutsourceFactoryId());
        }
        fb.setItemId(parentRoll.getItemId());
        fb.setItemCode(parentRoll.getItemCode());
        fb.setItemName(parentRoll.getItemName());
        // 统一按重量（吨）报工，与外协收货路径 createFeedback 单位一致；
        // 否则末道工序 addQuantityProduced、卡数量判定会出现 ROLL 数 vs TON 数不可比。
        fb.setUnitOfMeasure("TON");
        fb.setUnitName("吨");
        BigDecimal childTotalWeight = sumChildWeights(childSpecs);
        fb.setQuantity(childTotalWeight);
        fb.setQuantityFeedback(childTotalWeight);
        fb.setQuantityQualified(childTotalWeight);
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

    private void advanceCard(ProFeedback fb, boolean isLast, boolean isOutsource) {
        if (fb.getCardId() == null) return;
        String targetStatus = STATUS_CARD_ACTIVE;
        if (isLast) {
            com.ruoyi.system.domain.mes.pro.ProCard card = cardMapper.selectProCardByCardId(fb.getCardId());
            if (card != null) {
                BigDecimal produced = nvl(feedbackMapper.sumAuditedQualifiedByCardAndProcess(
                        fb.getCardId(), fb.getProcessId()));
                BigDecimal planned = nvl(card.getQuantityTransfered());
                if (produced.compareTo(planned) >= 0) targetStatus = STATUS_CARD_COMPLETED;
            }
        }
        // 条件更新：厂内报工仅 ACTIVE→推进，外协收货仅 OUTSOURCING→推进，防并发丢失更新
        String expectedStatus = isOutsource ? STATUS_CARD_OUTSOURCING : STATUS_CARD_ACTIVE;
        int rows = cardMapper.advanceCard(
                fb.getCardId(), fb.getProcessId(), fb.getProcessName(), targetStatus,
                expectedStatus, SecurityUtils.getUsername());
        if (rows == 0)
            log.debug("流转卡推进跳过（已推进或状态不符）: cardId={}, expected={}, target={}",
                    fb.getCardId(), expectedStatus, targetStatus);
    }

    private ProSlittingRecord buildSlittingRecord(SlittingRequest req, WmRollDetail parentRoll,
                                                   BigDecimal childTotal, BigDecimal edgeWeightTon,
                                                   BigDecimal loss, BigDecimal lossRate,
                                                   String slitBatchNo, Long feedbackId, String operator,
                                                   BigDecimal pickQty, String status) {
        ProSlittingRecord record = new ProSlittingRecord();
        record.setSlitMode(req.getSlitMode() != null ? req.getSlitMode() : MODE_INTERNAL);
        record.setVendorId(req.getVendorId());
        record.setVendorCode(req.getVendorCode());
        record.setVendorName(req.getVendorName());
        record.setSlitBatchNo(slitBatchNo);
        record.setFeedbackId(feedbackId);
        record.setWorkorderId(req.getWorkorderId());
        record.setWorkorderCode(req.getWorkorderCode());
        record.setProcessId(req.getProcessId());
        record.setProcessCode(req.getProcessCode());
        record.setProcessName(req.getProcessName());
        record.setCardId(req.getCardId());
        record.setSourceItemId(req.getSourceItemId());
        record.setSourceItemCode(req.getSourceItemCode());
        record.setSourceItemName(req.getSourceItemName());
        record.setSourceWarehouseId(req.getSourceWarehouseId());
        record.setSourceWarehouseCode(req.getSourceWarehouseCode());
        record.setSourceWarehouseName(req.getSourceWarehouseName());
        record.setPickQty(pickQty);
        record.setPickTime(DateUtils.getNowDate());
        record.setPickBy(operator);
        record.setParentRollId(parentRoll.getRollId());
        record.setParentRollCode(parentRoll.getRollCode());
        record.setParentItemId(parentRoll.getItemId());
        record.setParentItemCode(parentRoll.getItemCode());
        record.setParentItemName(parentRoll.getItemName());
        record.setParentWeight(pickQty);
        record.setChildCount(req.getChildRolls() != null ? req.getChildRolls().size() : 0);
        record.setChildTotalWeight(childTotal);
        record.setEdgeItemId(req.getEdgeItemId());
        record.setEdgeItemCode(req.getEdgeItemCode());
        record.setEdgeItemName(req.getEdgeItemName());
        record.setEdgeWeight(req.getEdgeWeight());
        record.setLossWeight(loss);
        record.setLossRate(lossRate);
        record.setOperator(operator);
        record.setWorkstationId(req.getWorkstationId());
        record.setSlitTime(DateUtils.getNowDate());
        record.setStatus(status);
        record.setRemark(req.getRemark());
        record.setCreateTime(DateUtils.getNowDate());
        record.setCreateBy(operator);
        return record;
    }

    // ════════════════════════════════════════════════════════════════
    // 外协 Step 1：建单 + 发料（母卷 OUTSOURCED + SPLIT 扣库存）
    // ════════════════════════════════════════════════════════════════

    private ProSlittingRecord startOutsource(SlittingRequest req) {
        validateOutsourceIssue(req);
        MdVendor vendor = vendorMapper.selectMdVendorByVendorId(req.getVendorId());
        if (vendor == null) throw new ServiceException("外协厂商不存在: " + req.getVendorId());
        req.setVendorCode(vendor.getVendorCode());
        req.setVendorName(vendor.getVendorName());

        // 预加载并校验所有母卷（快速失败，仅为用户体验；锁内仍会重新 SELECT FOR UPDATE 兜底）
        for (Long rollId : req.getParentRollIds()) {
            WmRollDetail roll = rollDetailMapper.selectWmRollDetailByRollId(rollId);
            if (roll == null) throw new ServiceException("母卷不存在: rollId=" + rollId);
            if (!"IN_STOCK".equals(roll.getStatus())) {
                throw new ServiceException("母卷 " + roll.getRollCode() + " 状态非在库(" + roll.getStatus() + ")，不能发料");
            }
            if (roll.getActualWeight() == null || roll.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("母卷 " + roll.getRollCode() + " 重量异常");
            }
        }

        // 多母卷原子发料：所有母卷在一个事务内处理，任一失败整体回滚。
        // 复合锁键按 rollId 升序拼接，保证同一组母卷的并发请求串行化；
        // 不同组但有交集的并发由 issueOneParent 内的 markOutsourcedIfInStock 条件 UPDATE 兜底，
        // 一方失败将随整个事务回滚（不会出现部分母卷已发料）。
        List<Long> sortedIds = new ArrayList<>(req.getParentRollIds());
        java.util.Collections.sort(sortedIds);
        String bulkLockKey = "wm:slitting:outsource:bulk:" + sortedIds;
        ProSlittingRecord first = lockTemplate.executeWithResult(bulkLockKey, 15,
                () -> txTemplate.execute(status -> {
                    ProSlittingRecord rec = null;
                    for (Long parentId : sortedIds) {
                        rec = issueOneParent(req, parentId);
                    }
                    return rec;
                }));
        log.info("外协发料完成: 厂商={}, 母卷数={}, 操作人={}", vendor.getVendorName(),
                sortedIds.size(), SecurityUtils.getUsername());
        return first;
    }

    private void validateOutsourceIssue(SlittingRequest req) {
        if (req.getVendorId() == null) throw new ServiceException("外协厂商不能为空");
        if (req.getParentRollIds() == null || req.getParentRollIds().isEmpty()) {
            throw new ServiceException("请至少选择一个母卷发料");
        }
        if (SecurityUtils.getVendorId() != null) {
            throw new ServiceException("外协厂商账号不能建单，仅我方员工可发料");
        }
    }

    private ProSlittingRecord issueOneParent(SlittingRequest req, Long parentId) {
        // 锁内重新查询（前置校验在锁外，存在 TOCTOU 窗口）：
        // Redisson 串行化同一 rollId 的并发请求，事务内 SELECT 能看到前一个持锁者提交的最新状态
        WmRollDetail parent = rollDetailMapper.selectWmRollDetailByRollId(parentId);
        if (parent == null) throw new ServiceException("母卷不存在: rollId=" + parentId);
        if (!"IN_STOCK".equals(parent.getStatus())) {
            throw new ServiceException("母卷 " + parent.getRollCode() + " 状态非在库(" + parent.getStatus() + ")，不能发料");
        }
        if (parent.getActualWeight() == null || parent.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("母卷 " + parent.getRollCode() + " 重量异常");
        }

        String operator = SecurityUtils.getUsername();
        String slitBatchNo = genCodeWithFallback("SLITTING_CODE", "SL");

        // 先建分切单（获取 slitId 作为库存事务 sourceDocId）
        ProSlittingRecord record = buildOutsourceIssueRecord(req, parent, slitBatchNo, operator);
        slittingMapper.insertProSlittingRecord(record);

        // SPLIT 扣减 material_stock（母卷物料从原仓库出库）
        WmTransaction pickTx = buildOutsourcePickTx(parent, slitBatchNo, record.getSlitId());
        transactionService.processTransaction(pickTx);

        // 母卷状态 → OUTSOURCED（条件 UPDATE 兜底：仅 IN_STOCK 可跃迁，防并发重入）
        int rows = rollDetailMapper.markOutsourcedIfInStock(parentId, operator, DateUtils.getNowDate());
        if (rows == 0) {
            throw new ServiceException("母卷 " + parent.getRollCode() + " 已被其他操作变更，请刷新重试");
        }

        // 流转卡状态 → OUTSOURCING（外协进行中），让流转卡列表/看板体现"正在外协"
        markCardOutsourcing(record);
        return record;
    }

    /**
     * 发料时把关联的流转卡置为外协中，当前工序指向分切工序。
     * 仅 ACTIVE → OUTSOURCING：不回退已完工/已取消卡，也防并发外协单覆盖工序。
     * 卡可能尚未建立（工单未开工）——查不存在时跳过属预期；DB 异常必须向上抛出回滚发料事务。
     */
    private void markCardOutsourcing(ProSlittingRecord record) {
        if (record.getCardId() == null) return;
        ProCard existing = cardMapper.selectProCardByCardId(record.getCardId());
        if (existing == null) {
            log.debug("流转卡尚未建立，跳过外协标记: cardId={}", record.getCardId());
            return;
        }
        int rows = cardMapper.markOutsourcingIfActive(
                record.getCardId(), record.getProcessId(), record.getProcessName(),
                SecurityUtils.getUsername());
        if (rows == 0)
            log.debug("流转卡非 ACTIVE 状态，跳过外协标记（不回退已完工/已外协卡）: cardId={}, status={}",
                    record.getCardId(), existing.getStatus());
    }

    private ProSlittingRecord buildOutsourceIssueRecord(SlittingRequest req, WmRollDetail parent,
                                                         String slitBatchNo, String operator) {
        ProSlittingRecord record = new ProSlittingRecord();
        record.setSlitMode(MODE_OUTSOURCE);
        record.setVendorId(req.getVendorId());
        record.setVendorCode(req.getVendorCode());
        record.setVendorName(req.getVendorName());
        record.setSlitBatchNo(slitBatchNo);
        record.setWorkorderId(req.getWorkorderId());
        record.setWorkorderCode(req.getWorkorderCode());
        record.setRouteId(req.getRouteId());
        record.setProcessId(req.getProcessId());
        record.setProcessCode(req.getProcessCode());
        record.setProcessName(req.getProcessName());
        record.setCardId(req.getCardId());
        record.setParentRollId(parent.getRollId());
        record.setParentRollCode(parent.getRollCode());
        record.setSourceItemId(parent.getItemId());
        record.setSourceItemCode(parent.getItemCode());
        record.setSourceItemName(parent.getItemName());
        record.setSourceWarehouseId(parent.getWarehouseId());
        record.setSourceWarehouseCode(parent.getWarehouseCode());
        record.setSourceWarehouseName(parent.getWarehouseName());
        record.setParentItemId(parent.getItemId());
        record.setParentItemCode(parent.getItemCode());
        record.setParentItemName(parent.getItemName());
        record.setParentWidth(parent.getActualWidth());
        record.setParentWeight(parent.getActualWeight());
        record.setPickQty(parent.getActualWeight());
        record.setPickTime(DateUtils.getNowDate());
        record.setPickBy(operator);
        record.setStatus("ISSUED");
        record.setRemark(req.getRemark());
        record.setCreateTime(DateUtils.getNowDate());
        record.setCreateBy(operator);
        return record;
    }

    private WmTransaction buildOutsourcePickTx(WmRollDetail parent, String slitBatchNo, Long slitId) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.SPLIT.getCode());
        tx.setSourceDocType("SLITTING");
        tx.setSourceDocId(slitId);
        tx.setSourceDocCode(slitBatchNo);
        tx.setSourceLineId(0L);
        tx.setItemId(parent.getItemId());
        tx.setItemCode(parent.getItemCode());
        tx.setItemName(parent.getItemName());
        tx.setUnitOfMeasure(parent.getUnitOfMeasure() != null ? parent.getUnitOfMeasure() : "TON");
        tx.setUnitName("吨");
        tx.setQuantity(parent.getActualWeight().negate());
        tx.setBatchId(parent.getBatchId());
        tx.setBatchCode(parent.getBatchCode());
        tx.setWarehouseId(parent.getWarehouseId());
        tx.setWarehouseCode(parent.getWarehouseCode());
        tx.setWarehouseName(parent.getWarehouseName());
        tx.setTransactionTime(new Date());
        return tx;
    }

    // ════════════════════════════════════════════════════════════════
    // 外协 Step 2：厂商录结果（子卷 OUTSOURCED，记录 SLITTING）
    // ════════════════════════════════════════════════════════════════

    @Override
    public ProSlittingRecord recordOutsourceResult(Long slitId, OutsourceResultRequest request) {
        if (request == null || request.getChildRolls() == null || request.getChildRolls().isEmpty()) {
            throw new ServiceException("至少录入一条子卷结果");
        }
        for (int i = 0; i < request.getChildRolls().size(); i++) {
            ChildRollSpec c = request.getChildRolls().get(i);
            if (c.getActualWeight() == null || c.getActualWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("第" + (i + 1) + "条子卷重量必须大于0");
            }
        }
        String lockKey = "wm:slitting:" + slitId;
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> txTemplate.execute(status -> doRecordResult(slitId, request)));
    }

    private ProSlittingRecord doRecordResult(Long slitId, OutsourceResultRequest request) {
        ProSlittingRecord record = loadAndCheckVendor(slitId, "ISSUED");
        WmRollDetail parent = rollDetailMapper.selectWmRollDetailByRollId(record.getParentRollId());
        if (parent == null || !"OUTSOURCED".equals(parent.getStatus())) {
            throw new ServiceException("母卷状态异常，无法录结果");
        }

        BigDecimal parentWeight = nvl(record.getParentWeight());
        BigDecimal childTotal = sumChildWeights(request.getChildRolls());
        if (childTotal.compareTo(parentWeight) > 0) {
            throw new ServiceException("子卷总重(" + childTotal + "吨)超过母卷重量(" + parentWeight + "吨)");
        }

        String operator = SecurityUtils.getUsername();
        List<WmRollDetail> childRolls = new ArrayList<>();
        for (ChildRollSpec spec : request.getChildRolls()) {
            WmRollDetail child = buildChildRoll(spec, parent, record.getSlitBatchNo(), operator, "OUTSOURCED");
            // 子卷在厂商处，仓库暂记母卷原仓库（收货时可改）
            rollDetailMapper.insertWmRollDetail(child);
            childRolls.add(child);
        }

        // 追溯在收货时写（子卷入库后才形成真实物料流转），此处仅建子卷记录。

        BigDecimal loss = parentWeight.subtract(childTotal);
        record.setChildCount(childRolls.size());
        record.setChildTotalWeight(childTotal);
        record.setLossWeight(loss);
        record.setLossRate(lossRate(loss, parentWeight));
        record.setStatus("SLITTING");
        record.setSlitTime(DateUtils.getNowDate());
        record.setUpdateBy(operator);
        record.setUpdateTime(DateUtils.getNowDate());
        slittingMapper.updateProSlittingRecord(record);
        record.setChildRolls(childRolls);

        log.info("外协厂商录结果: slitId={}, 批次={}, 子卷={}卷, 厂商={}",
                slitId, record.getSlitBatchNo(), childRolls.size(), record.getVendorName());
        return record;
    }

    // ════════════════════════════════════════════════════════════════
    // 外协 Step 3：我方收货（子卷 IN_STOCK + 入库，母卷 CONSUMED + 报工）
    // ════════════════════════════════════════════════════════════════

    @Override
    public ProSlittingRecord receiveOutsource(Long slitId, OutsourceReceiveRequest request) {
        if (SecurityUtils.getVendorId() != null) {
            throw new ServiceException("外协厂商账号不能收货，仅我方员工可确认收货");
        }
        String lockKey = "wm:slitting:" + slitId;
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> txTemplate.execute(status -> doReceive(slitId, request)));
    }

    private ProSlittingRecord doReceive(Long slitId, OutsourceReceiveRequest request) {
        ProSlittingRecord record = slittingMapper.selectProSlittingRecordBySlitId(slitId);
        if (record == null) throw new ServiceException("分切单不存在: " + slitId);
        if (!"SLITTING".equals(record.getStatus())) {
            throw new ServiceException("当前状态(" + record.getStatus() + ")不能收货，仅 SLITTING 可收货");
        }
        WmRollDetail parent = rollDetailMapper.selectWmRollDetailByRollId(record.getParentRollId());
        if (parent == null) throw new ServiceException("母卷不存在");

        List<WmRollDetail> children = loadOutsourceChildren(record.getSlitBatchNo(), parent.getRollId());
        if (children.isEmpty()) throw new ServiceException("未找到厂商录入的子卷，无法收货");

        // 重量平衡校验前置（fail-fast）：避免入库/建报工/推进任务后才因损耗超标回滚
        BigDecimal childTotal = children.stream().map(c -> nvl(c.getActualWeight())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal edgeKg = request != null && request.getEdgeWeight() != null ? request.getEdgeWeight() : BigDecimal.ZERO;
        BigDecimal edgeTon = edgeKg.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        validateWeightBalance(nvl(record.getParentWeight()), childTotal, edgeTon);

        Long whId = request != null && request.getReceiveWarehouseId() != null
                ? request.getReceiveWarehouseId() : parent.getWarehouseId();
        String whCode = request != null && request.getReceiveWarehouseCode() != null
                ? request.getReceiveWarehouseCode() : parent.getWarehouseCode();
        String whName = request != null && request.getReceiveWarehouseName() != null
                ? request.getReceiveWarehouseName() : parent.getWarehouseName();

        String operator = SecurityUtils.getUsername();
        List<WmTransaction> recptTxList = receiveChildrenIntoStock(children, whId, whCode, whName, record, operator);
        receiveEdgeIfAny(request, parent, record, operator);

        parent.setStatus("CONSUMED");
        parent.setRemainingQuantity(BigDecimal.ZERO);
        parent.setUpdateTime(DateUtils.getNowDate());
        parent.setUpdateBy(operator);
        rollDetailMapper.updateWmRollDetail(parent);

        ProFeedback fb = createOutsourceFeedback(record, parent, children, operator);
        writeSlitTraces(parent, children, fb.getRecordId(), record.getWorkorderId());
        // 库存级物料转换边：MATERIAL_STOCK(母卷) ─SLIT→ MATERIAL_STOCK(子卷)，
        // 与 ROLL→ROLL 边互补，让从库存维度追溯能直接看到分切转换关系
        writeStockSlitTraces(parent, recptTxList, fb.getRecordId(), record.getWorkorderId());

        // 推进排产任务：外协收货等同于该工序产出（与通用外协 OutsourceServiceImpl.advanceTask 一致），
        // 否则 ProTask 产量不增、状态不推进，任务看板/报表与工单结案状态不一致。
        advanceTask(record, children, operator);

        finalizeReceiveRecord(record, request, parent, children, fb, operator);
        record.setChildRolls(children);
        log.info("外协收货完成: slitId={}, 批次={}, 子卷={}卷, 操作人={}",
                slitId, record.getSlitBatchNo(), children.size(), operator);
        return record;
    }

    private List<WmRollDetail> loadOutsourceChildren(String slitBatchNo, Long parentRollId) {
        WmRollDetail q = new WmRollDetail();
        q.setSlitBatchNo(slitBatchNo);
        q.setParentRollId(parentRollId);
        List<WmRollDetail> all = rollDetailMapper.selectWmRollDetailList(q);
        List<WmRollDetail> outsourced = new ArrayList<>();
        for (WmRollDetail c : all) {
            if ("OUTSOURCED".equals(c.getStatus())) outsourced.add(c);
        }
        return outsourced;
    }

    private List<WmTransaction> receiveChildrenIntoStock(List<WmRollDetail> children, Long whId, String whCode,
                                           String whName, ProSlittingRecord record, String operator) {
        // 按物料聚合入库（不同子卷物料分别入库存）
        Map<Long, List<WmRollDetail>> byItem = new LinkedHashMap<>();
        for (WmRollDetail c : children) {
            c.setStatus("IN_STOCK");
            c.setWarehouseId(whId);
            c.setWarehouseCode(whCode);
            c.setWarehouseName(whName);
            c.setRemainingQuantity(c.getActualWeight());
            c.setUpdateTime(DateUtils.getNowDate());
            c.setUpdateBy(operator);
            rollDetailMapper.updateWmRollDetail(c);
            byItem.computeIfAbsent(c.getItemId(), k -> new ArrayList<>()).add(c);
        }
        List<WmTransaction> txList = new ArrayList<>();
        for (List<WmRollDetail> group : byItem.values()) {
            WmRollDetail first = group.get(0);
            BigDecimal total = group.stream().map(c -> nvl(c.getActualWeight())).reduce(BigDecimal.ZERO, BigDecimal::add);
            WmTransaction recptTx = buildRecptTx(first, total, record.getSlitBatchNo(), record.getSlitId());
            WmTransaction processed = transactionService.processTransaction(recptTx);
            txList.add(processed);
            // 回填子卷的库存 ID（用于写 MATERIAL_STOCK→MATERIAL_STOCK 的 SLIT 追溯边）
            if (processed != null && processed.getMaterialStockId() != null) {
                for (WmRollDetail c : group) {
                    c.setMaterialStockId(processed.getMaterialStockId());
                    rollDetailMapper.updateWmRollDetail(c); // 持久化 material_stock_id，否则追溯断链
                }
            }
        }
        return txList;
    }

    private WmTransaction buildRecptTx(WmRollDetail child, BigDecimal totalWeight, String slitBatchNo, Long slitId) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.ITEM_RECPT.getCode());
        tx.setSourceDocType("SLITTING");
        tx.setSourceDocId(slitId);
        tx.setSourceDocCode(slitBatchNo);
        tx.setSourceLineId(1L);
        tx.setItemId(child.getItemId());
        tx.setItemCode(child.getItemCode());
        tx.setItemName(child.getItemName());
        tx.setUnitOfMeasure(child.getUnitOfMeasure() != null ? child.getUnitOfMeasure() : "TON");
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

    private void receiveEdgeIfAny(OutsourceReceiveRequest request, WmRollDetail parent,
                                   ProSlittingRecord record, String operator) {
        if (request == null || request.getEdgeItemId() == null
                || request.getEdgeWeight() == null || request.getEdgeWeight().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (request.getEdgeItemId().equals(parent.getItemId())) {
            throw new ServiceException("纸边物料不能与母卷物料相同");
        }
        WmTransaction edgeTx = new WmTransaction();
        edgeTx.setTransactionType(TransactionTypeEnum.MISC_RECPT.getCode());
        edgeTx.setSourceDocType("SLITTING");
        edgeTx.setSourceDocId(record.getSlitId());
        edgeTx.setSourceDocCode(record.getSlitBatchNo());
        edgeTx.setSourceLineId(2L);
        edgeTx.setItemId(request.getEdgeItemId());
        edgeTx.setItemCode(request.getEdgeItemCode());
        edgeTx.setItemName(request.getEdgeItemName());
        edgeTx.setUnitOfMeasure("KG");
        edgeTx.setUnitName("千克");
        edgeTx.setQuantity(request.getEdgeWeight());
        edgeTx.setWarehouseId(parent.getWarehouseId());
        edgeTx.setWarehouseCode(parent.getWarehouseCode());
        edgeTx.setWarehouseName(parent.getWarehouseName());
        edgeTx.setTransactionTime(new Date());
        transactionService.processTransaction(edgeTx);
    }

    private ProFeedback createOutsourceFeedback(ProSlittingRecord record, WmRollDetail parent,
                                                 List<WmRollDetail> children, String operator) {
        SlittingRequest reqStub = new SlittingRequest();
        reqStub.setWorkorderId(record.getWorkorderId());
        reqStub.setWorkorderCode(record.getWorkorderCode());
        reqStub.setRouteId(record.getRouteId());
        reqStub.setProcessId(record.getProcessId());
        reqStub.setProcessCode(record.getProcessCode());
        reqStub.setProcessName(record.getProcessName());
        reqStub.setCardId(record.getCardId());
        reqStub.setVendorId(record.getVendorId());
        reqStub.setVendorCode(record.getVendorCode());
        reqStub.setVendorName(record.getVendorName());
        List<ChildRollSpec> specs = new ArrayList<>();
        for (WmRollDetail c : children) {
            ChildRollSpec s = new ChildRollSpec();
            s.setItemId(c.getItemId());
            s.setItemCode(c.getItemCode());
            s.setItemName(c.getItemName());
            s.setActualWidth(c.getActualWidth());
            s.setActualWeightGsm(c.getActualWeightGsm());
            s.setActualLength(c.getActualLength());
            s.setActualWeight(c.getActualWeight());
            specs.add(s);
        }
        WmRollDetail stub = new WmRollDetail();
        stub.setItemId(parent.getItemId());
        stub.setItemCode(parent.getItemCode());
        stub.setItemName(parent.getItemName());
        stub.setActualWeight(record.getParentWeight());
        return createAndPersistFeedback(reqStub, stub, specs, operator, "OUTSOURCE");
    }

    private void finalizeReceiveRecord(ProSlittingRecord record, OutsourceReceiveRequest request,
                                        WmRollDetail parent, List<WmRollDetail> children,
                                        ProFeedback fb, String operator) {
        BigDecimal childTotal = children.stream().map(c -> nvl(c.getActualWeight())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal edgeKg = request != null && request.getEdgeWeight() != null ? request.getEdgeWeight() : BigDecimal.ZERO;
        BigDecimal edgeTon = edgeKg.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
        BigDecimal parentWeight = nvl(record.getParentWeight());
        BigDecimal loss = parentWeight.subtract(childTotal).subtract(edgeTon);
        if (loss.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("子卷+纸边总重超过母卷重量，请检查数据");
        }
        if (lossRate(loss, parentWeight).compareTo(MAX_LOSS_RATE) > 0) {
            throw new ServiceException("损耗率" + lossRate(loss, parentWeight) + "%超过" + MAX_LOSS_RATE + "%上限");
        }
        record.setFeedbackId(fb.getRecordId());
        record.setChildCount(children.size());
        record.setChildTotalWeight(childTotal);
        if (request != null) {
            record.setEdgeItemId(request.getEdgeItemId());
            record.setEdgeItemCode(request.getEdgeItemCode());
            record.setEdgeItemName(request.getEdgeItemName());
            record.setEdgeWeight(request.getEdgeWeight());
        }
        record.setLossWeight(loss);
        record.setLossRate(lossRate(loss, parentWeight));
        record.setStatus("RECEIVED");
        record.setSlitTime(DateUtils.getNowDate());
        record.setUpdateBy(operator);
        record.setUpdateTime(DateUtils.getNowDate());
        slittingMapper.updateProSlittingRecord(record);
    }

    /**
     * 收货后推进排产任务：累加合格产量，达计划量则 COMPLETED。
     * 与通用外协 OutsourceServiceImpl.advanceTask 保持一致口径；无任务（未排产）时跳过。
     */
    private void advanceTask(ProSlittingRecord record, List<WmRollDetail> children, String operator) {
        if (record.getWorkorderId() == null || record.getProcessId() == null) return;
        ProTask query = new ProTask();
        query.setWorkorderId(record.getWorkorderId());
        query.setProcessId(record.getProcessId());
        List<ProTask> tasks = proTaskMapper.selectProTaskList(query);
        if (tasks == null || tasks.isEmpty()) return;
        ProTask target = tasks.stream()
                .filter(t -> ProConstants.TASK_STATUS_PRODUCING.equals(t.getStatus()))
                .findFirst().orElse(null);
        if (target == null) {
            log.warn("分切外协收货未找到生产中任务: workorderId={}, processId={}",
                    record.getWorkorderId(), record.getProcessId());
            return;
        }
        BigDecimal totalQty = children.stream()
                .map(c -> nvl(c.getActualWeight())).reduce(BigDecimal.ZERO, BigDecimal::add);
        proTaskMapper.addQuantityProduced(target.getTaskId(), totalQty, totalQty, BigDecimal.ZERO);
        ProTask updated = proTaskMapper.selectProTaskByTaskId(target.getTaskId());
        if (updated != null && ProConstants.TASK_STATUS_PRODUCING.equals(updated.getStatus())) {
            BigDecimal produced = nvl(updated.getQuantityProduced());
            BigDecimal planned = nvl(updated.getQuantity());
            if (planned.compareTo(BigDecimal.ZERO) > 0 && produced.compareTo(planned) >= 0) {
                // 条件完成：仅 PRODUCING → COMPLETED，防收货与工单取消并发回退
                int rows = proTaskMapper.completeTaskIfProducing(updated.getTaskId(), operator);
                if (rows > 0)
                    log.info("分切外协收货推进任务完成: taskId={}, produced={}/{}", updated.getTaskId(), produced, planned);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    // 厂商权限校验 + 查询
    // ════════════════════════════════════════════════════════════════

    private ProSlittingRecord loadAndCheckVendor(Long slitId, String expectStatus) {
        ProSlittingRecord record = slittingMapper.selectProSlittingRecordBySlitId(slitId);
        if (record == null) throw new ServiceException("分切单不存在: " + slitId);
        Long vendorId = currentVendorIdOrNull();
        if (vendorId != null && !vendorId.equals(record.getVendorId())) {
            throw new ServiceException("无权操作该分切单（非本厂商单据）");
        }
        if (expectStatus != null && !expectStatus.equals(record.getStatus())) {
            throw new ServiceException("当前状态(" + record.getStatus() + ")不允许此操作，需 " + expectStatus);
        }
        return record;
    }

    /**
     * 取当前登录厂商 ID：无认证上下文（定时任务/系统调用）返回 null；
     * 有认证但取 vendorId 失败时直接抛异常（fail-closed），避免越权看到全部数据。
     */
    private Long currentVendorIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) return null;
        return SecurityUtils.getVendorId();
    }

    @Override
    public List<WmMaterialStock> listAvailableStock(Long itemId) {
        return materialStockMapper.selectAvailableBatches(itemId, null, "NORMAL");
    }

    @Override
    public List<WmRollDetail> listAvailableParentRolls(Long itemId) {
        WmRollDetail query = new WmRollDetail();
        query.setStatus("IN_STOCK");
        if (itemId != null) query.setItemId(itemId);
        return rollDetailMapper.selectWmRollDetailList(query);
    }

    @Override
    public List<ProSlittingRecord> selectList(ProSlittingRecord query) {
        Long vendorId = currentVendorIdOrNull();
        if (vendorId != null) {
            query.setVendorId(vendorId);
        }
        return slittingMapper.selectProSlittingRecordList(query);
    }

    @Override
    public ProSlittingRecord selectBySlitId(Long slitId) {
        ProSlittingRecord record = slittingMapper.selectProSlittingRecordBySlitId(slitId);
        if (record == null) return null;
        Long vendorId = currentVendorIdOrNull();
        if (vendorId != null && !vendorId.equals(record.getVendorId())) {
            throw new ServiceException("无权查看该分切单");
        }
        if (record.getParentRollId() != null) {
            WmRollDetail parent = rollDetailMapper.selectWmRollDetailByRollId(record.getParentRollId());
            if (parent != null) {
                List<WmRollDetail> parents = new ArrayList<>();
                parents.add(parent);
                record.setParentRolls(parents);
            }
        }
        WmRollDetail childQuery = new WmRollDetail();
        childQuery.setSlitBatchNo(record.getSlitBatchNo());
        if (record.getParentRollId() != null) {
            childQuery.setParentRollId(record.getParentRollId());
        }
        record.setChildRolls(rollDetailMapper.selectWmRollDetailList(childQuery));
        return record;
    }

    // ════════════════════════════════════════════════════════════════
    // 工具
    // ════════════════════════════════════════════════════════════════

    private BigDecimal sumChildWeights(List<ChildRollSpec> childRolls) {
        BigDecimal total = BigDecimal.ZERO;
        if (childRolls == null) return total;
        for (ChildRollSpec c : childRolls) {
            total = total.add(nvl(c.getActualWeight()));
        }
        return total;
    }

    /**
     * 生成编码，自动编码规则不可用时以「前缀 + 时间戳 + 4 位随机数」兜底（DB 唯一约束最终兜底）。
     */
    private String genCodeWithFallback(String ruleName, String fallbackPrefix) {
        if (autoCodeGenerator != null) {
            try {
                String code = autoCodeGenerator.genSerialCode(ruleName, null);
                if (code != null && !code.isEmpty()) return code;
            } catch (Exception ignored) { /* fall through to timestamp */ }
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        int rand = (int) (Math.random() * 10000);
        return fallbackPrefix + ts + String.format("%04d", rand);
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
