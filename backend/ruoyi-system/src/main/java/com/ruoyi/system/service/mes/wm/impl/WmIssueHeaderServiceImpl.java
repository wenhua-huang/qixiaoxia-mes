package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.common.enums.WmIssueConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.wm.WmIssueHeaderMapper;
import com.ruoyi.system.mapper.mes.wm.WmIssueLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmIssueDetailMapper;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmTransactionMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderBomMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IWmIssueHeaderService;

/**
 * WmIssueHeaderService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class WmIssueHeaderServiceImpl implements IWmIssueHeaderService
{
    @Autowired
    private WmIssueHeaderMapper wmIssueHeaderMapper;

    @Autowired
    private WmIssueLineMapper wmIssueLineMapper;

    @Autowired
    private WmIssueDetailMapper wmIssueDetailMapper;

    @Autowired
    private ProWorkorderBomMapper proWorkorderBomMapper;

    @Autowired
    private WmMaterialStockMapper wmMaterialStockMapper;

    @Autowired
    private WmTransactionMapper wmTransactionMapper;

    @Autowired
    private ProMaterialTraceMapper proMaterialTraceMapper;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    @Override
    public WmIssueHeader selectWmIssueHeaderByIssueId(Long issueId) { return wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId); }

    @Override
    public List<WmIssueHeader> selectWmIssueHeaderList(WmIssueHeader e) { return wmIssueHeaderMapper.selectWmIssueHeaderList(e); }

    @Override
    public List<WmIssueHeader> selectAll() { return wmIssueHeaderMapper.selectWmIssueHeaderList(new WmIssueHeader()); }

    @Override
    @Transactional
    public int insertWmIssueHeader(WmIssueHeader e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        if (e.getStatus() == null) e.setStatus(WmIssueConstants.STATUS_DRAFT);
        // issueCode 为空时自动生成（编码规则 ISSUE_CODE：ISS+yyyyMMdd+3位流水）
        if (StringUtils.isEmpty(e.getIssueCode())) {
            e.setIssueCode(autoCodeGenerator.genSerialCode(WmIssueConstants.CODE_RULE_ISSUE, ""));
        }
        return wmIssueHeaderMapper.insertWmIssueHeader(e);
    }

    @Override
    public int updateWmIssueHeader(WmIssueHeader e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueHeaderMapper.updateWmIssueHeader(e);
    }

    /**
     * 批量删除领料单：仅 DRAFT/PENDING 状态可删，防止误删已预占/已发料单据导致库存账实不符。
     * 同时级联删除领料行（明细表保留，作为历史发料记录）。
     */
    @Override
    @Transactional
    public int deleteWmIssueHeaderByIssueIds(Long[] issueIds) {
        if (issueIds == null || issueIds.length == 0) return 0;
        for (Long id : issueIds) {
            WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(id);
            if (header == null) continue;
            if (!WmIssueConstants.isEditable(header.getStatus())) {
                throw new ServiceException("领料单[" + header.getIssueCode() + "]状态为["
                        + header.getStatus() + "]，仅草稿/待审核状态可删除");
            }
            // 级联删除领料行
            wmIssueLineMapper.deleteWmIssueLineByIssueId(id);
        }
        return wmIssueHeaderMapper.deleteWmIssueHeaderByIssueIds(issueIds);
    }

    @Override
    public int deleteWmIssueHeaderByIssueId(Long issueId) { return wmIssueHeaderMapper.deleteWmIssueHeaderByIssueId(issueId); }

    // ── 2a. BOM → 领料行自动生成 ──

    @Override
    @Transactional
    public int loadBomLines(Long issueId, Long workorderId) {
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");

        ProWorkorderBom query = new ProWorkorderBom();
        query.setWorkorderId(workorderId);
        List<ProWorkorderBom> bomList = proWorkorderBomMapper.selectProWorkorderBomList(query);
        if (bomList == null || bomList.isEmpty()) throw new ServiceException("工单BOM为空，请先维护工单BOM");

        BigDecimal totalQty = BigDecimal.ZERO;
        for (ProWorkorderBom bom : bomList) {
            WmIssueLine line = new WmIssueLine();
            line.setIssueId(issueId);
            line.setItemId(bom.getItemId());
            line.setItemCode(bom.getItemCode());
            line.setItemName(bom.getItemName());
            line.setUnitOfMeasure(bom.getUnitOfMeasure());
            line.setUnitName(bom.getUnitName());
            line.setQuantityIssue(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO);
            line.setWarehouseId(header.getWarehouseId());
            line.setCreateTime(DateUtils.getNowDate());
            line.setCreateBy(SecurityUtils.getUsername());
            wmIssueLineMapper.insertWmIssueLine(line);
            totalQty = totalQty.add(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO);
        }

        header.setQuantityTotal(totalQty);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueHeaderMapper.updateWmIssueHeader(header);
    }

    // ── 2b. 确认领料单：Redis 锁 + TransactionTemplate，预占库存 ──

    @Override
    public int confirmIssue(Long issueId) {
        lockTemplate.execute("wm:issue:lock:" + issueId, 10,
                () -> txTemplate.execute(status -> doConfirmIssue(issueId)));
        return 1;
    }

    private Long doConfirmIssue(Long issueId) {
        // 1. 加载 header + lines
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        // 兼容两种入口：DRAFT 直达（旧 confirm）或 APPROVED 审核后（新 allocate）
        String st = header.getStatus();
        if (!WmIssueConstants.STATUS_DRAFT.equals(st) && !WmIssueConstants.STATUS_APPROVED.equals(st)
                && !"CONFIRMED".equals(st)) {
            throw new ServiceException("只有草稿/已下达状态的领料单才能预占，当前状态：" + st);
        }

        WmIssueLine lineQuery = new WmIssueLine();
        lineQuery.setIssueId(issueId);
        List<WmIssueLine> lines = wmIssueLineMapper.selectWmIssueLineList(lineQuery);
        if (lines == null || lines.isEmpty()) throw new ServiceException("领料单无明细行，请先添加物料后再确认");

        for (WmIssueLine line : lines) {
            // 2. 跳过零量行（先检查避免无效的库存查询）
            BigDecimal delta = line.getQuantityIssue() != null ? line.getQuantityIssue() : BigDecimal.ZERO;
            if (delta.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 3. 查库存并锁定行（SELECT FOR UPDATE，防止并发超分）
            WmMaterialStock stockQuery = new WmMaterialStock();
            stockQuery.setItemId(line.getItemId());
            stockQuery.setBatchId(line.getBatchId() != null ? line.getBatchId() : 0L);
            stockQuery.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            stockQuery.setVendorId(0L);
            stockQuery.setWorkorderId(0L);
            stockQuery.setQualityStatus(WmIssueConstants.QUALITY_NORMAL);

            WmMaterialStock existing = wmMaterialStockMapper.loadMaterialStockForUpdate(stockQuery);
            if (existing == null) {
                throw new ServiceException("物料[" + line.getItemCode() + "]库存记录不存在，无法预占");
            }

            // 检查可用库存是否充足
            BigDecimal avail = existing.getQuantityAvailable() != null ? existing.getQuantityAvailable() : BigDecimal.ZERO;
            if (avail.compareTo(delta) < 0) {
                throw new ServiceException("物料[" + line.getItemCode() + "]可用库存不足！可用：" + avail + "，需预占：" + delta);
            }

            // 扣减可用库存（预占），现有库存不动
            BigDecimal newAvailable = avail.subtract(delta);
            existing.setQuantityAvailable(newAvailable.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newAvailable);
            existing.setUpdateTime(new Date());
            wmMaterialStockMapper.updateWmMaterialStock(existing);

            // 3. 写库存事务 (ALLOCATE — 预占)
            WmTransaction tx = new WmTransaction();
            tx.setTransactionType(WmIssueConstants.TX_ALLOCATE);
            tx.setSourceDocType(WmIssueConstants.SOURCE_ISSUE);
            tx.setSourceDocId(issueId);
            tx.setSourceDocCode(header.getIssueCode());
            tx.setSourceLineId(line.getLineId());
            tx.setMaterialStockId(existing.getMaterialStockId());
            tx.setItemId(line.getItemId());
            tx.setItemCode(line.getItemCode());
            tx.setItemName(line.getItemName());
            tx.setSpecification(line.getItemSpc());
            tx.setUnitOfMeasure(line.getUnitOfMeasure());
            tx.setUnitName(line.getUnitName());
            tx.setQuantity(delta.negate());
            tx.setBatchId(line.getBatchId() != null ? line.getBatchId() : 0L);
            tx.setBatchCode(line.getBatchCode());
            tx.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            tx.setWorkorderId(header.getWorkorderId());
            tx.setWorkorderCode(header.getWorkorderCode());
            tx.setTransactionTime(new Date());
            tx.setCreateTime(DateUtils.getNowDate());
            tx.setCreateBy(SecurityUtils.getUsername());
            wmTransactionMapper.insertWmTransaction(tx);
        }

        // 4. 更新 header 状态
        header.setStatus(WmIssueConstants.STATUS_ALLOCATED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmIssueHeaderMapper.updateWmIssueHeader(header);

        return issueId;
    }

    // ── 2b2. 释放预占：CONFIRMED → DRAFT，恢复 quantityAvailable ──

    @Override
    public int releaseAllocation(Long issueId) {
        lockTemplate.execute("wm:issue:lock:" + issueId, 10,
                () -> txTemplate.execute(status -> doReleaseAllocation(issueId)));
        return 1;
    }

    private Long doReleaseAllocation(Long issueId) {
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        String st = header.getStatus();
        if (!WmIssueConstants.STATUS_ALLOCATED.equals(st) && !"CONFIRMED".equals(st)) {
            throw new ServiceException("只有已预占状态的领料单才能释放预占，当前状态：" + st);
        }

        WmIssueLine lineQuery = new WmIssueLine();
        lineQuery.setIssueId(issueId);
        List<WmIssueLine> lines = wmIssueLineMapper.selectWmIssueLineList(lineQuery);
        if (lines == null || lines.isEmpty()) return issueId;

        for (WmIssueLine line : lines) {
            WmMaterialStock stockQuery = new WmMaterialStock();
            stockQuery.setItemId(line.getItemId());
            stockQuery.setBatchId(line.getBatchId() != null ? line.getBatchId() : 0L);
            stockQuery.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            stockQuery.setVendorId(0L);
            stockQuery.setWorkorderId(0L);
            stockQuery.setQualityStatus(WmIssueConstants.QUALITY_NORMAL);

            WmMaterialStock existing = wmMaterialStockMapper.loadMaterialStockForUpdate(stockQuery);
            if (existing == null) continue; // 库存记录已不存在，跳过

            BigDecimal delta = line.getQuantityIssue() != null ? line.getQuantityIssue() : BigDecimal.ZERO;
            if (delta.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 恢复可用库存
            BigDecimal currentAvail = existing.getQuantityAvailable() != null ? existing.getQuantityAvailable() : BigDecimal.ZERO;
            existing.setQuantityAvailable(currentAvail.add(delta));
            existing.setUpdateTime(new Date());
            wmMaterialStockMapper.updateWmMaterialStock(existing);

            // 写释放事务记录
            WmTransaction tx = new WmTransaction();
            tx.setTransactionType(WmIssueConstants.TX_RELEASE);
            tx.setSourceDocType(WmIssueConstants.SOURCE_ISSUE);
            tx.setSourceDocId(issueId);
            tx.setSourceDocCode(header.getIssueCode());
            tx.setSourceLineId(line.getLineId());
            tx.setMaterialStockId(existing.getMaterialStockId());
            tx.setItemId(line.getItemId());
            tx.setItemCode(line.getItemCode());
            tx.setItemName(line.getItemName());
            tx.setSpecification(line.getItemSpc());
            tx.setUnitOfMeasure(line.getUnitOfMeasure());
            tx.setUnitName(line.getUnitName());
            tx.setQuantity(delta); // 正数 = 释放
            tx.setBatchId(line.getBatchId() != null ? line.getBatchId() : 0L);
            tx.setBatchCode(line.getBatchCode());
            tx.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            tx.setWorkorderId(header.getWorkorderId());
            tx.setWorkorderCode(header.getWorkorderCode());
            tx.setTransactionTime(new Date());
            tx.setCreateTime(DateUtils.getNowDate());
            tx.setCreateBy(SecurityUtils.getUsername());
            wmTransactionMapper.insertWmTransaction(tx);
        }

        // 恢复为已下达状态（释放预占后回到 APPROVED，仍可再次预占或作废）
        header.setStatus(WmIssueConstants.STATUS_APPROVED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmIssueHeaderMapper.updateWmIssueHeader(header);

        return issueId;
    }

    // ── 2c. 执行出库：Redis 锁 + TransactionTemplate ──

    @Override
    public int executeIssue(Long issueId) {
        lockTemplate.execute("wm:issue:lock:" + issueId, 10,
                () -> txTemplate.execute(status -> doExecuteIssue(issueId)));
        return 1;
    }

    private Long doExecuteIssue(Long issueId) {
        // 1. 加载 header + lines
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        String st = header.getStatus();
        if (WmIssueConstants.STATUS_ISSUED.equals(st)) throw new ServiceException("领料单已执行，不能重复执行");
        if (!WmIssueConstants.STATUS_ALLOCATED.equals(st) && !"CONFIRMED".equals(st)) {
            throw new ServiceException("只有已预占状态的领料单才能执行出库，当前状态：" + st);
        }

        WmIssueLine lineQuery = new WmIssueLine();
        lineQuery.setIssueId(issueId);
        List<WmIssueLine> lines = wmIssueLineMapper.selectWmIssueLineList(lineQuery);
        if (lines == null || lines.isEmpty()) throw new ServiceException("领料单无明细行，请先加载BOM或手动添加行");

        for (WmIssueLine line : lines) {
            // 2. 查库存 (itemId + batchId + warehouseId + vendorId + workorderId + qualityStatus)
            WmMaterialStock stockQuery = new WmMaterialStock();
            stockQuery.setItemId(line.getItemId());
            stockQuery.setBatchId(line.getBatchId() != null ? line.getBatchId() : 0L);
            stockQuery.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            stockQuery.setVendorId(0L);
            stockQuery.setWorkorderId(0L);
            stockQuery.setQualityStatus(WmIssueConstants.QUALITY_NORMAL);

            WmMaterialStock existing = wmMaterialStockMapper.loadMaterialStockForUpdate(stockQuery);
            if (existing == null) {
                throw new ServiceException("物料[" + line.getItemCode() + "]库存记录不存在，无法出库");
            }

            BigDecimal delta = line.getQuantityIssue() != null ? line.getQuantityIssue() : BigDecimal.ZERO;
            if (delta.compareTo(BigDecimal.ZERO) <= 0) continue; // 跳过零量行

            BigDecimal result = existing.getQuantityOnhand() != null ? existing.getQuantityOnhand().subtract(delta) : delta.negate();
            if (result.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("物料[" + line.getItemCode() + "]库存不足！当前库存：" + existing.getQuantityOnhand() + "，需出库：" + delta);
            }

            // 只更新 quantityOnhand（available 在 confirm 时已扣），置空 available 防止动态 UPDATE 误写
            existing.setQuantityOnhand(result);
            existing.setQuantityAvailable(null); // 不写回 available，避免覆盖并发 confirm 的写入
            existing.setUpdateTime(new Date());
            wmMaterialStockMapper.updateWmMaterialStock(existing);

            // 3. 写库存事务 (ISSUE_OUT)
            WmTransaction tx = new WmTransaction();
            tx.setTransactionType(WmIssueConstants.TX_ISSUE_OUT);
            tx.setSourceDocType(WmIssueConstants.SOURCE_ISSUE);
            tx.setSourceDocId(issueId);
            tx.setSourceDocCode(header.getIssueCode());
            tx.setSourceLineId(line.getLineId());
            tx.setMaterialStockId(existing.getMaterialStockId());
            tx.setItemId(line.getItemId());
            tx.setItemCode(line.getItemCode());
            tx.setItemName(line.getItemName());
            tx.setSpecification(line.getItemSpc());
            tx.setUnitOfMeasure(line.getUnitOfMeasure());
            tx.setUnitName(line.getUnitName());
            tx.setQuantity(delta.negate());
            tx.setBatchId(line.getBatchId() != null ? line.getBatchId() : 0L);
            tx.setBatchCode(line.getBatchCode());
            tx.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            tx.setWorkorderId(header.getWorkorderId());
            tx.setWorkorderCode(header.getWorkorderCode());
            tx.setTransactionTime(new Date());
            tx.setCreateTime(DateUtils.getNowDate());
            tx.setCreateBy(SecurityUtils.getUsername());
            wmTransactionMapper.insertWmTransaction(tx);

            // 4. 写物料追溯 (ISSUE)
            ProMaterialTrace trace = new ProMaterialTrace();
            trace.setTraceType("ISSUE");
            trace.setParentType("MATERIAL_STOCK");
            trace.setParentId(existing.getMaterialStockId());
            trace.setChildType("CARD");
            trace.setChildId(0L);
            trace.setQuantity(line.getQuantityIssue());
            trace.setUnitOfMeasure(line.getUnitOfMeasure());
            trace.setWorkorderId(header.getWorkorderId());
            trace.setIssueId(issueId);
            trace.setIssueDetailId(line.getLineId());
            trace.setTransactionId(tx.getTransactionId());
            trace.setProcessId(line.getProcessId());
            trace.setTraceTime(new Date());
            trace.setCreateTime(DateUtils.getNowDate());
            trace.setCreateBy(SecurityUtils.getUsername());
            proMaterialTraceMapper.insertProMaterialTrace(trace);
        }

        // 5. 更新 header 状态
        header.setStatus(WmIssueConstants.STATUS_ISSUED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmIssueHeaderMapper.updateWmIssueHeader(header);

        return issueId;
    }

    // ════════════════════════════════════════════════════════════════
    // Phase 2：完整生命周期方法（submit/approve/reject/issueOut/close/cancel）
    // ════════════════════════════════════════════════════════════════

    /** 提交审核：DRAFT → PENDING */
    @Override
    public int submitForApprove(Long issueId) {
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        if (!WmIssueConstants.STATUS_DRAFT.equals(header.getStatus())) {
            throw new ServiceException("只有草稿状态的领料单才能提交审核，当前状态：" + header.getStatus());
        }
        // 校验必须有明细行
        WmIssueLine lineQuery = new WmIssueLine();
        lineQuery.setIssueId(issueId);
        List<WmIssueLine> lines = wmIssueLineMapper.selectWmIssueLineList(lineQuery);
        if (lines == null || lines.isEmpty()) throw new ServiceException("领料单无明细行，请先添加物料后再提交");

        header.setStatus(WmIssueConstants.STATUS_PENDING);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueHeaderMapper.updateWmIssueHeader(header);
    }

    /** 审核通过：PENDING → APPROVED，记录审核人/审核时间 */
    @Override
    public int approve(Long issueId) {
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        if (!WmIssueConstants.STATUS_PENDING.equals(header.getStatus())) {
            throw new ServiceException("只有待审核状态的领料单才能审核，当前状态：" + header.getStatus());
        }
        header.setStatus(WmIssueConstants.STATUS_APPROVED);
        header.setApproveBy(SecurityUtils.getUsername());
        header.setApproveTime(DateUtils.getNowDate());
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueHeaderMapper.updateWmIssueHeader(header);
    }

    /** 审核退回：PENDING → DRAFT */
    @Override
    public int reject(Long issueId) {
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        if (!WmIssueConstants.STATUS_PENDING.equals(header.getStatus())) {
            throw new ServiceException("只有待审核状态的领料单才能退回，当前状态：" + header.getStatus());
        }
        header.setStatus(WmIssueConstants.STATUS_DRAFT);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueHeaderMapper.updateWmIssueHeader(header);
    }

    /** 关闭：ISSUED → CLOSED（收料确认/手工关闭，终态） */
    @Override
    public int close(Long issueId) {
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        String st = header.getStatus();
        if (WmIssueConstants.STATUS_CLOSED.equals(st)) return 1; // 已关闭幂等
        if (!WmIssueConstants.STATUS_ISSUED.equals(st) && !WmIssueConstants.STATUS_PARTIAL_ISSUED.equals(st)
                && !"POSTED".equals(st)) {
            throw new ServiceException("只有已发料状态的领料单才能关闭，当前状态：" + st);
        }
        header.setStatus(WmIssueConstants.STATUS_CLOSED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueHeaderMapper.updateWmIssueHeader(header);
    }

    /**
     * 发料出库（支持分批）：ALLOCATED/PARTIAL_ISSUED → PARTIAL_ISSUED/ISSUED。
     * 按 details 扣减 onhand、写 detail 明细、写 transaction 流水、写 trace 追溯。
     */
    @Override
    public int issueOut(Long issueId, List<WmIssueDetail> details) {
        if (details == null || details.isEmpty()) {
            throw new ServiceException("发料明细不能为空");
        }
        lockTemplate.execute("wm:issue:lock:" + issueId, 10,
                () -> txTemplate.execute(status -> doIssueOut(issueId, details)));
        return 1;
    }

    private Long doIssueOut(Long issueId, List<WmIssueDetail> details) {
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        String st = header.getStatus();
        // 仅 ALLOCATED 或 PARTIAL_ISSUED（继续发料）允许，兼容历史 CONFIRMED
        if (!WmIssueConstants.STATUS_ALLOCATED.equals(st) && !WmIssueConstants.STATUS_PARTIAL_ISSUED.equals(st)
                && !"CONFIRMED".equals(st)) {
            throw new ServiceException("只有已预占/部分发料状态的领料单才能发料，当前状态：" + st);
        }

        // 加载所有行，构建 lineId→line 映射，便于累加 quantityIssued
        WmIssueLine lineQuery = new WmIssueLine();
        lineQuery.setIssueId(issueId);
        List<WmIssueLine> lines = wmIssueLineMapper.selectWmIssueLineList(lineQuery);
        Map<Long, WmIssueLine> lineMap = new HashMap<>();
        for (WmIssueLine l : lines) lineMap.put(l.getLineId(), l);

        BigDecimal issuedThisTime = BigDecimal.ZERO;
        for (WmIssueDetail d : details) {
            BigDecimal qty = d.getQuantity() != null ? d.getQuantity() : BigDecimal.ZERO;
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 1. 扣减 onhand（FOR UPDATE 锁库存行）
            WmMaterialStock stockQuery = buildStockQuery(d, header);
            WmMaterialStock existing = wmMaterialStockMapper.loadMaterialStockForUpdate(stockQuery);
            if (existing == null) {
                throw new ServiceException("物料[" + d.getItemCode() + "]库存记录不存在，无法发料");
            }
            BigDecimal newOnhand = (existing.getQuantityOnhand() != null ? existing.getQuantityOnhand() : BigDecimal.ZERO).subtract(qty);
            if (newOnhand.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("物料[" + d.getItemCode() + "]库存不足！当前：" + existing.getQuantityOnhand() + "，需发料：" + qty);
            }
            existing.setQuantityOnhand(newOnhand);
            existing.setQuantityAvailable(null); // 不覆盖 available（已预占）
            existing.setUpdateTime(new Date());
            wmMaterialStockMapper.updateWmMaterialStock(existing);

            // 2. 写发料明细（materialStockId 关联）
            d.setIssueId(issueId);
            d.setMaterialStockId(existing.getMaterialStockId());
            d.setWarehouseId(d.getWarehouseId() != null ? d.getWarehouseId() : header.getWarehouseId());
            d.setCreateTime(DateUtils.getNowDate());
            d.setCreateBy(SecurityUtils.getUsername());
            wmIssueDetailMapper.insertWmIssueDetail(d);

            // 3. 写库存流水 ISSUE_OUT + 4. 物料追溯
            writeIssueTransactionAndTrace(header, d, qty, existing);

            // 5. 累加行的 quantityIssued
            WmIssueLine line = lineMap.get(d.getLineId());
            if (line != null) {
                BigDecimal issued = line.getQuantityIssued() != null ? line.getQuantityIssued() : BigDecimal.ZERO;
                line.setQuantityIssued(issued.add(qty));
                line.setUpdateTime(DateUtils.getNowDate());
                line.setUpdateBy(SecurityUtils.getUsername());
                wmIssueLineMapper.updateWmIssueLine(line);
            }
            issuedThisTime = issuedThisTime.add(qty);
        }

        // 6. 累加 header 的 quantityIssuedTotal，判断是否全发完
        BigDecimal totalIssued = (header.getQuantityIssuedTotal() != null ? header.getQuantityIssuedTotal() : BigDecimal.ZERO).add(issuedThisTime);
        header.setQuantityIssuedTotal(totalIssued);
        // 全发完（累计 >= 申请总量）转 ISSUED，否则 PARTIAL_ISSUED
        boolean allIssued = totalIssued.compareTo(header.getQuantityTotal() != null ? header.getQuantityTotal() : BigDecimal.ZERO) >= 0;
        header.setStatus(allIssued ? WmIssueConstants.STATUS_ISSUED : WmIssueConstants.STATUS_PARTIAL_ISSUED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmIssueHeaderMapper.updateWmIssueHeader(header);
        return issueId;
    }

    /** 构建库存定位查询（itemId + batchId + warehouseId + vendorId=0 + workorderId=0 + quality=NORMAL） */
    private WmMaterialStock buildStockQuery(WmIssueDetail d, WmIssueHeader header) {
        WmMaterialStock q = new WmMaterialStock();
        q.setItemId(d.getItemId());
        q.setBatchId(d.getBatchId() != null ? d.getBatchId() : 0L);
        q.setWarehouseId(d.getWarehouseId() != null ? d.getWarehouseId() : header.getWarehouseId());
        q.setVendorId(0L);
        q.setWorkorderId(0L);
        q.setQualityStatus(WmIssueConstants.QUALITY_NORMAL);
        return q;
    }

    /** 写库存流水 ISSUE_OUT + 物料追溯（领料出库） */
    private void writeIssueTransactionAndTrace(WmIssueHeader header, WmIssueDetail d, BigDecimal qty, WmMaterialStock stock) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(WmIssueConstants.TX_ISSUE_OUT);
        tx.setSourceDocType(WmIssueConstants.SOURCE_ISSUE);
        tx.setSourceDocId(header.getIssueId());
        tx.setSourceDocCode(header.getIssueCode());
        tx.setSourceLineId(d.getLineId());
        tx.setMaterialStockId(stock.getMaterialStockId());
        tx.setItemId(d.getItemId());
        tx.setItemCode(d.getItemCode());
        tx.setItemName(d.getItemName());
        tx.setUnitOfMeasure(d.getUnitOfMeasure());
        tx.setUnitName(d.getUnitName());
        tx.setQuantity(qty.negate());
        tx.setBatchId(d.getBatchId() != null ? d.getBatchId() : 0L);
        tx.setBatchCode(d.getBatchCode());
        tx.setWarehouseId(d.getWarehouseId());
        tx.setWorkorderId(header.getWorkorderId());
        tx.setWorkorderCode(header.getWorkorderCode());
        tx.setTransactionTime(new Date());
        tx.setCreateTime(DateUtils.getNowDate());
        tx.setCreateBy(SecurityUtils.getUsername());
        wmTransactionMapper.insertWmTransaction(tx);

        ProMaterialTrace trace = new ProMaterialTrace();
        trace.setTraceType("ISSUE");
        trace.setParentType("MATERIAL_STOCK");
        trace.setParentId(stock.getMaterialStockId());
        trace.setChildType("CARD");
        trace.setChildId(0L);
        trace.setQuantity(qty);
        trace.setUnitOfMeasure(d.getUnitOfMeasure());
        trace.setWorkorderId(header.getWorkorderId());
        trace.setIssueId(header.getIssueId());
        trace.setIssueDetailId(d.getLineId());
        trace.setTransactionId(tx.getTransactionId());
        trace.setTraceTime(new Date());
        trace.setCreateTime(DateUtils.getNowDate());
        trace.setCreateBy(SecurityUtils.getUsername());
        proMaterialTraceMapper.insertProMaterialTrace(trace);
    }

    /**
     * 作废：非终态 → CANCELED。
     * ALLOCATED 态作废需先恢复 available；ISSUED/PARTIAL_ISSUED 态不允许直接作废（已扣库存，需走退料）。
     */
    @Override
    public int cancel(Long issueId, String reason) {
        lockTemplate.execute("wm:issue:lock:" + issueId, 10,
                () -> txTemplate.execute(status -> doCancel(issueId, reason)));
        return 1;
    }

    private Long doCancel(Long issueId, String reason) {
        WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (header == null) throw new ServiceException("领料单不存在");
        String st = header.getStatus();
        if (WmIssueConstants.isTerminal(st)) {
            throw new ServiceException("领料单已是终态，不能作废，当前状态：" + st);
        }
        // 已发料（已扣 onhand）不允许直接作废，需走退料流程恢复库存
        if (WmIssueConstants.STATUS_ISSUED.equals(st) || WmIssueConstants.STATUS_PARTIAL_ISSUED.equals(st)
                || "POSTED".equals(st)) {
            throw new ServiceException("已发料的领料单不能直接作废，请通过退料流程恢复库存");
        }
        // ALLOCATED 态需先释放预占（恢复 available）
        if (WmIssueConstants.STATUS_ALLOCATED.equals(st) || "CONFIRMED".equals(st)) {
            doReleaseAllocation(issueId);
            // 重新加载（releaseAllocation 已把状态改为 APPROVED）
            header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        }
        header.setStatus(WmIssueConstants.STATUS_CANCELED);
        header.setCancelReason(reason);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmIssueHeaderMapper.updateWmIssueHeader(header);
        return issueId;
    }
}
