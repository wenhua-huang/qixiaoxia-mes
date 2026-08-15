package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.annotation.PostConstruct;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.enums.OutsourceStatus;
import com.ruoyi.common.enums.TransactionTypeEnum;
import com.ruoyi.common.enums.WmIssueConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.md.MdVendor;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProConstants;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.pro.ProTask;
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.wm.OutsourceRequest;
import com.ruoyi.system.domain.mes.wm.WmBatch;
import com.ruoyi.system.domain.mes.wm.WmOutsourceIssueLine;
import com.ruoyi.system.domain.mes.wm.WmOutsourceOrder;
import com.ruoyi.system.domain.mes.wm.WmOutsourceRecptLine;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.domain.mes.wm.vo.OutsourceBatchResult;
import com.ruoyi.system.mapper.mes.md.MdVendorMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProFeedbackMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProTaskMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmOutsourceLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmOutsourceOrderMapper;
import com.ruoyi.system.service.mes.pro.IProWorkorderDocService;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IOutsourceService;
import com.ruoyi.system.service.mes.wm.IWmBatchService;
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

    // 状态常量（来源统一为 OutsourceStatus 枚举，避免散落字符串）
    private static final String STATUS_DRAFT = OutsourceStatus.DRAFT.getCode();
    private static final String STATUS_ISSUED = OutsourceStatus.ISSUED.getCode();
    private static final String STATUS_VENDOR_RCVD = OutsourceStatus.VENDOR_RCVD.getCode();
    private static final String STATUS_PROCESSING = OutsourceStatus.PROCESSING.getCode();
    private static final String STATUS_FINISHED = OutsourceStatus.FINISHED.getCode();
    private static final String STATUS_SHIPPED = OutsourceStatus.SHIPPED.getCode();
    private static final String STATUS_RECEIVED = OutsourceStatus.RECEIVED.getCode();

    // 流转卡状态（qxx_pro_card.status）
    private static final String STATUS_CARD_OUTSOURCING = "OUTSOURCING";
    private static final String STATUS_CARD_ACTIVE = "ACTIVE";
    private static final String STATUS_CARD_COMPLETED = "COMPLETED";
    // 报工状态/来源
    private static final String FEEDBACK_STATUS_AUDITED = "AUDITED";
    private static final String FEEDBACK_SOURCE_OUTSOURCE = "OUTSOURCE";
    private static final String FEEDBACK_CLIENT_PC = "PC";
    // 库存事务来源单据类型
    private static final String SOURCE_DOC_OUTSOURCE = "OUTSOURCE";

    private static final String SOURCE_TYPE_SLITTING = "SLITTING";
    private static final String SOURCE_TYPE_GENERIC = "GENERIC";

    /** 无关联工单时的占位 workorderId */
    private static final Long PLACEHOLDER_WORKORDER_ID = 0L;

    @Autowired private RedisLockTemplate lockTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private WmOutsourceOrderMapper orderMapper;
    @Autowired private WmOutsourceLineMapper lineMapper;
    @Autowired private MdVendorMapper vendorMapper;
    @Autowired private IWmTransactionService transactionService;
    @Autowired private IWmBatchService wmBatchService;
    @Autowired private ProFeedbackMapper feedbackMapper;
    @Autowired private ProWorkorderMapper workorderMapper;
    @Autowired private ProRouteProcessMapper routeProcessMapper;
    @Autowired private ProCardMapper cardMapper;
    @Autowired private ProMaterialTraceMapper traceMapper;
    @Autowired private ProTaskMapper proTaskMapper;
    @Autowired private WmMaterialStockMapper materialStockMapper;
    @Autowired private AutoCodeGenerator autoCodeGenerator;
    @Autowired private IProWorkorderDocService workorderDocService;

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

        // 锁键按唯一约束域 (workorderId, processId) 加锁，与 uk_wo_process 一致；
        // 手工单(无工单/工序)退回 vendor 维度（同一厂商不并发建手工单）
        String lockKey;
        if (req.getWorkorderId() != null && req.getProcessId() != null) {
            lockKey = "wm:outsource:create:" + req.getWorkorderId() + ":" + req.getProcessId();
        } else {
            lockKey = "wm:outsource:create:vendor:" + req.getVendorId();
        }
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> tx().execute(status -> doCreate(req, vendor)));
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

    private WmOutsourceOrder doCreate(OutsourceRequest req, MdVendor vendor)
    {
        String operator = SecurityUtils.getUsername();
        boolean draft = req.isDraft();
        WmOutsourceOrder order = buildOrderHeader(req, vendor, operator, draft);
        try
        {
            orderMapper.insertOutsourceOrder(order);
        }
        catch (DuplicateKeyException e)
        {
            // (workorder_id, process_id) 唯一约束：同工序已存在外协单
            throw new ServiceException("该工单工序已存在外协单，不能重复创建");
        }
        persistIssueLines(req, order, operator, draft);
        if (!draft) markCardOutsourcing(order);
        log.info("外协{}单创建: orderCode={}, vendor={}, 行数={}, 总量={}",
                draft ? "发料草稿" : "发货", order.getOrderCode(), req.getVendorName(),
                req.getIssueLines().size(), order.getIssueTotalQty());
        return order;
    }

    /** 构建外协单头（不含发料行） */
    private WmOutsourceOrder buildOrderHeader(OutsourceRequest req, MdVendor vendor, String operator, boolean draft)
    {
        WmOutsourceOrder order = new WmOutsourceOrder();
        order.setOrderCode(autoCodeGenerator.genSerialCode("OUTSOURCE_CODE", null));
        order.setVendorId(req.getVendorId());
        order.setVendorCode(req.getVendorCode());
        order.setVendorName(req.getVendorName());
        order.setOutsourceFactoryId(vendor != null ? vendor.getOutsourceFactoryId() : null);
        order.setWorkorderId(req.getWorkorderId());
        order.setWorkorderCode(req.getWorkorderCode());
        order.setCardId(req.getCardId());
        order.setRouteId(req.getRouteId());
        order.setProcessId(req.getProcessId());
        order.setProcessCode(req.getProcessCode());
        order.setProcessName(req.getProcessName());
        order.setSourceType(resolveSourceType(req.getSourceType(), req.getProcessCode()));
        order.setSourceRefId(req.getSourceRefId());
        order.setStatus(draft ? STATUS_DRAFT : STATUS_ISSUED);
        order.setOperator(operator);
        if (!draft) order.setIssueTime(DateUtils.getNowDate());
        order.setRemark(req.getRemark());
        order.setCreateBy(operator);
        order.setCreateTime(DateUtils.getNowDate());
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmOutsourceIssueLine line : req.getIssueLines())
        {
            totalQty = totalQty.add(nvl(line.getQuantity()));
        }
        order.setIssueTotalQty(totalQty);
        return order;
    }

    /** 持久化发料行；非草稿模式同时扣库存写追溯 */
    private void persistIssueLines(OutsourceRequest req, WmOutsourceOrder order, String operator, boolean draft)
    {
        for (WmOutsourceIssueLine line : req.getIssueLines())
        {
            line.setOrderId(order.getOrderId());
            line.setCreateBy(operator);
            line.setCreateTime(DateUtils.getNowDate());
            lineMapper.insertIssueLine(line);
            if (!draft)
            {
                // PC 端直接发货可能未指定批次，FIFO 自动解析最早可用批次
                resolveFifoBatchIfMissing(line);
                WmTransaction pickTx = transactionService.processTransaction(buildPickTx(line, order));
                writeIssueTrace(line, order, pickTx.getMaterialStockId());
            }
        }
    }

    /**
     * 发料行未指定批次时（batchId 为 null 或 0），按 FIFO 查最早可用库存并回填。
     * 仅在非草稿（直接发货扣料）时调用，保证批次管理物料也能正确扣减。
     */
    private void resolveFifoBatchIfMissing(WmOutsourceIssueLine line)
    {
        if (line.getBatchId() != null && line.getBatchId() > 0) return;
        if (line.getItemId() == null) return;
        List<WmMaterialStock> stocks = materialStockMapper.selectAvailableBatches(
                line.getItemId(), line.getWarehouseId(), WmIssueConstants.QUALITY_NORMAL);
        if (stocks != null && !stocks.isEmpty())
        {
            WmMaterialStock s = stocks.get(0);
            line.setBatchId(s.getBatchId());
            line.setBatchCode(s.getBatchCode());
            if (line.getWarehouseId() == null)
            {
                line.setWarehouseId(s.getWarehouseId());
                line.setWarehouseCode(s.getWarehouseCode());
                line.setWarehouseName(s.getWarehouseName());
            }
        }
    }

    /**
     * 发料前定位真实库存桶，回填 vendorId。
     *
     * 采购收货按 PO 供应商把原料写入不同 vendor_id 桶（见 WmItemRecptServiceImpl），
     * 而发料事务默认 vendorId=0，loadMaterialStockForUpdate 的 6 元组等值匹配会
     * 因 vendor_id 不一致而查不到桶，误报"库存记录不存在"。这里按 item+batch+warehouse
     * （workorder=0 通用原料、NORMAL）忽略 vendor 定位实际库存行，供 buildPickTx 回填。
     *
     * @return 命中的库存记录；未定位到返回 null（交由后续流程报库存不存在）
     */
    private WmMaterialStock resolvePickStock(WmOutsourceIssueLine line)
    {
        if (line.getItemId() == null || line.getBatchId() == null || line.getWarehouseId() == null)
            return null;
        WmMaterialStock q = new WmMaterialStock();
        q.setItemId(line.getItemId());
        q.setBatchId(line.getBatchId());
        q.setWarehouseId(line.getWarehouseId());
        q.setWorkorderId(0L);
        q.setQualityStatus(WmIssueConstants.QUALITY_NORMAL);
        List<WmMaterialStock> list = materialStockMapper.selectWmMaterialStockList(q);
        if (list == null || list.isEmpty()) return null;
        // 多供应商桶时优先扣有现货的，按最早入库 FIFO
        return list.stream()
                .filter(s -> s.getQuantityOnhand() != null
                        && s.getQuantityOnhand().compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.comparing(
                        WmMaterialStock::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(list.get(0));
    }

    /**
     * 派生外协来源类型：调用方未明确指定(空/GENERIC)时，按工序码识别。
     * SLITTING（纸张分切）→ SLITTING；其余默认 GENERIC。
     * 让 OutsourceIssueHelper 等自动建单也能正确标注来源，避免分切单显示为通用、录结果走错策略。
     */
    public static String resolveSourceType(String sourceType, String processCode)
    {
        if (sourceType != null && !SOURCE_TYPE_GENERIC.equals(sourceType))
            return sourceType;
        if (ProConstants.PROCESS_CODE_SLITTING.equals(processCode))
            return SOURCE_TYPE_SLITTING;
        return sourceType != null ? sourceType : SOURCE_TYPE_GENERIC;
    }

    // ════════════════════════════════════════════════════════════════
    // Step 1.5：执行发料（草稿 DRAFT → 已发料 ISSUED）+ 修改发料行 + 厂商签收
    // ════════════════════════════════════════════════════════════════

    @Override
    public WmOutsourceOrder executeOutsource(Long orderId)
    {
        if (SecurityUtils.getVendorId() != null)
            throw new ServiceException("外协厂商账号不能执行发料，仅我方员工可操作");
        String lockKey = "wm:outsource:" + orderId;
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> tx().execute(status -> doExecute(orderId)));
    }

    @Override
    public OutsourceBatchResult batchExecuteOutsource(List<Long> orderIds)
    {
        return runBatch(orderIds, "发料", this::executeOutsource);
    }

    private WmOutsourceOrder doExecute(Long orderId)
    {
        WmOutsourceOrder order = loadAndCheck(orderId, STATUS_DRAFT);
        validateWorkorderStarted(order);
        // 草稿可能在开工前自动生成（cardId 为空），执行时卡已建好，补填 cardId
        // 确保 writeIssueTrace 能写出 MATERIAL_STOCK→CARD 边，避免追溯断链
        ensureCardId(order);
        String operator = SecurityUtils.getUsername();
        List<WmOutsourceIssueLine> lines = lineMapper.selectIssueLinesByOrderId(orderId);
        if (lines == null || lines.isEmpty())
            throw new ServiceException("无发料行，不能执行发料");
        // 扣库存 + 写发料追溯（草稿创建时跳过的动作，此处补做）
        for (WmOutsourceIssueLine line : lines)
        {
            // 草稿行可能未指定批次（PC 直接保存草稿），执行时按 FIFO 补最早可用批次
            resolveFifoBatchIfMissing(line);
            WmTransaction pickTx = buildPickTx(line, order);
            transactionService.processTransaction(pickTx);
            writeIssueTrace(line, order, pickTx.getMaterialStockId());
        }
        markCardOutsourcing(order);
        WmOutsourceOrder upd = new WmOutsourceOrder();
        upd.setOrderId(orderId);
        upd.setStatus(STATUS_ISSUED);
        upd.setIssueTime(DateUtils.getNowDate());
        upd.setUpdateBy(operator);
        upd.setUpdateTime(DateUtils.getNowDate());
        orderMapper.updateOutsourceOrder(upd);
        log.info("外协发料执行: orderId={}, orderCode={}", orderId, order.getOrderCode());
        return orderMapper.selectOutsourceOrderByOrderId(orderId);
    }

    @Override
    public void updateIssueLines(Long orderId, List<WmOutsourceIssueLine> lines)
    {
        if (SecurityUtils.getVendorId() != null)
            throw new ServiceException("外协厂商账号不能修改发料行，仅我方员工可操作");
        String lockKey = "wm:outsource:" + orderId;
        lockTemplate.executeWithResult(lockKey, 10,
                () -> tx().execute(status -> { doUpdateIssueLines(orderId, lines); return null; }));
    }

    private void doUpdateIssueLines(Long orderId, List<WmOutsourceIssueLine> lines)
    {
        WmOutsourceOrder order = loadAndCheck(orderId, STATUS_DRAFT);
        if (lines == null || lines.isEmpty())
            throw new ServiceException("发料行不能为空");
        String operator = SecurityUtils.getUsername();
        // 先删后插（支持增删行）
        lineMapper.deleteIssueLinesByOrderId(orderId);
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmOutsourceIssueLine line : lines)
        {
            if (line.getItemId() == null) throw new ServiceException("发料行物料不能为空");
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
                throw new ServiceException("发料数量必须大于0");
            line.setOrderId(orderId);
            line.setCreateBy(operator);
            line.setCreateTime(DateUtils.getNowDate());
            if (line.getUnitName() == null) {
                line.setUnitName(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "吨");
            }
            lineMapper.insertIssueLine(line);
            totalQty = totalQty.add(line.getQuantity());
        }
        WmOutsourceOrder upd = new WmOutsourceOrder();
        upd.setOrderId(orderId);
        upd.setIssueTotalQty(totalQty);
        upd.setUpdateBy(operator);
        upd.setUpdateTime(DateUtils.getNowDate());
        orderMapper.updateOutsourceOrder(upd);
    }

    @Override
    public void deleteOutsource(Long orderId)
    {
        if (SecurityUtils.getVendorId() != null)
            throw new ServiceException("外协厂商账号不能删除外协单，仅我方员工可操作");
        String lockKey = "wm:outsource:" + orderId;
        lockTemplate.executeWithResult(lockKey, 10,
                () -> tx().execute(status -> { doDeleteOutsource(orderId); return null; }));
    }

    private void doDeleteOutsource(Long orderId)
    {
        WmOutsourceOrder order = orderMapper.selectOutsourceOrderByOrderId(orderId);
        if (order == null) throw new ServiceException("外协单不存在");
        if (!STATUS_DRAFT.equals(order.getStatus()))
            throw new ServiceException("仅草稿状态外协单可删除，当前状态：" + order.getStatus());
        lineMapper.deleteIssueLinesByOrderId(orderId);
        orderMapper.deleteOutsourceOrderByOrderId(orderId);
        log.info("外协草稿单已删除: orderId={}, orderCode={}", orderId, order.getOrderCode());
    }

    @Override
    public WmOutsourceOrder vendorReceive(Long orderId)
    {
        if (SecurityUtils.getVendorId() == null)
            throw new ServiceException("仅外协厂商可签收，仅我方员工不能操作");
        String lockKey = "wm:outsource:" + orderId;
        return lockTemplate.executeWithResult(lockKey, 10, () -> tx().execute(status -> {
            WmOutsourceOrder order = loadAndCheckVendor(orderId, STATUS_ISSUED);
            String operator = SecurityUtils.getUsername();
            WmOutsourceOrder upd = new WmOutsourceOrder();
            upd.setOrderId(orderId);
            upd.setStatus(STATUS_VENDOR_RCVD);
            upd.setVendorReceiveTime(DateUtils.getNowDate());
            upd.setVendorReceiver(operator);
            upd.setUpdateBy(operator);
            upd.setUpdateTime(DateUtils.getNowDate());
            orderMapper.updateOutsourceOrder(upd);
            log.info("外协厂商签收: orderId={}, vendor={}", orderId, operator);
            return orderMapper.selectOutsourceOrderByOrderId(orderId);
        }));
    }

    // ════════════════════════════════════════════════════════════════
    // Step 2：厂商录加工结果 + 手动完成 + 厂商发货
    // ════════════════════════════════════════════════════════════════

    @Override
    public WmOutsourceOrder recordResult(Long orderId, List<WmOutsourceRecptLine> resultLines)
    {
        if (SecurityUtils.getVendorId() == null)
            throw new ServiceException("仅外协厂商可录入加工结果，我方员工不能操作");
        if (resultLines == null || resultLines.isEmpty())
            throw new ServiceException("至少录入一条加工结果");
        String lockKey = "wm:outsource:" + orderId;
        return lockTemplate.executeWithResult(lockKey, 10,
                () -> tx().execute(status -> doRecordResult(orderId, resultLines)));
    }

    private WmOutsourceOrder doRecordResult(Long orderId, List<WmOutsourceRecptLine> resultLines)
    {
        // 状态机：必须厂商已签收(VENDOR_RCVD) 或已在加工中(PROCESSING，补录)；
        // 不再允许 ISSUED 直接跳 PROCESSING，强制厂商先签收留审计链
        WmOutsourceOrder order = loadAndCheckVendorAny(orderId,
                STATUS_VENDOR_RCVD, STATUS_PROCESSING);
        String operator = SecurityUtils.getUsername();

        List<WmOutsourceIssueLine> issueLines = lineMapper.selectIssueLinesByOrderId(orderId);
        order.setIssueLines(issueLines);

        // 自动补全收货物料/仓库 + 校验每行数量
        for (WmOutsourceRecptLine line : resultLines)
        {
            inheritIssueInfo(line, issueLines);
            validateRecptLine(line);
        }

        // 回调 strategy（分切建子卷等）
        OutsourceResultStrategy strategy = strategyMap.get(order.getSourceType());
        List<WmOutsourceRecptLine> processedLines = strategy != null
                ? strategy.onRecordResult(order, resultLines) : resultLines;
        if (processedLines == null)
            throw new ServiceException("外协结果策略处理异常：未返回收货行");

        // strategy 可能调整数量，重新校验
        for (WmOutsourceRecptLine line : processedLines) validateRecptLine(line);

        // 持久化前先加载历史收货量（按 itemId），用于单品超收校验
        Map<Long, BigDecimal> existingRecptByItem = loadRecptQtyByItem(orderId);

        // 超收/越权物料校验必须在持久化之前：GENERIC 来源收货物料必须严格匹配发料明细，
        // 否则伪造 itemId 的收货行会先撞 NOT NULL 约束而非返回友好错误
        validatePerItemOverReceipt(order.getSourceType(), processedLines, issueLines, existingRecptByItem);

        BigDecimal totalQty = persistRecptLines(processedLines, orderId, operator);
        applyFinishedStatus(order, orderId, totalQty, processedLines, issueLines, operator, existingRecptByItem);
        log.info("外协录结果: orderId={}, 行数={}, 累计={}/{}",
                orderId, processedLines.size(),
                nvl(order.getRecptTotalQty()).add(totalQty), order.getIssueTotalQty());
        return loadFullOrder(orderId);
    }

    /**
     * 收货物料信息继承：单发料行直接继承（分切场景）；
     * 多发料行时收货行必须指定 itemId，且必须在发料行中存在（防错挂物料/批次）。
     */
    private void inheritIssueInfo(WmOutsourceRecptLine line, List<WmOutsourceIssueLine> issueLines)
    {
        if (issueLines == null || issueLines.isEmpty())
            throw new ServiceException("外协单发料行为空，无法录结果");
        if (issueLines.size() == 1)
        {
            WmOutsourceIssueLine sole = issueLines.get(0);
            inheritFromIssue(line, sole);
            return;
        }
        // 多物料：必须指定 itemId
        if (line.getItemId() == null)
            throw new ServiceException("多物料外协请指定每条结果的收货物料");
        WmOutsourceIssueLine matched = issueLines.stream()
                .filter(il -> line.getItemId().equals(il.getItemId()))
                .findFirst().orElse(null);
        if (matched == null)
            throw new ServiceException("收货物料不在发料行中: itemId=" + line.getItemId());
        inheritFromIssue(line, matched);
    }

    private void inheritFromIssue(WmOutsourceRecptLine line, WmOutsourceIssueLine issue)
    {
        if (line.getItemId() == null)
            line.setItemId(issue.getItemId());
        // 物料编码/名称按字段级补全：调用方可能只传 itemId（App 常见），不传 code/name 会撞 NOT NULL
        if (line.getItemCode() == null) line.setItemCode(issue.getItemCode());
        if (line.getItemName() == null) line.setItemName(issue.getItemName());
        if (line.getUnitOfMeasure() == null)
        {
            line.setUnitOfMeasure(issue.getUnitOfMeasure());
            line.setUnitName(issue.getUnitName());
        }
        if (line.getWarehouseId() == null)
        {
            line.setWarehouseId(issue.getWarehouseId());
            line.setWarehouseCode(issue.getWarehouseCode());
            line.setWarehouseName(issue.getWarehouseName());
        }
        // 注：batchId 不自动继承原材料批次——成品有自己的独立批次（resolveRecptBatch）
    }

    /** 校验收货行数量非空且为正 */
    private void validateRecptLine(WmOutsourceRecptLine line)
    {
        if (line.getItemId() == null) throw new ServiceException("收货行物料不能为空");
        if (line.getQuantity() == null) throw new ServiceException("收货数量不能为空");
        if (line.getQuantity().compareTo(BigDecimal.ZERO) <= 0)
            throw new ServiceException("收货数量必须大于0");
    }

    private BigDecimal persistRecptLines(List<WmOutsourceRecptLine> lines, Long orderId, String operator)
    {
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmOutsourceRecptLine line : lines)
        {
            line.setOrderId(orderId);
            line.setCreateBy(operator);
            line.setCreateTime(DateUtils.getNowDate());
            lineMapper.insertRecptLine(line);
            totalQty = totalQty.add(nvl(line.getQuantity()));
        }
        return totalQty;
    }

    /** 累计收货量（按物料），硬拒绝单品超量；录满则自动 FINISHED，否则 PROCESSING */
    private void applyFinishedStatus(WmOutsourceOrder order, Long orderId, BigDecimal addedQty,
                                     List<WmOutsourceRecptLine> addedLines,
                                     List<WmOutsourceIssueLine> issueLines, String operator,
                                     Map<Long, BigDecimal> existingRecptByItem)
    {
        BigDecimal existing = nvl(order.getRecptTotalQty());
        BigDecimal accumulated = existing.add(addedQty);
        BigDecimal issueQty = nvl(order.getIssueTotalQty());

        // 按物料维度校验超收：同 itemId 历史收货 + 本次新增 > 该物料发料量
        validatePerItemOverReceipt(order.getSourceType(), addedLines, issueLines, existingRecptByItem);

        WmOutsourceOrder upd = new WmOutsourceOrder();
        upd.setOrderId(orderId);
        upd.setRecptTotalQty(accumulated);
        // 是否收满的判定按来源类型区分：
        // - SLITTING：母子卷同为 TON，用头部汇总（收货物料不同于发料物料，不能按 itemId 匹配）
        // - GENERIC：按每个发料物料逐项判定，避免跨 UoM 头部求和导致误判 FINISHED
        boolean allReceived = SOURCE_TYPE_SLITTING.equals(order.getSourceType())
                ? isAllReceivedByHeader(issueQty, accumulated)
                : isAllReceivedPerItem(addedLines, issueLines, existingRecptByItem);
        if (allReceived)
        {
            upd.setStatus(STATUS_FINISHED);
            upd.setFinishTime(DateUtils.getNowDate());
            upd.setFinishBy(operator);
        }
        else
        {
            upd.setStatus(STATUS_PROCESSING);
        }
        upd.setUpdateBy(operator);
        upd.setUpdateTime(DateUtils.getNowDate());
        orderMapper.updateOutsourceOrder(upd);
    }

    /** SLITTING 收满判定：发料总量 > 0 且累计收货 ≥ 发料总量 */
    private boolean isAllReceivedByHeader(BigDecimal issueQty, BigDecimal accumulated)
    {
        return issueQty.compareTo(BigDecimal.ZERO) > 0 && accumulated.compareTo(issueQty) >= 0;
    }

    /** GENERIC 收满判定：每个发料物料的历史+本次累计收货都 ≥ 该物料发料量 */
    private boolean isAllReceivedPerItem(List<WmOutsourceRecptLine> addedLines,
                                         List<WmOutsourceIssueLine> issueLines,
                                         Map<Long, BigDecimal> existingRecptByItem)
    {
        if (issueLines == null || issueLines.isEmpty()) return false;
        Map<Long, BigDecimal> addedByItem = new HashMap<>();
        if (addedLines != null)
        {
            for (WmOutsourceRecptLine rl : addedLines)
            {
                if (rl.getItemId() == null) continue;
                addedByItem.merge(rl.getItemId(), nvl(rl.getQuantity()), BigDecimal::add);
            }
        }
        for (WmOutsourceIssueLine il : issueLines)
        {
            if (il.getItemId() == null) return false;
            BigDecimal issued = nvl(il.getQuantity());
            if (issued.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal hist = existingRecptByItem != null
                    ? existingRecptByItem.getOrDefault(il.getItemId(), BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal added = addedByItem.getOrDefault(il.getItemId(), BigDecimal.ZERO);
            if (hist.add(added).compareTo(issued) < 0) return false;
        }
        return true;
    }

    /** 持久化前加载历史收货量（按 itemId 汇总），供单品超收校验 */
    private Map<Long, BigDecimal> loadRecptQtyByItem(Long orderId)
    {
        Map<Long, BigDecimal> result = new HashMap<>();
        List<WmOutsourceRecptLine> history = lineMapper.selectRecptLinesByOrderId(orderId);
        if (history != null)
        {
            for (WmOutsourceRecptLine rl : history)
            {
                if (rl.getItemId() == null) continue;
                result.merge(rl.getItemId(), nvl(rl.getQuantity()), BigDecimal::add);
            }
        }
        return result;
    }

    /**
     * 按物料校验超收：同 itemId 历史+本次累计收货不得超过该物料发料量。
     * SLITTING 来源的子卷产出物料本就不同于发料母卷物料，跳过未匹配项；
     * GENERIC 来源若收货物料不在发料明细中，视为越权/错误提交，直接拒绝（防止厂商提交任意 itemId 绕过超收）。
     */
    private void validatePerItemOverReceipt(String sourceType,
                                            List<WmOutsourceRecptLine> addedLines,
                                            List<WmOutsourceIssueLine> issueLines,
                                            Map<Long, BigDecimal> existingRecptByItem)
    {
        if (addedLines == null || addedLines.isEmpty() || issueLines == null || issueLines.isEmpty()) return;
        boolean slitting = SOURCE_TYPE_SLITTING.equals(sourceType);
        Map<Long, BigDecimal> issueByItem = new HashMap<>();
        for (WmOutsourceIssueLine il : issueLines)
        {
            if (il.getItemId() == null) continue;
            issueByItem.merge(il.getItemId(), nvl(il.getQuantity()), BigDecimal::add);
        }
        Map<Long, BigDecimal> addedByItem = new HashMap<>();
        for (WmOutsourceRecptLine rl : addedLines)
        {
            if (rl.getItemId() == null) continue;
            addedByItem.merge(rl.getItemId(), nvl(rl.getQuantity()), BigDecimal::add);
        }
        for (Map.Entry<Long, BigDecimal> e : addedByItem.entrySet())
        {
            Long itemId = e.getKey();
            BigDecimal issued = issueByItem.getOrDefault(itemId, BigDecimal.ZERO);
            if (issued.compareTo(BigDecimal.ZERO) <= 0)
            {
                // 分切产出子卷物料不同于发料母卷，属正常；其余来源必须严格匹配发料明细
                if (slitting) continue;
                throw new ServiceException("收货物料[" + itemId + "]不在发料明细中，不允许收货");
            }
            BigDecimal hist = existingRecptByItem != null
                    ? existingRecptByItem.getOrDefault(itemId, BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal total = hist.add(e.getValue());
            if (total.compareTo(issued) > 0)
                throw new ServiceException("物料[" + itemId + "]累计收货(" + total + ")超过发料(" + issued + ")");
        }
    }

    @Override
    public WmOutsourceOrder complete(Long orderId)
    {
        if (SecurityUtils.getVendorId() == null)
            throw new ServiceException("仅外协厂商可完成，仅我方员工不能操作");
        String lockKey = "wm:outsource:" + orderId;
        return lockTemplate.executeWithResult(lockKey, 10, () -> tx().execute(status -> {
            WmOutsourceOrder order = loadAndCheckVendor(orderId, STATUS_PROCESSING);
            List<WmOutsourceRecptLine> recptLines = lineMapper.selectRecptLinesByOrderId(orderId);
            if (recptLines == null || recptLines.isEmpty())
                throw new ServiceException("请先录入加工结果再完成");
            String operator = SecurityUtils.getUsername();
            WmOutsourceOrder upd = new WmOutsourceOrder();
            upd.setOrderId(orderId);
            upd.setStatus(STATUS_FINISHED);
            upd.setFinishTime(DateUtils.getNowDate());
            upd.setFinishBy(operator);
            upd.setUpdateBy(operator);
            upd.setUpdateTime(DateUtils.getNowDate());
            orderMapper.updateOutsourceOrder(upd);
            log.info("外协厂商完成: orderId={}, operator={}", orderId, operator);
            return orderMapper.selectOutsourceOrderByOrderId(orderId);
        }));
    }

    @Override
    public WmOutsourceOrder ship(Long orderId)
    {
        if (SecurityUtils.getVendorId() == null)
            throw new ServiceException("仅外协厂商可发货，仅我方员工不能操作");
        String lockKey = "wm:outsource:" + orderId;
        return lockTemplate.executeWithResult(lockKey, 10, () -> tx().execute(status -> {
            WmOutsourceOrder order = loadAndCheckVendor(orderId, STATUS_FINISHED);
            String operator = SecurityUtils.getUsername();
            WmOutsourceOrder upd = new WmOutsourceOrder();
            upd.setOrderId(orderId);
            upd.setStatus(STATUS_SHIPPED);
            upd.setShipTime(DateUtils.getNowDate());
            upd.setShipBy(operator);
            upd.setUpdateBy(operator);
            upd.setUpdateTime(DateUtils.getNowDate());
            orderMapper.updateOutsourceOrder(upd);
            log.info("外协厂商发货: orderId={}, operator={}", orderId, operator);
            return orderMapper.selectOutsourceOrderByOrderId(orderId);
        }));
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

    @Override
    public OutsourceBatchResult batchReceiveOutsource(List<Long> orderIds)
    {
        return runBatch(orderIds, "收货", this::receiveOutsource);
    }

    /**
     * 批量执行模板：逐单独立事务+独立 Redis 锁（已封装在 action 内），
     * 单条失败不影响其他单，失败原因汇总返回。外层不加事务。
     */
    private OutsourceBatchResult runBatch(List<Long> orderIds, String actionName,
                                          java.util.function.Consumer<Long> action)
    {
        OutsourceBatchResult result = new OutsourceBatchResult();
        if (orderIds == null || orderIds.isEmpty())
            return result;
        result.setTotal(orderIds.size());
        int success = 0;
        for (Long orderId : orderIds)
        {
            try
            {
                action.accept(orderId);
                success++;
            }
            catch (Exception e)
            {
                String code = resolveOrderCode(orderId);
                result.getFailures().add(new OutsourceBatchResult.FailItem(
                        orderId, code, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                log.warn("外协批量{}失败: orderId={}, reason={}", actionName, orderId, e.getMessage());
            }
        }
        result.setSuccess(success);
        result.setFailed(orderIds.size() - success);
        log.info("外协批量{}完成: total={}, success={}, failed={}",
                actionName, result.getTotal(), result.getSuccess(), result.getFailed());
        return result;
    }

    private String resolveOrderCode(Long orderId)
    {
        try
        {
            WmOutsourceOrder o = orderMapper.selectOutsourceOrderByOrderId(orderId);
            return o != null ? o.getOrderCode() : "#" + orderId;
        }
        catch (Exception e)
        {
            return "#" + orderId;
        }
    }

    private WmOutsourceOrder doReceive(Long orderId)
    {
        // 严格状态机：必须厂商完工+发货（SHIPPED）后我方才能收货
        WmOutsourceOrder order = loadAndCheck(orderId, STATUS_SHIPPED);
        String operator = SecurityUtils.getUsername();

        // cardId 补填：外协发料时卡可能还没建（工单未开工），收货时卡应已存在
        ensureCardId(order);

        List<WmOutsourceRecptLine> recptLines = lineMapper.selectRecptLinesByOrderId(orderId);
        if (recptLines == null || recptLines.isEmpty())
            throw new ServiceException("无收货行，请先让厂商录入加工结果");

        // 逐行解析/生成成品独立批次（不沿用原材料批次），写回收货行，并捕获入库事务
        // （processTransaction 返回的 tx 携带实际影响的 materialStockId，用于写追溯边）
        Map<Long, WmTransaction> recptTxByLine = new HashMap<>();
        for (WmOutsourceRecptLine line : recptLines)
        {
            resolveRecptBatch(line, order, operator);
            WmTransaction recptTx = transactionService.processTransaction(buildRecptTx(line, order));
            recptTxByLine.put(line.getLineId(), recptTx);
        }

        // 建报工
        Long feedbackId = createFeedback(order, recptLines, operator);

        // 写收货追溯：
        //   ① 流转卡/工单 → 报工（外协加工边，把外协报工挂回生产主链）
        //   ② 外协单 → 报工（供应商交付，外协单为作用域锚点）
        //   ③ 报工 → 成品库存（入库边，避免入库后库存成为追溯孤儿）
        writeCardFeedbackTrace(order, feedbackId);
        for (WmOutsourceRecptLine line : recptLines)
        {
            writeRecptTrace(line, order, feedbackId);
            WmTransaction recptTx = recptTxByLine.get(line.getLineId());
            if (recptTx != null && recptTx.getMaterialStockId() != null)
            {
                writeStockinTrace(line, order, feedbackId, recptTx);
            }
        }
        // 分切单补写物料转换边：MATERIAL_STOCK(母卷) ─SLIT→ MATERIAL_STOCK(子卷)
        // 让追溯图直接体现「一卷纸切成了哪些料」，而非仅通过 CARD/FB 间接关联
        if (SOURCE_TYPE_SLITTING.equals(order.getSourceType()))
        {
            writeSlitTransformTraces(order, feedbackId, recptLines, recptTxByLine);
        }

        // 回调 strategy（分切子卷→IN_STOCK 等）；传入收货行→库存ID映射供策略回填领域对象
        OutsourceResultStrategy strategy = strategyMap.get(order.getSourceType());
        if (strategy != null)
        {
            Map<Long, Long> stockIdByLineId = new HashMap<>();
            for (Map.Entry<Long, WmTransaction> e : recptTxByLine.entrySet())
            {
                if (e.getValue() != null && e.getValue().getMaterialStockId() != null)
                    stockIdByLineId.put(e.getKey(), e.getValue().getMaterialStockId());
            }
            strategy.onReceive(order, recptLines, feedbackId, stockIdByLineId);
        }

        // 推进任务状态：外协收货 → 任务 COMPLETED + 累加产量（失败将回滚收货事务）
        advanceTask(order, recptLines, operator);

        // 流转卡恢复/推进（只前进不回退）。
        // 外协单创建早于开工建卡时单上 cardId 为空，发料标记与收货推进都会被跳过：
        // 收货时按工单回补卡关联（仅取 ACTIVE 卡，避免干扰其它外协单占用的卡），
        // 补走 OUTSOURCING 标记（对已标记卡幂等无副作用）后推进，与发料即关联的卡链路一致
        resolveOrderCardIfAbsent(order);
        if (order.getCardId() != null)
        {
            markCardOutsourcing(order);
            advanceCard(order, feedbackId, operator);
        }

        // 订单 → RECEIVED（回补的 cardId 随之一并落库，后续操作不再缺关联）
        WmOutsourceOrder upd = new WmOutsourceOrder();
        upd.setOrderId(orderId);
        upd.setStatus(STATUS_RECEIVED);
        upd.setFeedbackId(feedbackId);
        upd.setCardId(order.getCardId());
        upd.setReceiveTime(DateUtils.getNowDate());
        upd.setUpdateBy(operator);
        upd.setUpdateTime(DateUtils.getNowDate());
        orderMapper.updateOutsourceOrder(upd);

        log.info("外协收货完成: orderId={}, feedbackId={}", orderId, feedbackId);
        return loadFullOrder(orderId);
    }

    /**
     * 解析收货行成品批次：物料启用批次管理时，按 (物料+厂商+生产日期+有效期+批号)
     * 查重或新建独立成品批次，写回 batchId/batchCode 并持久化收货行。
     * 厂商未填日期时用当天兜底。未启用批次管理则跳过。
     */
    private void resolveRecptBatch(WmOutsourceRecptLine line, WmOutsourceOrder order, String operator)
    {
        if (line.getBatchId() != null) return; // 已带批次（如分切策略生成），不重复生成
        WmBatch param = new WmBatch();
        param.setItemId(line.getItemId());
        param.setItemCode(line.getItemCode());
        param.setItemName(line.getItemName());
        param.setSpecification(line.getSpecification());
        param.setVendorId(order.getVendorId());
        param.setVendorCode(order.getVendorCode());
        param.setVendorName(order.getVendorName());
        param.setProduceDate(line.getProduceDate() != null ? line.getProduceDate() : DateUtils.getNowDate());
        param.setExpireDate(line.getExpireDate());
        param.setLotNumber(line.getLotNumber());
        WmBatch generated = wmBatchService.getOrGenerateBatchCode(param);
        if (generated != null)
        {
            line.setBatchId(generated.getBatchId());
            line.setBatchCode(generated.getBatchCode());
        }
        line.setUpdateBy(operator);
        line.setUpdateTime(DateUtils.getNowDate());
        int updated = lineMapper.updateRecptLine(line);
        if (updated == 0)
            throw new ServiceException("收货行批次写回失败(行可能已被删除或工厂不匹配): lineId=" + line.getLineId());
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

    /**
     * 发料前校验工单已开工：未开工则无流转卡，投料 trace 会断链。
     * 无关联工单的外协单（workorderId 为空或占位 0）跳过。
     */
    private void validateWorkorderStarted(WmOutsourceOrder order)
    {
        Long wid = order.getWorkorderId();
        if (wid == null || PLACEHOLDER_WORKORDER_ID.equals(wid)) return;
        ProWorkorder wo = workorderMapper.selectProWorkorderByWorkorderId(wid);
        if (wo == null)
            throw new ServiceException("关联工单不存在: " + wid);
        if (!ProConstants.WORKORDER_STATUS_PRODUCING.equals(wo.getStatus()))
            throw new ServiceException("工单[" + wo.getWorkorderCode() + "]尚未开工（当前状态："
                    + wo.getStatus() + "），请先开工后再执行外协发料");
    }

    private WmOutsourceOrder loadAndCheckAny(Long orderId, String... expectStatuses)
    {
        WmOutsourceOrder order = orderMapper.selectOutsourceOrderByOrderId(orderId);
        if (order == null) throw new ServiceException("外协单不存在: " + orderId);
        boolean ok = false;
        for (String s : expectStatuses) if (s.equals(order.getStatus())) { ok = true; break; }
        if (!ok)
            throw new ServiceException("外协单状态不允许此操作(当前:" + order.getStatus() + ")");
        return order;
    }

    private WmOutsourceOrder loadAndCheckVendor(Long orderId, String expectStatus)
    {
        WmOutsourceOrder order = loadAndCheck(orderId, expectStatus);
        return checkVendorOwnership(order);
    }

    private WmOutsourceOrder loadAndCheckVendorAny(Long orderId, String... expectStatuses)
    {
        WmOutsourceOrder order = loadAndCheckAny(orderId, expectStatuses);
        return checkVendorOwnership(order);
    }

    private WmOutsourceOrder checkVendorOwnership(WmOutsourceOrder order)
    {
        Long vendorId = currentVendorIdOrNull();
        if (vendorId != null && !vendorId.equals(order.getVendorId()))
            throw new ServiceException("无权操作此外协单");
        return order;
    }

    private Long currentVendorIdOrNull()
    {
        // 无认证上下文（内部/系统调用）时返回 null；认证存在但取 vendorId 失败应传播，
        // 避免把认证基础设施异常误判为"内部员工"而绕过厂商权限校验。
        if (org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication() == null)
            return null;
        return SecurityUtils.getVendorId();
    }

    /** 构建发料扣库存事务（ISSUE_OUT 负数） */
    private WmTransaction buildPickTx(WmOutsourceIssueLine line, WmOutsourceOrder order)
    {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.OUTSOURCE_ISSUE.getCode());
        tx.setSourceDocType(SOURCE_DOC_OUTSOURCE);
        tx.setSourceDocId(order.getOrderId());
        tx.setSourceDocCode(order.getOrderCode());
        tx.setSourceLineId(line.getLineId() != null ? line.getLineId() : 0L);
        tx.setItemId(line.getItemId());
        tx.setItemCode(line.getItemCode());
        tx.setItemName(line.getItemName());
        tx.setSpecification(line.getSpecification());
        tx.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
        tx.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
        tx.setQuantity(line.getQuantity().negate());
        tx.setBatchId(line.getBatchId());
        tx.setBatchCode(line.getBatchCode());
        tx.setWarehouseId(line.getWarehouseId());
        tx.setWarehouseCode(line.getWarehouseCode());
        tx.setWarehouseName(line.getWarehouseName());
        // 外协发料扣的是我方原料库存（workorder_id=0 的通用库存），不设 workorderId——
        // 否则 initStock 会按 workorder_id=工单号匹配库存，找不到原料库存记录（与厂内领料 WmIssueHeaderServiceImpl 一致）。
        // 单据与工单的关联由 sourceDocType=OUTSOURCE/sourceDocId(外协单头) 及 writeIssueTrace 承载。
        tx.setWorkorderId(null);
        tx.setWorkorderCode(null);
        // 发料扣的是我方原料库存：采购收货按 PO 供应商分桶（vendor_id 非 0），
        // 必须按 item+batch+warehouse 定位真实库存桶回填 vendorId，否则 6 元组匹配
        // 会因 vendor_id=0 找不到记录而误报"库存记录不存在"。
        WmMaterialStock pickStock = resolvePickStock(line);
        if (pickStock != null)
        {
            tx.setVendorId(pickStock.getVendorId());
            tx.setLocationId(pickStock.getLocationId());
            tx.setAreaId(pickStock.getAreaId());
        }
        tx.setTransactionTime(new Date());
        return tx;
    }

    /** 构建收货入库事务（OUTSOURCE_RECPT 正数） */
    private WmTransaction buildRecptTx(WmOutsourceRecptLine line, WmOutsourceOrder order)
    {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(TransactionTypeEnum.OUTSOURCE_RECPT.getCode());
        tx.setSourceDocType(SOURCE_DOC_OUTSOURCE);
        tx.setSourceDocId(order.getOrderId());
        tx.setSourceDocCode(order.getOrderCode());
        tx.setSourceLineId(line.getLineId() != null ? line.getLineId() : 0L);
        tx.setItemId(line.getItemId());
        tx.setItemCode(line.getItemCode());
        tx.setItemName(line.getItemName());
        tx.setSpecification(line.getSpecification());
        tx.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
        tx.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
        tx.setQuantity(line.getQuantity());
        tx.setBatchId(line.getBatchId());
        tx.setBatchCode(line.getBatchCode());
        tx.setWarehouseId(line.getWarehouseId());
        tx.setWarehouseCode(line.getWarehouseCode());
        tx.setWarehouseName(line.getWarehouseName());
        // 外协收货入的是我方通用仓库库存（workorder_id=0, vendor_id=0），不设 workorderId/vendorId——
        // 否则会新建工单专属/厂商桶库存记录，与厂内产品入库(WmProductRecpt)及 buildPickTx 口径不一致；
        // 厂内 FIFO 领料(WmIssueHeaderServiceImpl) 只探测 vendor_id=0 的库存，若入到厂商桶将永远选不中。
        // 工单/厂商关联由 sourceDocType/sourceDocId(外协单) + trace 边承载。
        tx.setWorkorderId(null);
        tx.setWorkorderCode(null);
        tx.setTransactionTime(new Date());
        return tx;
    }

    /**
     * 发料追溯：写两条边
     *   1. MATERIAL_STOCK → OUTSOURCE_ORDER：外协单维度锚点，承载供应商/工序详情
     *   2. MATERIAL_STOCK → CARD：与厂内 ISSUE 边对称，让反查追溯从 CARD 能回溯到原料库存
     * child 用外协单而非供应商，避免同一供应商的全部历史外协单在追溯图上汇聚成扇出枢纽；
     * 供应商信息保留在 trace.vendor_id 和外协单节点描述上。
     */
    private void writeIssueTrace(WmOutsourceIssueLine line, WmOutsourceOrder order, Long materialStockId)
    {
        Long parentStockId = materialStockId != null ? materialStockId
                : (line.getSourceRefId() != null ? line.getSourceRefId() : 0L);
        ProMaterialTrace trace = new ProMaterialTrace();
        trace.setTraceType("OUTSOURCE_ISSUE");
        trace.setParentType("MATERIAL_STOCK");
        // 使用实际扣减的库存记录 ID（processTransaction 返回），避免 parentId=0 断链
        trace.setParentId(parentStockId);
        trace.setChildType("OUTSOURCE_ORDER");
        trace.setChildId(order.getOrderId());
        trace.setQuantity(line.getQuantity());
        trace.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
        trace.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
        trace.setVendorId(order.getVendorId());
        trace.setWorkorderId(order.getWorkorderId());
        trace.setCardId(order.getCardId());
        trace.setProcessId(order.getProcessId());
        trace.setTraceTime(DateUtils.getNowDate());
        trace.setCreateTime(DateUtils.getNowDate());
        trace.setCreateBy(SecurityUtils.getUsername());
        traceMapper.insertProMaterialTrace(trace);

        // 补 MATERIAL_STOCK → CARD 边，与厂内发料 ISSUE 边对称，修复反查追溯在 CARD 节点断联
        if (order.getCardId() != null)
        {
            ProMaterialTrace cardEdge = new ProMaterialTrace();
            cardEdge.setTraceType("OUTSOURCE_ISSUE");
            cardEdge.setParentType("MATERIAL_STOCK");
            cardEdge.setParentId(parentStockId);
            cardEdge.setChildType("CARD");
            cardEdge.setChildId(order.getCardId());
            cardEdge.setQuantity(line.getQuantity());
            cardEdge.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
            cardEdge.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
            cardEdge.setVendorId(order.getVendorId());
            cardEdge.setWorkorderId(order.getWorkorderId());
            cardEdge.setCardId(order.getCardId());
            cardEdge.setProcessId(order.getProcessId());
            cardEdge.setTraceTime(DateUtils.getNowDate());
            cardEdge.setCreateTime(DateUtils.getNowDate());
            cardEdge.setCreateBy(SecurityUtils.getUsername());
            traceMapper.insertProMaterialTrace(cardEdge);
        }
    }

    /**
     * 收货追溯：OUTSOURCE_ORDER → FEEDBACK。
     * parent 用外协单而非供应商，与发料边对称，外协单作为追溯作用域锚点。
     */
    private void writeRecptTrace(WmOutsourceRecptLine line, WmOutsourceOrder order, Long feedbackId)
    {
        ProMaterialTrace trace = new ProMaterialTrace();
        trace.setTraceType("OUTSOURCE_RECPT");
        trace.setParentType("OUTSOURCE_ORDER");
        trace.setParentId(order.getOrderId());
        trace.setChildType("FEEDBACK");
        trace.setChildId(feedbackId);
        trace.setQuantity(line.getQuantity());
        trace.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
        trace.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
        trace.setVendorId(order.getVendorId());
        trace.setWorkorderId(order.getWorkorderId());
        trace.setCardId(order.getCardId());
        trace.setProcessId(order.getProcessId());
        trace.setFeedbackId(feedbackId);
        trace.setTraceTime(DateUtils.getNowDate());
        trace.setCreateTime(DateUtils.getNowDate());
        trace.setCreateBy(SecurityUtils.getUsername());
        traceMapper.insertProMaterialTrace(trace);
    }

    /**
     * 入库追溯：FEEDBACK → MATERIAL_STOCK（外协成品入库）。
     * 与厂内产出入库 WmProductRecptServiceImpl.writeProduceStockinTrace 对等，
     * 补齐收货后到库存的边，避免入库库存成为追溯孤儿。
     */
    private void writeStockinTrace(WmOutsourceRecptLine line, WmOutsourceOrder order,
                                   Long feedbackId, WmTransaction recptTx)
    {
        ProMaterialTrace trace = new ProMaterialTrace();
        trace.setTraceType("OUTSOURCE_RECPT");
        trace.setParentType("FEEDBACK");
        trace.setParentId(feedbackId);
        trace.setChildType("MATERIAL_STOCK");
        trace.setChildId(recptTx.getMaterialStockId());
        trace.setQuantity(line.getQuantity());
        trace.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
        trace.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
        trace.setVendorId(order.getVendorId());
        trace.setWorkorderId(order.getWorkorderId());
        trace.setCardId(order.getCardId());
        trace.setProcessId(order.getProcessId());
        trace.setFeedbackId(feedbackId);
        trace.setTransactionId(recptTx.getTransactionId());
        trace.setTraceTime(DateUtils.getNowDate());
        trace.setCreateTime(DateUtils.getNowDate());
        trace.setCreateBy(SecurityUtils.getUsername());
        traceMapper.insertProMaterialTrace(trace);
    }

    /**
     * 分切物料转换追溯：MATERIAL_STOCK(母卷/原料) ─SLIT→ MATERIAL_STOCK(子卷/成品)。
     * <p>一卷原料纸发出去分切，回厂时变成多个不同规格的子卷入库（1→N 物料转换）。
     * 输入库存从本单 OUTSOURCE_ISSUE 边取（发料时扣减的母卷库存），
     * 输出库存从收货事务的 materialStockId 取。
     * <p>仅 SLITTING 来源类型调用；用 NOT EXISTS 防重复。
     */
    private void writeSlitTransformTraces(WmOutsourceOrder order, Long feedbackId,
                                          List<WmOutsourceRecptLine> recptLines,
                                          Map<Long, WmTransaction> recptTxByLine)
    {
        // 收集输入库存：本单 OUTSOURCE_ISSUE 边的 parent MATERIAL_STOCK
        List<ProMaterialTrace> issueTraces = traceMapper.selectByChild("OUTSOURCE_ORDER", order.getOrderId());
        Set<Long> inputStockIds = new java.util.LinkedHashSet<>();
        if (issueTraces != null)
        {
            for (ProMaterialTrace t : issueTraces)
            {
                if ("OUTSOURCE_ISSUE".equals(t.getTraceType())
                        && "MATERIAL_STOCK".equals(t.getParentType())
                        && t.getParentId() != null && t.getParentId() > 0)
                {
                    inputStockIds.add(t.getParentId());
                }
            }
        }
        if (inputStockIds.isEmpty()) return;

        String operator = SecurityUtils.getUsername();
        for (WmOutsourceRecptLine line : recptLines)
        {
            WmTransaction recptTx = recptTxByLine.get(line.getLineId());
            if (recptTx == null || recptTx.getMaterialStockId() == null) continue;
            Long outputStockId = recptTx.getMaterialStockId();
            for (Long inputStockId : inputStockIds)
            {
                if (inputStockId.equals(outputStockId)) continue;
                ProMaterialTrace trace = new ProMaterialTrace();
                trace.setTraceType("SLIT");
                trace.setParentType("MATERIAL_STOCK");
                trace.setParentId(inputStockId);
                trace.setChildType("MATERIAL_STOCK");
                trace.setChildId(outputStockId);
                trace.setQuantity(line.getQuantity());
                trace.setUnitOfMeasure(line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "TON");
                trace.setUnitName(line.getUnitName() != null ? line.getUnitName() : "吨");
                trace.setVendorId(order.getVendorId());
                trace.setWorkorderId(order.getWorkorderId());
                trace.setCardId(order.getCardId());
                trace.setProcessId(order.getProcessId());
                trace.setFeedbackId(feedbackId);
                trace.setTraceTime(DateUtils.getNowDate());
                trace.setCreateTime(DateUtils.getNowDate());
                trace.setCreateBy(operator);
                traceMapper.insertProMaterialTrace(trace);
            }
        }
    }

    /**
     * 外协加工追溯：CARD/WORKORDER → FEEDBACK。
     * 外协报工 createFeedback 直接走 mapper（绕过 ProFeedbackServiceImpl.insertProFeedback，
     * 避免触发 BOM 自动填充和工序序校验），故在此补齐 CARD→FEEDBACK 边，
     * 让外协报工挂回生产主链。无 cardId 时退化到 WORKORDER，两者皆无则跳过。
     */
    private void writeCardFeedbackTrace(WmOutsourceOrder order, Long feedbackId)
    {
        String parentType;
        Long parentId;
        if (order.getCardId() != null)
        {
            parentType = "CARD";
            parentId = order.getCardId();
        }
        else if (order.getWorkorderId() != null)
        {
            parentType = "WORKORDER";
            parentId = order.getWorkorderId();
        }
        else
        {
            return;
        }
        ProMaterialTrace trace = new ProMaterialTrace();
        trace.setTraceType("OUTSOURCE_PROCESS");
        trace.setParentType(parentType);
        trace.setParentId(parentId);
        trace.setChildType("FEEDBACK");
        trace.setChildId(feedbackId);
        // 数量取报工聚合数量（createFeedback 已按收货行汇总），这里查一次
        ProFeedback fb = feedbackMapper.selectProFeedbackByRecordId(feedbackId);
        trace.setQuantity(fb != null && fb.getQuantity() != null ? fb.getQuantity() : BigDecimal.ZERO);
        trace.setUnitOfMeasure(fb != null && fb.getUnitOfMeasure() != null ? fb.getUnitOfMeasure() : "TON");
        trace.setUnitName(fb != null && fb.getUnitName() != null ? fb.getUnitName() : "吨");
        trace.setVendorId(order.getVendorId());
        trace.setWorkorderId(order.getWorkorderId());
        trace.setCardId(order.getCardId());
        trace.setProcessId(order.getProcessId());
        trace.setFeedbackId(feedbackId);
        trace.setTraceTime(DateUtils.getNowDate());
        trace.setCreateTime(DateUtils.getNowDate());
        trace.setCreateBy(SecurityUtils.getUsername());
        traceMapper.insertProMaterialTrace(trace);
    }

    /**
     * 流转卡 → OUTSOURCING（发料时卡可能未建，先查存在再更新）。
     * 仅 ACTIVE → OUTSOURCING：不回退 COMPLETED/CANCELLED 卡，也防止同卡不同工序并发外协单覆盖 currentProcessId。
     * 卡已 OUTSOURCING（幂等重发）返回 0 视为正常跳过。
     */
    private void markCardOutsourcing(WmOutsourceOrder order)
    {
        if (order.getCardId() == null) return;
        ProCard existing = cardMapper.selectProCardByCardId(order.getCardId());
        if (existing == null)
        {
            log.debug("流转卡尚未建立，跳过外协标记: cardId={}", order.getCardId());
            return;
        }
        int rows = cardMapper.markOutsourcingIfActive(
                order.getCardId(), order.getProcessId(), order.getProcessName(),
                SecurityUtils.getUsername());
        if (rows == 0)
            log.debug("流转卡非 ACTIVE 状态，跳过外协标记（不回退已完工/已外协卡）: cardId={}, status={}",
                    order.getCardId(), existing.getStatus());
    }

    /**
     * 外协单缺卡关联时按工单回补：建单早于开工建卡的时序下 order.cardId 为空，
     * 发料标记（markCardOutsourcing）与收货推进（advanceCard）都会因空关联被跳过。
     * 仅回补 ACTIVE 卡——不碰其它外协单已占用（OUTSOURCING）或已完结的卡；
     * 工单无 ACTIVE 卡（未开工/全部外占）保持为空，由推进逻辑自行跳过。包内可见以便单测。
     */
    void resolveOrderCardIfAbsent(WmOutsourceOrder order)
    {
        if (order.getCardId() != null || order.getWorkorderId() == null) return;
        ProCard q = new ProCard();
        q.setWorkorderId(order.getWorkorderId());
        List<ProCard> cards = cardMapper.selectProCardList(q);
        ProCard picked = (cards == null) ? null : cards.stream()
                .filter(c -> STATUS_CARD_ACTIVE.equals(c.getStatus()))
                .findFirst().orElse(null);
        if (picked == null)
        {
            log.debug("外协单缺卡关联且工单无 ACTIVE 卡，跳过回补: orderId={}, workorderId={}",
                    order.getOrderId(), order.getWorkorderId());
            return;
        }
        order.setCardId(picked.getCardId());
        log.info("外协单回补卡关联: orderId={}, workorderId={}, cardId={}",
                order.getOrderId(), order.getWorkorderId(), picked.getCardId());
    }

    /**
     * 流转卡恢复 ACTIVE 或推进（只前进不回退，防止乱序收货导致卡工序倒退）。
     * 使用条件 UPDATE（仅 OUTSOURCING → 目标状态）防止并发收货丢失更新；返回 0 视为幂等已推进。
     */
    private void advanceCard(WmOutsourceOrder order, Long feedbackId, String operator)
    {
        ProFeedback fb = feedbackMapper.selectProFeedbackByRecordId(feedbackId);
        if (fb == null) return;
        Long forwardProcessId = resolveForwardProcessId(order);
        String forwardProcessName = forwardProcessId != null ? order.getProcessName() : null;

        String targetStatus = STATUS_CARD_ACTIVE;
        if (isLastProcess(order))
        {
            ProCard card = cardMapper.selectProCardByCardId(order.getCardId());
            if (card != null)
            {
                BigDecimal produced = nvl(feedbackMapper.sumAuditedQualifiedByCardAndProcess(
                        order.getCardId(), order.getProcessId()));
                BigDecimal planned = nvl(card.getQuantityTransfered());
                if (produced.compareTo(planned) >= 0) targetStatus = STATUS_CARD_COMPLETED;
            }
        }
        int rows = cardMapper.advanceCard(
                order.getCardId(), forwardProcessId, forwardProcessName, targetStatus,
                STATUS_CARD_OUTSOURCING, operator);
        if (rows == 0)
            log.debug("流转卡推进跳过（已推进或状态不符）: cardId={}, target={}", order.getCardId(), targetStatus);
    }

    /**
     * 判断外协工序是否在卡当前工序之后：只前进不回退。
     * 串行路线（SS）：reportPos > currentPos 时返回 processId；否则返回 null（跳过覆写）。
     * 并行路线（FS）或无位置信息时保守返回 processId（允许推进）。
     */
    private Long resolveForwardProcessId(WmOutsourceOrder order)
    {
        if (order.getRouteId() == null || order.getProcessId() == null) return order.getProcessId();
        List<ProRouteProcess> routeProcesses = routeProcessMapper.selectProRouteProcessByRouteId(order.getRouteId());
        if (routeProcesses == null || routeProcesses.isEmpty()) return order.getProcessId();
        Map<Long, Integer> posByProcess = new HashMap<>();
        for (int i = 0; i < routeProcesses.size(); i++)
        {
            ProRouteProcess rp = routeProcesses.get(i);
            if (rp.getProcessId() != null) posByProcess.put(rp.getProcessId(), rp.getOrderNum() != null ? rp.getOrderNum() : i);
        }
        Integer reportPos = posByProcess.get(order.getProcessId());
        if (reportPos == null) return order.getProcessId();
        ProCard card = cardMapper.selectProCardByCardId(order.getCardId());
        if (card == null || card.getCurrentProcessId() == null) return order.getProcessId();
        Integer currentPos = posByProcess.get(card.getCurrentProcessId());
        if (currentPos == null) return order.getProcessId();
        // 只在新工序位置更靠后时才推进
        return reportPos > currentPos ? order.getProcessId() : null;
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
        if (lines == null || lines.isEmpty())
            throw new ServiceException("收货行为空，无法建报工");
        // 报工物料取收货行（成品），不再固定取首个发料行（原料），避免多物料/分切母子卷混淆
        WmOutsourceRecptLine firstRecpt = lines.get(0);
        ProFeedback fb = new ProFeedback();
        fb.setFeedbackType(FEEDBACK_SOURCE_OUTSOURCE);
        fb.setFeedbackCode(autoCodeGenerator.genSerialCode("FEEDBACK_CODE", null));
        fb.setWorkorderId(order.getWorkorderId() != null ? order.getWorkorderId() : PLACEHOLDER_WORKORDER_ID);
        fb.setWorkorderCode(order.getWorkorderCode());
        fb.setProcessId(order.getProcessId() != null ? order.getProcessId() : PLACEHOLDER_WORKORDER_ID);
        fb.setProcessCode(order.getProcessCode());
        fb.setProcessName(order.getProcessName());
        fb.setCardId(order.getCardId());
        fb.setRouteId(order.getRouteId() != null ? order.getRouteId() : PLACEHOLDER_WORKORDER_ID);
        fb.setWorkstationId(ProConstants.WS_VIRTUAL_ID);
        fb.setItemId(firstRecpt.getItemId());
        fb.setItemCode(firstRecpt.getItemCode());
        fb.setItemName(firstRecpt.getItemName());
        fb.setUnitOfMeasure(firstRecpt.getUnitOfMeasure() != null ? firstRecpt.getUnitOfMeasure() : "TON");
        fb.setUnitName(firstRecpt.getUnitName() != null ? firstRecpt.getUnitName() : "吨");
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
        fb.setFeedbackChannel(FEEDBACK_CLIENT_PC);
        fb.setFeedbackTime(DateUtils.getNowDate());
        fb.setStatus(FEEDBACK_STATUS_AUDITED);
        // 外协关联：反馈反查外协发料单及厂商（本框架无独立收货单头，收货即订单转 RECEIVED）
        fb.setOutsourceIssueId(order.getOrderId());
        fb.setOutsourceIssueCode(order.getOrderCode());
        fb.setVendorId(order.getVendorId());
        fb.setVendorCode(order.getVendorCode());
        fb.setVendorName(order.getVendorName());
        // 多工厂外协：冗余外协厂工厂ID，与外协 8 表约定一致（M7 接线，否则跨厂报工数据隔离断链）
        fb.setOutsourceFactoryId(order.getOutsourceFactoryId());
        fb.setCreateTime(DateUtils.getNowDate());
        fb.setCreateBy(operator);
        feedbackMapper.insertProFeedback(fb);

        // 末工序回写工单产量 + 自动完工（与 ProFeedbackServiceImpl.auditFeedback / ProSlittingServiceImpl 一致）
        if (order.getWorkorderId() != null && isLastProcess(order))
        {
            workorderMapper.addQuantityProduced(order.getWorkorderId(), qty);
            workorderDocService.autoCompleteWorkorderIfQualified(order.getWorkorderId());
        }
        return fb.getRecordId();
    }

    /** 加载完整订单（头 + 发料行 + 收货行），写操作后返回给前端避免数据缺失 */
    private WmOutsourceOrder loadFullOrder(Long orderId)
    {
        WmOutsourceOrder order = orderMapper.selectOutsourceOrderByOrderId(orderId);
        if (order != null)
        {
            order.setIssueLines(lineMapper.selectIssueLinesByOrderId(orderId));
            order.setRecptLines(lineMapper.selectRecptLinesByOrderId(orderId));
        }
        return order;
    }

    /**
     * 外协收货后推进任务状态：按 workorderId+processId 找任务，累加已生产量，产够则置 COMPLETED。
     * 异常向上抛出以回滚收货事务。
     */
    private void advanceTask(WmOutsourceOrder order, List<WmOutsourceRecptLine> recptLines, String operator)
    {
        if (order.getWorkorderId() == null || order.getProcessId() == null) return;
        ProTask query = new ProTask();
        query.setWorkorderId(order.getWorkorderId());
        query.setProcessId(order.getProcessId());
        List<ProTask> tasks = proTaskMapper.selectProTaskList(query);
        if (tasks == null || tasks.isEmpty()) return;
        // 只给生产中(PRODUCING)任务加产量；CANCEL/COMPLETED 跳过（取第一个活跃任务）
        ProTask target = tasks.stream()
                .filter(t -> ProConstants.TASK_STATUS_PRODUCING.equals(t.getStatus()))
                .findFirst().orElse(null);
        if (target == null)
        {
            log.warn("外协收货未找到生产中任务: workorderId={}, processId={}", order.getWorkorderId(), order.getProcessId());
            return;
        }
        BigDecimal totalQty = recptLines.stream()
                .map(l -> nvl(l.getQuantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        proTaskMapper.addQuantityProduced(target.getTaskId(), totalQty, totalQty, BigDecimal.ZERO);
        ProTask updated = proTaskMapper.selectProTaskByTaskId(target.getTaskId());
        if (updated != null && ProConstants.TASK_STATUS_PRODUCING.equals(updated.getStatus()))
        {
            BigDecimal produced = nvl(updated.getQuantityProduced());
            BigDecimal planned = nvl(updated.getQuantity());
            if (planned.compareTo(BigDecimal.ZERO) > 0 && produced.compareTo(planned) >= 0)
            {
                // 条件完成：仅 PRODUCING → COMPLETED，防收货与工单取消并发回退
                int rows = proTaskMapper.completeTaskIfProducing(updated.getTaskId(), operator);
                if (rows > 0)
                    log.info("外协收货推进任务完成: taskId={}, produced={}/{}", updated.getTaskId(), produced, planned);
            }
        }
    }

    /** 收货时补填 cardId（发料时卡可能未建，收货时应已存在） */
    private void ensureCardId(WmOutsourceOrder order)
    {
        if (order.getCardId() != null || order.getWorkorderId() == null) return;
        Long cardId = resolveActiveCardId(order.getWorkorderId());
        if (cardId == null) return;
        order.setCardId(cardId);
        WmOutsourceOrder fix = new WmOutsourceOrder();
        fix.setOrderId(order.getOrderId());
        fix.setCardId(cardId);
        orderMapper.updateOutsourceOrder(fix);
        // 同步回填已有发料追溯边的 card_id，并补插 MATERIAL_STOCK→CARD 边
        backfillIssueTraceCardId(order, cardId);
    }

    /**
     * 回填外协发料追溯边的 card_id，并补插缺失的 MATERIAL_STOCK→CARD 边。
     * 用于草稿在开工前生成（cardId 为空）、执行时卡已建好但 trace 已写入的场景。
     */
    private void backfillIssueTraceCardId(WmOutsourceOrder order, Long cardId)
    {
        List<ProMaterialTrace> issueTraces = traceMapper.selectByChild("OUTSOURCE_ORDER", order.getOrderId());
        if (issueTraces == null) return;
        for (ProMaterialTrace t : issueTraces)
        {
            if (!"OUTSOURCE_ISSUE".equals(t.getTraceType())) continue;
            if (t.getCardId() == null)
            {
                ProMaterialTrace upd = new ProMaterialTrace();
                upd.setTraceId(t.getTraceId());
                upd.setCardId(cardId);
                traceMapper.updateProMaterialTrace(upd);
            }
            Long stockId = t.getParentId();
            if (stockId == null || stockId == 0L) continue;
            boolean exists = issueTraces.stream().anyMatch(x ->
                    "CARD".equals(x.getChildType()) && cardId.equals(x.getChildId())
                    && stockId.equals(x.getParentId())
                    && "OUTSOURCE_ISSUE".equals(x.getTraceType()));
            if (exists) continue;
            ProMaterialTrace cardEdge = new ProMaterialTrace();
            cardEdge.setTraceType("OUTSOURCE_ISSUE");
            cardEdge.setParentType("MATERIAL_STOCK");
            cardEdge.setParentId(stockId);
            cardEdge.setChildType("CARD");
            cardEdge.setChildId(cardId);
            cardEdge.setQuantity(t.getQuantity());
            cardEdge.setUnitOfMeasure(t.getUnitOfMeasure());
            cardEdge.setVendorId(order.getVendorId());
            cardEdge.setWorkorderId(order.getWorkorderId());
            cardEdge.setCardId(cardId);
            cardEdge.setProcessId(order.getProcessId());
            cardEdge.setTraceTime(t.getTraceTime() != null ? t.getTraceTime() : DateUtils.getNowDate());
            cardEdge.setCreateBy("system-cardId-backfill");
            cardEdge.setCreateTime(DateUtils.getNowDate());
            traceMapper.insertProMaterialTrace(cardEdge);
        }
    }

    /** 按工单 ID 查活跃流转卡（ACTIVE/OUTSOURCING），返回最新一张卡 ID；无卡返回 null（工单级生产属正常） */
    private Long resolveActiveCardId(Long workorderId)
    {
        if (workorderId == null) return null;
        ProCard query = new ProCard();
        query.setWorkorderId(workorderId);
        List<ProCard> cards = cardMapper.selectProCardList(query);
        if (cards == null) return null;
        return cards.stream()
                .filter(c -> STATUS_CARD_ACTIVE.equals(c.getStatus())
                        || STATUS_CARD_OUTSOURCING.equals(c.getStatus()))
                .map(ProCard::getCardId)
                .findFirst().orElse(null);
    }

    private static BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
}
