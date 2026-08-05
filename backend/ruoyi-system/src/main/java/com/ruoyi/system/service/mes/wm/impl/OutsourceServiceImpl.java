package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.TransactionTypeEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdVendor;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.wm.OutsourceRequest;
import com.ruoyi.system.domain.mes.wm.WmOutsourceIssueLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.mapper.mes.md.MdVendorMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.mapper.mes.wm.WmOutsourceLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmOutsourceOrderMapper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IOutsourceService;
import com.ruoyi.system.service.mes.wm.IWmTransactionService;
import com.ruoyi.system.service.mes.wm.OutsourceResultStrategy;

/**
 * 通用外协服务实现。
 *
 * 三步流程（发料→录结果→收货）从 ProSlittingServiceImpl 抽取，
 * 不依赖 WmRollDetail，仅依赖通用 itemId/batchId/warehouseId。
 * 分切等业务通过 OutsourceResultStrategy 注入领域逻辑。
 *
 * @author qixiaoxia
 */
@Service
public class OutsourceServiceImpl implements IOutsourceService
{
    private static final Logger log = LoggerFactory.getLogger(OutsourceServiceImpl.class);

    private static final String STATUS_ISSUED = "ISSUED";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_RECEIVED = "RECEIVED";

    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private WmOutsourceOrderMapper orderMapper;
    @Autowired private WmOutsourceLineMapper lineMapper;
    @Autowired private MdVendorMapper vendorMapper;
    @Autowired private IWmTransactionService transactionService;
    @Autowired private ProFeedbackMapper feedbackMapper;
    @Autowired private ProWorkorderMapper workorderMapper;
    @Autowired private ProRouteProcessMapper routeProcessMapper;
    @Autowired private ProCardMapper cardMapper;
    @Autowired private ProMaterialTraceMapper traceMapper;
    @Autowired private AutoCodeGenerator autoCodeGenerator;

    private TransactionTemplate txTemplate;

    /** Strategy 注册表：sourceType → strategy */
    private final Map<String, OutsourceResultStrategy> strategyMap = new ConcurrentHashMap<>();

    /** Strategy 注册入口（各业务 @PostConstruct 时调） */
    public void registerStrategy(OutsourceResultStrategy strategy)
    {
        strategyMap.put(strategy.getSourceType(), strategy);
        log.info("外协策略注册: {}", strategy.getSourceType());
    }

    private TransactionTemplate tx()
    {
        if (txTemplate == null) txTemplate = new TransactionTemplate(transactionManager);
        return txTemplate;
    }

    // ════════════════════════════════════════════════════════════════
    // Step 1：创建外协发货单 + 发料扣库存
    // ════════════════════════════════════════════════════════════════

    @Override
    public WmOutsourceOrder createOutsource(OutsourceRequest req)
    {
        validateCreate(req);
        MdVendor vendor = vendorMapper.selectMdVendorByVendorId(req.getVendorId());
        if (vendor == null) throw new ServiceException("外协厂商不存在: " + req.getVendorId());
        req.setVendorCode(vendor.getVendorCode());
        req.setVendorName(vendor.getVendorName());

        String lockKey = "wm:outsource:create:" + req.getVendorId();
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> tx().execute(status -> doCreate(req)));
    }

    private void validateCreate(OutsourceRequest req)
    {
        if (req.getVendorId() == null) throw new ServiceException("外协厂商不能为空");
        if (req.getIssueLines() == null || req.getIssueLines().isEmpty())
            throw new ServiceException("发料行不能为空");
        if (SecurityUtils.getVendorId() != null)
            throw new ServiceException("外协厂商账号不能创建发货单，仅我方员工可操作");
        for (WmOutsourceIssueLine line : req.getIssueLines())
        {
            if (line.getItemId() == null) throw new ServiceException("发料行物料不能为空");
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                throw new ServiceException("发料数量必须大于0");
        }
    }

    private WmOutsourceOrder doCreate(OutsourceRequest req)
    {
        String operator = SecurityUtils.getUsername();
        String orderCode = autoCodeGenerator.genSerialCode("OUTSOURCE_CODE", null);

        // 建头表
        WmOutsourceOrder order = new WmOutsourceOrder();
        order.setOrderCode(orderCode);
        order.setVendorId(req.getVendorId());
        order.setVendorCode(req.getVendorCode());
        order.setVendorName(req.getVendorName());
        order.setWorkorderId(req.getWorkorderId());
        order.setWorkorderCode(req.getWorkorderCode());
        order.setCardId(req.getCardId());
        order.setRouteId(req.getRouteId());
        order.setProcessId(req.getProcessId());
        order.setProcessCode(req.getProcessCode());
        order.setProcessName(req.getProcessName());
        order.setSourceType(req.getSourceType() != null ? req.getSourceType() : "GENERIC");
        order.setSourceRefId(req.getSourceRefId());
        order.setStatus(STATUS_ISSUED);
        order.setOperator(operator);
        order.setIssueTime(DateUtils.getNowDate());
        order.setRemark(req.getRemark());
        order.setCreateBy(operator);
        order.setCreateTime(DateUtils.getNowDate());

        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmOutsourceIssueLine line : req.getIssueLines())
        {
            totalQty = totalQty.add(line.getQuantity());
        }
        order.setIssueTotalQty(totalQty);
        orderMapper.insertOutsourceOrder(order);

        // 建发料行 + 扣库存
        for (WmOutsourceIssueLine line : req.getIssueLines())
        {
            line.setOrderId(order.getOrderId());
            line.setCreateBy(operator);
            line.setCreateTime(DateUtils.getNowDate());
            lineMapper.insertIssueLine(line);

            // 扣库存（ISSUE_OUT 负数）
            WmTransaction pickTx = buildPickTx(line, order);
            transactionService.processTransaction(pickTx);

            // 写发料追溯（MATERIAL_STOCK → VENDOR）
            writeIssueTrace(line, order);
        }

        // 流转卡 → OUTSOURCING
        markCardOutsourcing(order);

        log.info("外协发货单创建: orderCode={}, vendor={}, 行数={}, 总量={}",
                orderCode, req.getVendorName(), req.getIssueLines().size(), totalQty);
        return order;
    }

    // ════════════════════════════════════════════════════════════════
    // Step 2：厂商录加工结果
    // ════════════════════════════════════════════════════════════════

    @Override
    public WmOutsourceOrder recordResult(Long orderId, List<WmOutsourceRecptLine> resultLines)
    {
        if (resultLines == null || resultLines.isEmpty())
            throw new ServiceException("至少录入一条加工结果");
        String lockKey = "wm:outsource:" + orderId;
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> tx().execute(status -> doRecordResult(orderId, resultLines)));
    }

    private WmOutsourceOrder doRecordResult(Long orderId, List<WmOutsourceRecptLine> resultLines)
    {
        WmOutsourceOrder order = loadAndCheckVendor(orderId, STATUS_ISSUED);
        String operator = SecurityUtils.getUsername();

        // 回调 strategy（分切建子卷等）
        OutsourceResultStrategy strategy = strategyMap.get(order.getSourceType());
        List<WmOutsourceRecptLine> processedLines = strategy != null
                ? strategy.onRecordResult(order, resultLines)
                : resultLines;

        // 持久化收货行
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmOutsourceRecptLine line : processedLines)
        {
            line.setOrderId(orderId);
            line.setCreateBy(operator);
            line.setCreateTime(DateUtils.getNowDate());
            lineMapper.insertRecptLine(line);
            totalQty = totalQty.add(line.getQuantity());
        }

        // 订单 → PROCESSING
        WmOutsourceOrder upd = new WmOutsourceOrder();
        upd.setOrderId(orderId);
        upd.setStatus(STATUS_PROCESSING);
        upd.setRecptTotalQty(totalQty);
        upd.setUpdateBy(operator);
        upd.setUpdateTime(DateUtils.getNowDate());
        orderMapper.updateOutsourceOrder(upd);

        log.info("外协录结果: orderId={}, 行数={}", orderId, processedLines.size());
        return orderMapper.selectOutsourceOrderByOrderId(orderId);
    }

    // ════════════════════════════════════════════════════════════════
    // Step 3：我方收货（入库 + 建报工 + 追溯 + 推进卡）
    // ════════════════════════════════════════════════════════════════

    @Override
    public WmOutsourceOrder receiveOutsource(Long orderId)
    {
        if (SecurityUtils.getVendorId() != null)
            throw new ServiceException("外协厂商账号不能收货，仅我方员工可操作");
        String lockKey = "wm:outsource:" + orderId;
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> tx().execute(status -> doReceive(orderId)));
    }

    private WmOutsourceOrder doReceive(Long orderId)
    {
        WmOutsourceOrder order = loadAndCheck(orderId, STATUS_PROCESSING);
        String operator = SecurityUtils.getUsername();
        List<WmOutsourceRecptLine> recptLines = lineMapper.selectRecptLinesByOrderId(orderId);
        if (recptLines == null || recptLines.isEmpty())
            throw new ServiceException("无收货行，请先让厂商录入加工结果");

        // 收货入库（每行加库存 ITEM_RECPT 正数）
        for (WmOutsourceRecptLine line : recptLines)
        {
            WmTransaction recptTx = buildRecptTx(line, order);
            transactionService.processTransaction(recptTx);
        }

        // 建报工
        Long feedbackId = createFeedback(order, recptLines, operator);

        // 写收货追溯（VENDOR → MATERIAL_STOCK / FEEDBACK）
        for (WmOutsourceRecptLine line : recptLines)
        {
            writeRecptTrace(line, order, feedbackId);
        }

        // 回调 strategy（分切子卷→IN_STOCK 等）
        OutsourceResultStrategy strategy = strategyMap.get(order.getSourceType());
        if (strategy != null) strategy.onReceive(order, recptLines, feedbackId);

        // 流转卡恢复/推进
        if (order.getCardId() != null) advanceCard(order, feedbackId, operator);

        // 订单 → RECEIVED
        WmOutsourceOrder upd = new WmOutsourceOrder();
        upd.setOrderId(orderId);
        upd.setStatus(STATUS_RECEIVED);
        upd.setFeedbackId(feedbackId);
        upd.setReceiveTime(DateUtils.getNowDate());
        upd.setUpdateBy(operator);
        upd.setUpdateTime(DateUtils.getNowDate());
        orderMapper.updateOutsourceOrder(upd);

        log.info("外协收货完成: orderId={}, feedbackId={}", orderId, feedbackId);
        return orderMapper.selectOutsourceOrderByOrderId(orderId);
    }

    // ════════════════════════════════════════════════════════════════
    // 查询
    // ════════════════════════════════════════════════════════════════

    @Override
    public List<WmOutsourceOrder> selectList(WmOutsourceOrder query)
    {
        Long vendorId = currentVendorIdOrNull();
        if (vendorId != null) query.setVendorId(vendorId);
        return orderMapper.selectOutsourceOrderList(query);
    }

    @Override
    public WmOutsourceOrder selectByOrderId(Long orderId)
    {
        WmOutsourceOrder order = orderMapper.selectOutsourceOrderByOrderId(orderId);
        if (order == null) return null;
        // 厂商只能看自己的
        Long vendorId = currentVendorIdOrNull();
        if (vendorId != null && !vendorId.equals(order.getVendorId()))
            throw new ServiceException("无权查看此外协单");
        order.setIssueLines(lineMapper.selectIssueLinesByOrderId(orderId));
        order.setRecptLines(lineMapper.selectRecptLinesByOrderId(orderId));
        return order;
    }

    // ════════════════════════════════════════════════════════════════
    // 私有辅助
    // ════════════════════════════════════════════════════════════════

    private WmOutsourceOrder loadAndCheck(Long orderId, String expectStatus)
    {
        WmOutsourceOrder order = orderMapper.selectOutsourceOrderByOrderId(orderId);
        if (order == null) throw new ServiceException("外协单不存在: " + orderId);
        if (!expectStatus.equals(order.getStatus()))
            throw new ServiceException("外协单状态非" + expectStatus + "(当前:" + order.getStatus() + ")，不能操作");
        return order;
    }

    private WmOutsourceOrder loadAndCheckVendor(Long orderId, String expectStatus)
    {
        WmOutsourceOrder order = loadAndCheck(orderId, expectStatus);
        Long vendorId = currentVendorIdOrNull();
        if (vendorId != null && !vendorId.equals(order.getVendorId()))
            throw new ServiceException("无权操作此外协单");
        return order;
    }

    private Long currentVendorIdOrNull()
    {
        try { return SecurityUtils.getVendorId(); }
        catch (Exception e) { return null; }
    }

    /** 构建发料扣库存事务（ISSUE_OUT 负数） */
    private WmTransaction buildPickTx(WmOutsourceIssueLine line, WmOutsourceOrder order)
    {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.OUTSOURCE_ISSUE.getCode());
        tx.setSourceDocType("OUTSOURCE");
        tx.setSourceDocId(order.getOrderId());
        tx.setSourceDocCode(order.getOrderCode());
        tx.setSourceLineId(line.getLineId() != null ? line.getLineId() : 0L);
        tx.setItemId(line.getItemId());
        tx.setItemCode(line.getItemCode());
        tx.setItemName(line.getItemName());
        tx.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
        tx.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
        tx.setQuantity(line.getQuantity().negate());
        tx.setBatchId(line.getBatchId());
        tx.setBatchCode(line.getBatchCode());
        tx.setWarehouseId(line.getWarehouseId());
        tx.setWarehouseCode(line.getWarehouseCode());
        tx.setWarehouseName(line.getWarehouseName());
        tx.setWorkorderId(order.getWorkorderId());
        tx.setWorkorderCode(order.getWorkorderCode());
        // 发料扣的是我方库存（vendorId 不设，processTransaction 兜底为 0）
        tx.setTransactionTime(new Date());
        return tx;
    }

    /** 构建收货入库事务（OUTSOURCE_RECPT 正数） */
    private WmTransaction buildRecptTx(WmOutsourceRecptLine line, WmOutsourceOrder order)
    {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.OUTSOURCE_RECPT.getCode());
        tx.setSourceDocType("OUTSOURCE");
        tx.setSourceDocId(order.getOrderId());
        tx.setSourceDocCode(order.getOrderCode());
        tx.setSourceLineId(line.getLineId() != null ? line.getLineId() : 0L);
        tx.setItemId(line.getItemId());
        tx.setItemCode(line.getItemCode());
        tx.setItemName(line.getItemName());
        tx.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
        tx.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
        tx.setQuantity(line.getQuantity());
        tx.setBatchId(line.getBatchId());
        tx.setBatchCode(line.getBatchCode());
        tx.setWarehouseId(line.getWarehouseId());
        tx.setWarehouseCode(line.getWarehouseCode());
        tx.setWarehouseName(line.getWarehouseName());
        tx.setWorkorderId(order.getWorkorderId());
        tx.setWorkorderCode(order.getWorkorderCode());
        tx.setVendorId(order.getVendorId());
        tx.setTransactionTime(new Date());
        return tx;
    }

    /** 发料追溯：MATERIAL_STOCK → VENDOR */
    private void writeIssueTrace(WmOutsourceIssueLine line, WmOutsourceOrder order)
    {
        try
        {
            ProMaterialTrace trace = new ProMaterialTrace();
            trace.setTraceType("OUTSOURCE_ISSUE");
            trace.setParentType("MATERIAL_STOCK");
            trace.setParentId(line.getSourceRefId() != null ? line.getSourceRefId() : 0L);
            trace.setChildType("VENDOR");
            trace.setChildId(order.getVendorId());
            trace.setQuantity(line.getQuantity());
            trace.setUnitOfMeasure(line.getUnitOfMeasure());
            trace.setUnitName(line.getUnitName());
            trace.setVendorId(order.getVendorId());
            trace.setWorkorderId(order.getWorkorderId());
            trace.setTraceTime(DateUtils.getNowDate());
            trace.setCreateTime(DateUtils.getNowDate());
            trace.setCreateBy(SecurityUtils.getUsername());
            traceMapper.insertProMaterialTrace(trace);
        }
        catch (Exception e)
        {
            log.error("写外协发料追溯失败, orderId={}", order.getOrderId(), e);
        }
    }

    /** 收货追溯：VENDOR → FEEDBACK */
    private void writeRecptTrace(WmOutsourceRecptLine line, WmOutsourceOrder order, Long feedbackId)
    {
        try
        {
            ProMaterialTrace trace = new ProMaterialTrace();
            trace.setTraceType("OUTSOURCE_RECPT");
            trace.setParentType("VENDOR");
            trace.setParentId(order.getVendorId());
            trace.setChildType("FEEDBACK");
            trace.setChildId(feedbackId);
            trace.setQuantity(line.getQuantity());
            trace.setUnitOfMeasure(line.getUnitOfMeasure());
            trace.setUnitName(line.getUnitName());
            trace.setVendorId(order.getVendorId());
            trace.setWorkorderId(order.getWorkorderId());
            trace.setFeedbackId(feedbackId);
            trace.setTraceTime(DateUtils.getNowDate());
            trace.setCreateTime(DateUtils.getNowDate());
            trace.setCreateBy(SecurityUtils.getUsername());
            traceMapper.insertProMaterialTrace(trace);
        }
        catch (Exception e)
        {
            log.error("写外协收货追溯失败, orderId={}", order.getOrderId(), e);
        }
    }

    /** 流转卡 → OUTSOURCING */
    private void markCardOutsourcing(WmOutsourceOrder order)
    {
        if (order.getCardId() == null) return;
        try
        {
            ProCard cardUpd = new ProCard();
            cardUpd.setCardId(order.getCardId());
            cardUpd.setStatus("OUTSOURCING");
            if (order.getProcessId() != null)
            {
                cardUpd.setCurrentProcessId(order.getProcessId());
                cardUpd.setCurrentProcessName(order.getProcessName());
            }
            cardUpd.setUpdateBy(SecurityUtils.getUsername());
            cardUpd.setUpdateTime(DateUtils.getNowDate());
            cardMapper.updateProCard(cardUpd);
        }
        catch (Exception e)
        {
            log.error("流转卡标记外协中失败, cardId={}", order.getCardId(), e);
        }
    }

    /** 流转卡恢复 ACTIVE 或推进 */
    private void advanceCard(WmOutsourceOrder order, Long feedbackId, String operator)
    {
        try
        {
            ProFeedback fb = feedbackMapper.selectProFeedbackByRecordId(feedbackId);
            if (fb == null) return;
            ProCard cardUpd = new ProCard();
            cardUpd.setCardId(order.getCardId());
            cardUpd.setCurrentProcessId(order.getProcessId());
            cardUpd.setCurrentProcessName(order.getProcessName());
            boolean isLast = isLastProcess(order);
            if (isLast)
            {
                ProCard card = cardMapper.selectProCardByCardId(order.getCardId());
                if (card != null)
                {
                    BigDecimal produced = nvl(feedbackMapper.sumAuditedQualifiedByCardAndProcess(
                            order.getCardId(), order.getProcessId()));
                    BigDecimal planned = nvl(card.getQuantityTransfered());
                    if (produced.compareTo(planned) >= 0) cardUpd.setStatus("COMPLETED");
                }
            }
            else
            {
                cardUpd.setStatus("ACTIVE");
            }
            cardUpd.setUpdateBy(operator);
            cardUpd.setUpdateTime(DateUtils.getNowDate());
            cardMapper.updateProCard(cardUpd);
        }
        catch (Exception e)
        {
            log.error("流转卡推进失败, cardId={}", order.getCardId(), e);
        }
    }

    private boolean isLastProcess(WmOutsourceOrder order)
    {
        if (order.getRouteId() == null || order.getProcessId() == null) return false;
        ProRouteProcess last = routeProcessMapper.selectLastProcessByRouteId(order.getRouteId());
        return last != null && last.getProcessId().equals(order.getProcessId());
    }

    /** 建轻量报工（status=AUDITED，只插主表） */
    private Long createFeedback(WmOutsourceOrder order, List<WmOutsourceRecptLine> lines, String operator)
    {
        WmOutsourceIssueLine firstIssue = lineMapper.selectIssueLinesByOrderId(order.getOrderId()).get(0);
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackType("OUTSOURCE");
        fb.setFeedbackCode(autoCodeGenerator.genSerialCode("FEEDBACK_CODE", null));
        fb.setWorkorderId(order.getWorkorderId() != null ? order.getWorkorderId() : 0L);
        fb.setWorkorderCode(order.getWorkorderCode());
        fb.setProcessId(order.getProcessId() != null ? order.getProcessId() : 0L);
        fb.setProcessCode(order.getProcessCode());
        fb.setProcessName(order.getProcessName());
        fb.setCardId(order.getCardId());
        fb.setRouteId(order.getRouteId() != null ? order.getRouteId() : 0L);
        fb.setWorkstationId(0L);
        fb.setItemId(firstIssue.getItemId());
        fb.setItemCode(firstIssue.getItemCode());
        fb.setItemName(firstIssue.getItemName());
        fb.setUnitOfMeasure(firstIssue.getUnitOfMeasure() != null ? firstIssue.getUnitOfMeasure() : "TON");
        fb.setUnitName(firstIssue.getUnitName() != null ? firstIssue.getUnitName() : "吨");
        BigDecimal qty = lines.stream().map(l -> nvl(l.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        fb.setQuantity(qty);
        fb.setQuantityFeedback(qty);
        fb.setQuantityQualified(qty);
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
        feedbackMapper.insertProFeedback(fb);

        // 末工序回写工单产量
        if (order.getWorkorderId() != null && isLastProcess(order))
        {
            workorderMapper.addQuantityProduced(order.getWorkorderId(), qty);
        }
        return fb.getRecordId();
    }

    private static BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
