package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
import com.ruoyi.system.domain.mes.pro.ProWorkorder;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderMapper;
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
    private ProCardMapper proCardMapper;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    @Autowired
    private ProWorkorderMapper proWorkorderMapper;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WmIssueHeaderServiceImpl.class);

    @PostConstruct
    void initTx() {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    /**
     * 解析领料对应的流转卡ID：
     * 领料单填了 card_id → 直接用；否则按工单查活跃卡取第一张。
     */
    private Long resolveCardId(WmIssueHeader header) {
        if (header.getCardId() != null && header.getCardId() > 0) return header.getCardId();
        if (header.getWorkorderId() == null) return null;
        try {
            ProCard q = new ProCard();
            q.setWorkorderId(header.getWorkorderId());
            q.setStatus("ACTIVE");
            List<ProCard> cards = proCardMapper.selectProCardList(q);
            return (cards != null && !cards.isEmpty()) ? cards.get(0).getCardId() : null;
        } catch (Exception e) {
            log.warn("领料-流转卡解析失败, workorderId={}", header.getWorkorderId(), e);
            return null;
        }
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
        // 仓库必填（领料必须有发料仓库定位，否则后续预占/发料无法定位库存）
        if (e.getWarehouseId() == null) {
            throw new ServiceException("领料仓库不能为空");
        }
        // issueCode 为空时自动生成（编码规则 ISSUE_CODE：ISS+yyyyMMdd+3位流水）
        if (StringUtils.isEmpty(e.getIssueCode())) {
            e.setIssueCode(autoCodeGenerator.genSerialCode(WmIssueConstants.CODE_RULE_ISSUE, ""));
        }
        // 查重：生产领料同工单+同工序任务(task)不允许存在多张非终态领料单
        //   - 一个工单允许按 taskId(工序) 拆多张领料单（generateIssueDocuments 按 processId 分组循环创建）
        //   - taskId 为空时（手工整单模式），退化为按工单维度防重
        if (e.getWorkorderId() != null && WmIssueConstants.TYPE_PRODUCE.equals(e.getIssueType())) {
            WmIssueHeader q = new WmIssueHeader();
            q.setWorkorderId(e.getWorkorderId());
            q.setIssueType(WmIssueConstants.TYPE_PRODUCE);
            if (e.getTaskId() != null) {
                q.setTaskId(e.getTaskId());
            }
            List<WmIssueHeader> existings = wmIssueHeaderMapper.selectWmIssueHeaderList(q);
            if (existings != null) {
                for (WmIssueHeader ex : existings) {
                    if (WmIssueConstants.isTerminal(ex.getStatus())) continue;
                    // taskId 为空时上面已按 workorderId 全表匹配；不为空时 SQL 已限定，此处无需再过滤
                    if (e.getTaskId() == null && ex.getTaskId() != null) {
                        // 新单是"整单模式"（无 task）,已存在的是"按工序拆分单"→ 视为不冲突
                        continue;
                    }
                    throw new ServiceException("工单[" + e.getWorkorderCode()
                            + "]已有进行中的领料单[" + ex.getIssueCode() + "]，请勿重复生成");
                }
            }
        }
        wmIssueHeaderMapper.insertWmIssueHeader(e);
        // 头行一次性原子落库（修复旧版"先存头后存行中途出错留脏单"问题）
        if (e.getLines() != null && !e.getLines().isEmpty()) {
            saveIssueLines(e, e.getLines());
        }
        return 1;
    }

    @Override
    public int updateWmIssueHeader(WmIssueHeader e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        // 携带 lines 表示全量替换明细（修复旧版"编辑删行不生效"问题）
        if (e.getLines() != null) {
            WmIssueHeader header = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(e.getIssueId());
            if (header != null && !WmIssueConstants.isEditable(header.getStatus())) {
                throw new ServiceException("领料单[" + header.getIssueCode() + "]状态["
                        + header.getStatus() + "]不可修改明细");
            }
            wmIssueLineMapper.deleteWmIssueLineByIssueId(e.getIssueId());
        }
        wmIssueHeaderMapper.updateWmIssueHeader(e);
        if (e.getLines() != null) {
            saveIssueLines(e, e.getLines());
        }
        return 1;
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

    // ── 2a-bis. 从工单生成领料单草稿（不落库，前端编辑后走 insert）──

    @Override
    public WmIssueHeader buildFromWorkorder(Long workorderId)
    {
        ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(workorderId);
        if (wo == null) throw new ServiceException("工单不存在");
        String woStatus = wo.getStatus();
        if (!"PREPARE".equals(woStatus) && !"PRODUCING".equals(woStatus)) {
            throw new ServiceException("工单状态[" + woStatus + "]不可生成领料单，仅待生产/生产中可生成");
        }

        ProWorkorderBom query = new ProWorkorderBom();
        query.setWorkorderId(workorderId);
        List<ProWorkorderBom> bomList = proWorkorderBomMapper.selectProWorkorderBomList(query);
        if (bomList == null || bomList.isEmpty()) throw new ServiceException("工单BOM为空，请先维护工单BOM");

        List<WmIssueLine> lines = new ArrayList<>();
        for (ProWorkorderBom bom : bomList) {
            WmIssueLine line = new WmIssueLine();
            line.setItemId(bom.getItemId());
            line.setItemCode(bom.getItemCode());
            line.setItemName(bom.getItemName());
            line.setUnitOfMeasure(bom.getUnitOfMeasure());
            line.setUnitName(bom.getUnitName());
            line.setProcessId(bom.getProcessId());
            line.setQuantityIssue(bom.getTotalQuantity() != null ? bom.getTotalQuantity() : BigDecimal.ZERO);
            lines.add(line);
        }

        WmIssueHeader draft = new WmIssueHeader();
        draft.setWorkorderId(wo.getWorkorderId());
        draft.setWorkorderCode(wo.getWorkorderCode());
        draft.setWorkorderName(wo.getWorkorderName());
        draft.setIssueType(WmIssueConstants.TYPE_PRODUCE);
        draft.setIssueName(wo.getWorkorderCode() + "-领料单");
        draft.setIssueDate(DateUtils.getNowDate());
        draft.setStatus(WmIssueConstants.STATUS_DRAFT);
        draft.setLines(lines);
        return draft;
    }

    /**
     * 公共：批量保存领料明细行（insert/update 复用）。
     * 回填 issueId/issueCode/warehouseId，累加 quantityTotal 到头。
     */
    private void saveIssueLines(WmIssueHeader header, List<WmIssueLine> lines)
    {
        if (lines == null || lines.isEmpty()) return;
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmIssueLine line : lines) {
            line.setIssueId(header.getIssueId());
            line.setIssueCode(header.getIssueCode());
            if (line.getWarehouseId() == null) line.setWarehouseId(header.getWarehouseId());
            line.setCreateTime(DateUtils.getNowDate());
            line.setCreateBy(SecurityUtils.getUsername());
            wmIssueLineMapper.insertWmIssueLine(line);
            BigDecimal qty = line.getQuantityIssue() != null ? line.getQuantityIssue() : BigDecimal.ZERO;
            totalQty = totalQty.add(qty);
        }
        header.setQuantityTotal(totalQty);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmIssueHeaderMapper.updateWmIssueHeader(header);
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

            Long preferWh = line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId();
            if (line.getBatchId() != null) {
                // 指定批次：走 6 字段精确匹配
                WmMaterialStock existing = loadStockForUpdate(line.getItemId(), line.getBatchId(), preferWh);
                if (existing == null) {
                    throw new ServiceException("物料[" + line.getItemCode() + "]库存记录不存在，无法预占");
                }
                BigDecimal avail = existing.getQuantityAvailable() != null ? existing.getQuantityAvailable() : BigDecimal.ZERO;
                if (avail.compareTo(delta) < 0) {
                    throw new ServiceException("物料[" + line.getItemCode() + "]可用库存不足！可用：" + avail + "，需预占：" + delta);
                }
                deductAvailable(existing, delta);
                writeIssueTransaction(line, header, delta.negate(), WmIssueConstants.TX_ALLOCATE, existing);
            } else {
                // 未指定批次：FIFO 自动分配（仓库优先，不足跨仓）
                allocateAvailableFifo(line, header, delta, preferWh);
            }
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
            // 按 materialStockId 计算当前「净预占」(ALLOCATE - ISSUE_OUT - RELEASE)：
            // 只归还仍未发料的预占部分，保证 release 幂等——重复 release 时净预占已为 0，不再重复归还 available
            Map<Long, BigDecimal> netAlloc = computeNetAllocation(issueId, line.getLineId());
            for (Map.Entry<Long, BigDecimal> e : netAlloc.entrySet()) {
                WmMaterialStock existing = wmMaterialStockMapper.selectWmMaterialStockByMaterialStockId(e.getKey());
                if (existing == null) continue; // 库存记录已不存在，跳过

                BigDecimal releaseQty = e.getValue(); // 净预占量（正数）
                existing.setQuantityAvailable((existing.getQuantityAvailable() != null ? existing.getQuantityAvailable() : BigDecimal.ZERO).add(releaseQty));
                existing.setUpdateTime(new Date());
                wmMaterialStockMapper.updateWmMaterialStock(existing);

                // 写释放事务（正数 = 释放），batch/warehouse 取实际库存记录
                WmTransaction tx = new WmTransaction();
                tx.setTransactionType(WmIssueConstants.TX_RELEASE);
                tx.setSourceDocType(WmIssueConstants.SOURCE_ISSUE);
                tx.setSourceDocId(issueId);
                tx.setSourceDocCode(header.getIssueCode());
                tx.setSourceLineId(line.getLineId());
                tx.setMaterialStockId(e.getKey());
                tx.setItemId(line.getItemId());
                tx.setItemCode(line.getItemCode());
                tx.setItemName(line.getItemName());
                tx.setSpecification(line.getItemSpc());
                tx.setUnitOfMeasure(line.getUnitOfMeasure());
                tx.setUnitName(line.getUnitName());
                tx.setQuantity(releaseQty);
                tx.setBatchId(existing.getBatchId());
                tx.setBatchCode(existing.getBatchCode());
                tx.setWarehouseId(existing.getWarehouseId());
                tx.setWorkorderId(header.getWorkorderId());
                tx.setWorkorderCode(header.getWorkorderCode());
                tx.setTransactionTime(new Date());
                tx.setCreateTime(DateUtils.getNowDate());
                tx.setCreateBy(SecurityUtils.getUsername());
                wmTransactionMapper.insertWmTransaction(tx);
            }
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
            BigDecimal delta = line.getQuantityIssue() != null ? line.getQuantityIssue() : BigDecimal.ZERO;
            if (delta.compareTo(BigDecimal.ZERO) <= 0) continue; // 跳过零量行

            // 计算「净预占」= ALLOCATE + RELEASE + 已 ISSUE_OUT（均带符号），
            // 按 materialStockId 汇总，避免 confirm/release 反复操作导致重复扣减。
            Map<Long, BigDecimal> netByStock = computeNetAllocation(issueId, line.getLineId());
            if (netByStock.isEmpty()) {
                // 未走过预占（如历史 CONFIRMED 直达），fallback 到当前批次精确扣减
                Long wh = line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId();
                WmMaterialStock existing = loadStockForUpdate(line.getItemId(), line.getBatchId(), wh);
                if (existing == null) {
                    throw new ServiceException("物料[" + line.getItemCode() + "]库存记录不存在，无法出库");
                }
                deductOnhandAndWriteIssueOut(line, header, delta, existing);
                continue;
            }
            for (Map.Entry<Long, BigDecimal> e : netByStock.entrySet()) {
                BigDecimal batchQty = e.getValue();
                if (batchQty.compareTo(BigDecimal.ZERO) <= 0) continue;
                WmMaterialStock existing = wmMaterialStockMapper.selectWmMaterialStockByMaterialStockId(e.getKey());
                if (existing == null) {
                    throw new ServiceException("物料[" + line.getItemCode() + "]库存记录不存在，无法出库");
                }
                deductOnhandAndWriteIssueOut(line, header, batchQty, existing);
            }
        }

        // 5. 更新 header 状态
        header.setStatus(WmIssueConstants.STATUS_ISSUED);
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmIssueHeaderMapper.updateWmIssueHeader(header);

        // 外协发料：写 OUTSOURCE_ISSUE trace（原料→供应商）
        if ("OUTSOURCE".equals(header.getIssueType()) && header.getVendorId() != null) {
            for (WmIssueLine line : lines) {
                try {
                    writeOutsourceIssueTrace(header, line);
                } catch (Exception e) {
                    log.error("外协发料追溯写入失败, issueId={}, lineId={}", issueId, line.getLineId(), e);
                }
            }
        }

        return issueId;
    }

    /**
     * 写入外协发料 OUTSOURCE_ISSUE 追溯（原料发给供应商加工）。
     * parent = MATERIAL_STOCK（具体扣减的库存行），child = VENDOR（供应商）。
     * 按本 line 已写入的 ISSUE_OUT 事务逐 stockId 拆分，与真实扣减记录一一对应，避免 parentId=0 造成断链。
     */
    private void writeOutsourceIssueTrace(WmIssueHeader header, WmIssueLine line) {
        // 汇总本 line 所有 ISSUE_OUT 的量（qty 为负），按 materialStockId 分组
        WmTransaction q = new WmTransaction();
        q.setSourceDocType(WmIssueConstants.SOURCE_ISSUE);
        q.setSourceDocId(header.getIssueId());
        q.setSourceLineId(line.getLineId());
        q.setTransactionType(WmIssueConstants.TX_ISSUE_OUT);
        List<WmTransaction> txs = wmTransactionMapper.selectWmTransactionList(q);
        Map<Long, BigDecimal> byStock = new HashMap<>();
        for (WmTransaction tx : txs) {
            if (tx.getMaterialStockId() == null || tx.getQuantity() == null) continue;
            byStock.merge(tx.getMaterialStockId(), tx.getQuantity().abs(), BigDecimal::add);
        }
        if (byStock.isEmpty()) {
            log.warn("外协发料追溯：line {} 未找到 ISSUE_OUT 事务，跳过 trace 写入", line.getLineId());
            return;
        }
        for (Map.Entry<Long, BigDecimal> e : byStock.entrySet()) {
            ProMaterialTrace trace = new ProMaterialTrace();
            trace.setTraceType("OUTSOURCE_ISSUE");
            trace.setParentType("MATERIAL_STOCK");
            trace.setParentId(e.getKey());
            trace.setChildType("VENDOR");
            trace.setChildId(header.getVendorId());
            trace.setQuantity(e.getValue());
            trace.setUnitOfMeasure(line.getUnitOfMeasure());
            trace.setWorkorderId(header.getWorkorderId());
            trace.setIssueId(header.getIssueId());
            trace.setVendorId(header.getVendorId());
            trace.setProcessId(line.getProcessId());
            trace.setTraceTime(DateUtils.getNowDate());
            trace.setCreateTime(DateUtils.getNowDate());
            trace.setCreateBy(SecurityUtils.getUsername());
            proMaterialTraceMapper.insertProMaterialTrace(trace);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // Phase 2：完整生命周期方法（submit/approve/reject/issueOut/close/cancel）
    // ════════════════════════════════════════════════════════════════

    /**
     * 批量执行通用骨架：逐张调用单条动作，尽力执行，失败收集到 failures 列表。
     * 单条方法各自加锁/事务/幂等，单张失败不影响其他张。
     * @param action 单条流转动作（submitForApprove / approve / confirmIssue 方法引用）
     * @return total/successCount/failedCount/failures[{issueId,issueCode,issueName,reason}]
     */
    private Map<String, Object> executeBatch(Long[] issueIds, java.util.function.Consumer<Long> action) {
        if (issueIds == null || issueIds.length == 0) {
            throw new ServiceException("未选择领料单");
        }
        int success = 0;
        List<Map<String, Object>> failures = new ArrayList<>();
        for (Long id : issueIds) {
            try {
                action.accept(id);
                success++;
            } catch (Exception e) {
                // ServiceException 是预期业务失败（状态不符/库存不足），仅收集原因；
                // 非 ServiceException 属于系统异常，需 warn 级日志留痕便于排查
                if (!(e instanceof ServiceException)) {
                    log.warn("批量流转失败(非业务异常), issueId={}", id, e);
                }
                WmIssueHeader h = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(id);
                Map<String, Object> f = new HashMap<>();
                f.put("issueId", id);
                f.put("issueCode", h != null ? h.getIssueCode() : null);
                f.put("issueName", h != null ? h.getIssueName() : null);
                // message 可能为 null（如 NPE），给前端友好兜底
                String msg = e.getMessage();
                f.put("reason", (msg != null && !msg.isEmpty()) ? msg : e.getClass().getSimpleName());
                failures.add(f);
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("total", issueIds.length);
        r.put("successCount", success);
        r.put("failedCount", failures.size());
        r.put("failures", failures);
        return r;
    }

    @Override
    public Map<String, Object> batchSubmitForApprove(Long[] issueIds) {
        return executeBatch(issueIds, this::submitForApprove);
    }

    @Override
    public Map<String, Object> batchApprove(Long[] issueIds) {
        return executeBatch(issueIds, this::approve);
    }

    @Override
    public Map<String, Object> batchConfirmIssue(Long[] issueIds) {
        return executeBatch(issueIds, this::confirmIssue);
    }

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

        // 发料出库前校验工单必须已开工：未开工则无流转卡，投料 trace 会断链
        // 正确流程：开工（建卡）→ 预占 → 发料出库（物料挂载到流转卡）
        if (header.getWorkorderId() != null) {
            ProWorkorder wo = proWorkorderMapper.selectProWorkorderByWorkorderId(header.getWorkorderId());
            if (wo != null && !"PRODUCING".equals(wo.getStatus())) {
                throw new ServiceException("工单[" + wo.getWorkorderCode() + "]尚未开工（当前状态：" + wo.getStatus()
                        + "），请先在工单管理页面点击「开工」后再发料出库");
            }
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

            WmIssueLine line = lineMap.get(d.getLineId());

            if (d.getBatchId() != null) {
                // 指定批次发料：优先用 materialStockId 精确定位（前端批次下拉选中即带此 id，不受 vendor_id 约束）；
                // 无 materialStockId 时按 6 字段（item+batch+wh+vendor=0+workorder=0+quality=NORMAL）无锁探测。
                // 这里只做只读探测（校验存在/物料一致），行锁统一在下面按 stockId 升序 for update，避免死锁。
                WmMaterialStock probe;
                if (d.getMaterialStockId() != null) {
                    probe = wmMaterialStockMapper.selectWmMaterialStockByMaterialStockId(d.getMaterialStockId());
                } else {
                    Long wh = d.getWarehouseId() != null ? d.getWarehouseId()
                            : (line != null && line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
                    // 无 stockId 时按 6 字段做只读探测（不加锁），拿到 stockId 后统一升序重锁
                    WmMaterialStock qs = new WmMaterialStock();
                    qs.setItemId(d.getItemId());
                    qs.setBatchId(d.getBatchId() != null ? d.getBatchId() : 0L);
                    qs.setWarehouseId(wh);
                    qs.setVendorId(0L);
                    qs.setWorkorderId(0L);
                    qs.setQualityStatus(WmIssueConstants.QUALITY_NORMAL);
                    probe = wmMaterialStockMapper.loadMaterialStock(qs);
                }
                if (probe == null) {
                    throw new ServiceException("物料[" + d.getItemCode() + "]批次[" + d.getBatchCode() + "]库存记录不存在，无法发料");
                }
                // 校验物料一致（防误传 materialStockId）
                if (!probe.getItemId().equals(d.getItemId())) {
                    throw new ServiceException("物料[" + d.getItemCode() + "]所选批次记录与物料不符，无法发料");
                }
                // 回填 detail 的实际批次信息（按命中记录）
                d.setBatchId(probe.getBatchId());
                d.setBatchCode(probe.getBatchCode());
                d.setWarehouseId(probe.getWarehouseId());
                // 本批次已被预占的量（净预占），本次发料中由已有预占覆盖的部分无需释放/重占
                Map<Long, BigDecimal> netAlloc = computeNetAllocation(issueId, d.getLineId());
                BigDecimal newBatchAllocated = netAlloc.getOrDefault(probe.getMaterialStockId(), BigDecimal.ZERO);
                BigDecimal coveredByExistingAlloc = newBatchAllocated.min(qty);
                // 需要从旧批次释放并占用到新批次的量
                BigDecimal toSwap = qty.subtract(coveredByExistingAlloc);

                // ★ 统一按 stockId 升序对本次涉及的所有库存加行锁（含新批次+需释放的旧批次），杜绝 A↔B 交换类死锁
                Set<Long> stockIdsToLock = new TreeSet<>();
                stockIdsToLock.add(probe.getMaterialStockId());
                if (toSwap.compareTo(BigDecimal.ZERO) > 0) {
                    for (Long sid : netAlloc.keySet()) {
                        if (!sid.equals(probe.getMaterialStockId())) stockIdsToLock.add(sid);
                    }
                }
                Map<Long, WmMaterialStock> lockedStocks = new HashMap<>();
                for (Long sid : stockIdsToLock) {
                    WmMaterialStock locked = wmMaterialStockMapper.selectMaterialStockForUpdateById(sid);
                    if (locked != null) lockedStocks.put(sid, locked);
                }
                WmMaterialStock existing = lockedStocks.get(probe.getMaterialStockId());
                if (existing == null) {
                    throw new ServiceException("物料[" + d.getItemCode() + "]批次[" + d.getBatchCode() + "]库存记录锁定失败");
                }

                if (toSwap.compareTo(BigDecimal.ZERO) > 0) {
                    // 1. 精确释放旧批次的 toSwap 量（跳过本批次，行已在上面按升序锁完，直接扣减写事务即可）
                    BigDecimal unreleased = toSwap;
                    if (line != null) {
                        unreleased = releaseFromLockedStocks(header, line, toSwap, netAlloc,
                                existing.getMaterialStockId(), lockedStocks);
                    }
                    if (unreleased.compareTo(BigDecimal.ZERO) > 0) {
                        throw new ServiceException("物料[" + d.getItemCode() + "]预占不足以支持指定批次["
                                + d.getBatchCode() + "]发料 " + qty + "，仍需从旧批次释放 " + unreleased
                                + "（可能预占已被消费或未分配，请先补预占或按预占发料）");
                    }
                    // 2. 占用本批次 available（校验可用量充足）
                    BigDecimal newAvail = existing.getQuantityAvailable() != null ? existing.getQuantityAvailable() : BigDecimal.ZERO;
                    if (newAvail.compareTo(toSwap) < 0) {
                        throw new ServiceException("物料[" + d.getItemCode() + "]批次[" + d.getBatchCode()
                                + "]可用库存不足！可用：" + newAvail + "，需占用：" + toSwap);
                    }
                    deductAvailable(existing, toSwap);
                    if (line != null) {
                        writeIssueTransaction(line, header, toSwap.negate(), WmIssueConstants.TX_ALLOCATE, existing);
                    }
                }
                // 3. 扣 onhand 发料
                issueOutSingleBatch(header, d, qty, existing);
            } else {
                // 未指定批次：按预占记录扣 onhand（预占哪个批次就发哪个，不重新 FIFO）
                // 净预占 = ALLOCATE + RELEASE + 已 ISSUE_OUT，按 materialStockId 汇总
                Map<Long, BigDecimal> netAlloc = computeNetAllocation(issueId, d.getLineId());
                if (netAlloc.isEmpty()) {
                    throw new ServiceException("物料[" + d.getItemCode() + "]未预占库存，无法发料，请先预占");
                }
                // 按预占批次 FIFO 顺序扣减，直到满足本次发料量
                BigDecimal remaining = issueOutFromAllocation(header, d, qty, netAlloc);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    throw new ServiceException("物料[" + d.getItemCode() + "]预占库存不足！本次需：" + qty);
                }
            }
            issuedThisTime = issuedThisTime.add(qty);

            // 累加行的 quantityIssued
            if (line != null) {
                BigDecimal issued = line.getQuantityIssued() != null ? line.getQuantityIssued() : BigDecimal.ZERO;
                line.setQuantityIssued(issued.add(qty));
                line.setUpdateTime(DateUtils.getNowDate());
                line.setUpdateBy(SecurityUtils.getUsername());
                wmIssueLineMapper.updateWmIssueLine(line);
            }
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

    // ════════════════════════════════════════════════════════════════
    // FIFO 库存分配公共方法（confirm/issueOut 复用）
    // ════════════════════════════════════════════════════════════════

    /** 按 itemId+batchId+warehouseId 精确匹配查库存并锁定（vendor=0/workorder=0/quality=NORMAL） */
    private WmMaterialStock loadStockForUpdate(Long itemId, Long batchId, Long warehouseId) {
        WmMaterialStock q = new WmMaterialStock();
        q.setItemId(itemId);
        q.setBatchId(batchId != null ? batchId : 0L);
        q.setWarehouseId(warehouseId);
        q.setVendorId(0L);
        q.setWorkorderId(0L);
        q.setQualityStatus(WmIssueConstants.QUALITY_NORMAL);
        return wmMaterialStockMapper.loadMaterialStockForUpdate(q);
    }

    /** 扣减可用库存（预占），现有库存不动；不足抛异常由调用方前置检查 */
    private void deductAvailable(WmMaterialStock stock, BigDecimal delta) {
        BigDecimal avail = stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : BigDecimal.ZERO;
        BigDecimal newAvailable = avail.subtract(delta);
        stock.setQuantityAvailable(newAvailable.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newAvailable);
        stock.setUpdateTime(new Date());
        wmMaterialStockMapper.updateWmMaterialStock(stock);
    }

    /**
     * FIFO 预占可用库存：仓库优先（preferWh），不足自动跨仓；按 create_time 升序（先进先出）
     * 扣减 quantityAvailable，每个批次写一条 ALLOCATE 事务。
     */
    private void allocateAvailableFifo(WmIssueLine line, WmIssueHeader header, BigDecimal need, Long preferWh) {
        BigDecimal remaining = allocateAvailableFrom(line, header, need, preferWh);
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // 指定仓不足，跨仓补
            remaining = allocateAvailableFrom(line, header, remaining, null);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new ServiceException("物料[" + line.getItemCode() + "]可用库存不足！仍需：" + remaining);
        }
    }

    /** 从指定仓库（warehouseId=null 表示所有仓）FIFO 扣减 available，返回未满足的剩余量 */
    private BigDecimal allocateAvailableFrom(WmIssueLine line, WmIssueHeader header, BigDecimal need, Long warehouseId) {
        if (need.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        List<WmMaterialStock> stocks = wmMaterialStockMapper.selectAvailableStocksForFifo(
                line.getItemId(), warehouseId, WmIssueConstants.QUALITY_NORMAL);
        BigDecimal remaining = need;
        for (WmMaterialStock stock : stocks) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal avail = stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : BigDecimal.ZERO;
            if (avail.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal take = avail.compareTo(remaining) >= 0 ? remaining : avail;
            deductAvailable(stock, take);
            writeIssueTransaction(line, header, take.negate(), WmIssueConstants.TX_ALLOCATE, stock);
            remaining = remaining.subtract(take);
        }
        return remaining;
    }

    /** 写领料库存事务（预占/释放/出库通用），batch/warehouse/materialStockId 取实际库存记录的值 */
    private void writeIssueTransaction(WmIssueLine line, WmIssueHeader header, BigDecimal signedQty, String txType, WmMaterialStock stock) {
        WmTransaction tx = new WmTransaction();
        tx.setTransactionType(txType);
        tx.setSourceDocType(WmIssueConstants.SOURCE_ISSUE);
        tx.setSourceDocId(header.getIssueId());
        tx.setSourceDocCode(header.getIssueCode());
        tx.setSourceLineId(line.getLineId());
        tx.setMaterialStockId(stock.getMaterialStockId());
        tx.setItemId(line.getItemId());
        tx.setItemCode(line.getItemCode());
        tx.setItemName(line.getItemName());
        tx.setSpecification(line.getItemSpc());
        tx.setUnitOfMeasure(line.getUnitOfMeasure());
        tx.setUnitName(line.getUnitName());
        tx.setQuantity(signedQty);
        tx.setBatchId(stock.getBatchId());
        tx.setBatchCode(stock.getBatchCode());
        tx.setWarehouseId(stock.getWarehouseId());
        tx.setWorkorderId(header.getWorkorderId());
        tx.setWorkorderCode(header.getWorkorderCode());
        tx.setTransactionTime(new Date());
        tx.setCreateTime(DateUtils.getNowDate());
        tx.setCreateBy(SecurityUtils.getUsername());
        wmTransactionMapper.insertWmTransaction(tx);
    }

    /**
     * 计算领料单某行的「净预占」——按 materialStockId 汇总
     * ALLOCATE + RELEASE + ISSUE_OUT（三者均带符号：ALLOCATE/ISSUE_OUT 负、RELEASE 正），
     * 取反后为正数表示当前仍需出库的预占量。支持 confirm/release 反复操作后正确计算。
     */
    private Map<Long, BigDecimal> computeNetAllocation(Long issueId, Long lineId) {
        // 拉取该行全部相关事务一次性汇总
        WmTransaction q = new WmTransaction();
        q.setSourceDocType(WmIssueConstants.SOURCE_ISSUE);
        q.setSourceDocId(issueId);
        q.setSourceLineId(lineId);
        List<WmTransaction> all = wmTransactionMapper.selectWmTransactionList(q);
        Map<Long, BigDecimal> net = new HashMap<>();
        for (WmTransaction tx : all) {
            if (tx.getMaterialStockId() == null || tx.getQuantity() == null) continue;
            BigDecimal qty = tx.getQuantity(); // 带符号
            net.merge(tx.getMaterialStockId(), qty, BigDecimal::add);
        }
        // 净预占为负（被占用的量），取反为正（需出库量）；移除非正项
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map.Entry<Long, BigDecimal> e : net.entrySet()) {
            BigDecimal need = e.getValue().negate();
            if (need.compareTo(BigDecimal.ZERO) > 0) result.put(e.getKey(), need);
        }
        return result;
    }

    /**
     * 从已锁定的库存映射中精确释放指定量的旧预占（发料指定批次时，先把旧批次的等量预占归还再占用新批次）。
     * 跳过 excludeStockId（新发料批次）；按 materialStockId 升序遍历（与 lockedStocks 加锁顺序一致），
     * 逐批次归还 available 并写 RELEASE 事务。锁在调用方一次性完成，本方法只负责扣减+事务。
     * 返回未能释放的余量（≥0）：调用方可将其作为"追加占用"（超过原预占的新占用）继续处理。
     */
    private BigDecimal releaseFromLockedStocks(WmIssueHeader header, WmIssueLine line, BigDecimal need,
                                                Map<Long, BigDecimal> netAlloc, Long excludeStockId,
                                                Map<Long, WmMaterialStock> lockedStocks) {
        BigDecimal remaining = need;
        List<Long> stockIds = new ArrayList<>(netAlloc.keySet());
        Collections.sort(stockIds);
        for (Long stockId : stockIds) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            if (stockId.equals(excludeStockId)) continue;
            BigDecimal allocQty = netAlloc.get(stockId);
            if (allocQty == null || allocQty.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal releaseQty = allocQty.compareTo(remaining) <= 0 ? allocQty : remaining;
            WmMaterialStock stock = lockedStocks.get(stockId);
            if (stock == null) continue;  // 未锁到（如已被删除），跳过
            BigDecimal oldAvail = stock.getQuantityAvailable() != null ? stock.getQuantityAvailable() : BigDecimal.ZERO;
            stock.setQuantityAvailable(oldAvail.add(releaseQty));
            stock.setUpdateTime(new Date());
            wmMaterialStockMapper.updateWmMaterialStock(stock);
            writeIssueTransaction(line, header, releaseQty, WmIssueConstants.TX_RELEASE, stock);
            remaining = remaining.subtract(releaseQty);
        }
        return remaining;
    }

    /** 出库后钳制 available ≤ onhand：消费预占时不变（min 取原值），无预占/超发时随 onhand 下降 */
    private static BigDecimal clampAvailableToOnhand(BigDecimal oldAvailable, BigDecimal newOnhand) {
        BigDecimal avail = oldAvailable != null ? oldAvailable : BigDecimal.ZERO;
        BigDecimal onh = newOnhand != null ? newOnhand : BigDecimal.ZERO;
        return avail.min(onh);
    }

    /** 出库：扣 quantityOnhand + 钳制 available，写 ISSUE_OUT 事务 + 物料追溯 */
    private void deductOnhandAndWriteIssueOut(WmIssueLine line, WmIssueHeader header, BigDecimal qty, WmMaterialStock stock) {
        BigDecimal result = stock.getQuantityOnhand() != null ? stock.getQuantityOnhand().subtract(qty) : qty.negate();
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("物料[" + line.getItemCode() + "]库存不足！当前库存：" + stock.getQuantityOnhand() + "，需出库：" + qty);
        }
        stock.setQuantityOnhand(result);
        stock.setQuantityAvailable(clampAvailableToOnhand(stock.getQuantityAvailable(), result));
        stock.setUpdateTime(new Date());
        wmMaterialStockMapper.updateWmMaterialStock(stock);

        writeIssueTransaction(line, header, qty.negate(), WmIssueConstants.TX_ISSUE_OUT, stock);

        ProMaterialTrace trace = new ProMaterialTrace();
        Long cardId = resolveCardId(header);
        trace.setTraceType("ISSUE");
        trace.setParentType("MATERIAL_STOCK");
        trace.setParentId(stock.getMaterialStockId());
        // 无流转卡的工单（工单级生产），投料去向记为工单本身，避免无效的 CARD:0 断链
        if (cardId != null) {
            trace.setChildType("CARD");
            trace.setChildId(cardId);
        } else {
            trace.setChildType("WORKORDER");
            trace.setChildId(header.getWorkorderId() != null ? header.getWorkorderId() : 0L);
        }
        trace.setQuantity(qty);
        trace.setUnitOfMeasure(line.getUnitOfMeasure());
        trace.setWorkorderId(header.getWorkorderId());
        trace.setCardId(cardId);
        trace.setIssueId(header.getIssueId());
        trace.setIssueDetailId(line.getLineId());
        trace.setProcessId(line.getProcessId());
        trace.setTraceTime(new Date());
        trace.setCreateTime(DateUtils.getNowDate());
        trace.setCreateBy(SecurityUtils.getUsername());
        proMaterialTraceMapper.insertProMaterialTrace(trace);
    }

    /** 发料单批次出库：扣单条库存 onhand + 写 detail + 写 ISSUE_OUT 事务 + trace */
    private void issueOutSingleBatch(WmIssueHeader header, WmIssueDetail d, BigDecimal qty, WmMaterialStock stock) {
        BigDecimal newOnhand = (stock.getQuantityOnhand() != null ? stock.getQuantityOnhand() : BigDecimal.ZERO).subtract(qty);
        if (newOnhand.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("物料[" + d.getItemCode() + "]库存不足！当前：" + stock.getQuantityOnhand() + "，需发料：" + qty);
        }
        stock.setQuantityOnhand(newOnhand);
        stock.setQuantityAvailable(clampAvailableToOnhand(stock.getQuantityAvailable(), newOnhand));
        stock.setUpdateTime(new Date());
        wmMaterialStockMapper.updateWmMaterialStock(stock);

        // 回写 detail 的实际批次/仓库/库存记录
        d.setIssueId(header.getIssueId());
        d.setMaterialStockId(stock.getMaterialStockId());
        d.setBatchId(stock.getBatchId());
        d.setBatchCode(stock.getBatchCode());
        d.setWarehouseId(stock.getWarehouseId());
        d.setCreateTime(DateUtils.getNowDate());
        d.setCreateBy(SecurityUtils.getUsername());
        wmIssueDetailMapper.insertWmIssueDetail(d);

        writeIssueTransactionAndTrace(header, d, qty, stock);
    }

    /**
     * 按预占记录发料出库：从 netAlloc（净预占，materialStockId→预占量）逐条扣 onhand。
     * 预占哪个批次就发哪个批次，按 materialStockId 顺序（事务写入顺序）消费，每个写一条 detail。
     * 返回未满足的剩余量。
     */
    private BigDecimal issueOutFromAllocation(WmIssueHeader header, WmIssueDetail d, BigDecimal need, Map<Long, BigDecimal> netAlloc) {
        BigDecimal remaining = need;
        for (Map.Entry<Long, BigDecimal> e : netAlloc.entrySet()) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal allocQty = e.getValue(); // 该批次净预占量（正数）
            if (allocQty.compareTo(BigDecimal.ZERO) <= 0) continue;
            WmMaterialStock stock = wmMaterialStockMapper.selectWmMaterialStockByMaterialStockId(e.getKey());
            if (stock == null) continue;
            // 本次从该批次发的量 = min(剩余需求, 净预占量, 实际 onhand)
            BigDecimal onhand = stock.getQuantityOnhand() != null ? stock.getQuantityOnhand() : BigDecimal.ZERO;
            BigDecimal take = allocQty.compareTo(remaining) <= 0 ? allocQty : remaining;
            if (take.compareTo(onhand) > 0) take = onhand;
            if (take.compareTo(BigDecimal.ZERO) <= 0) continue;
            issueOutSingleBatch(header, d, take, stock);
            remaining = remaining.subtract(take);
        }
        return remaining;
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
        Long cardId = resolveCardId(header);
        trace.setTraceType("ISSUE");
        trace.setParentType("MATERIAL_STOCK");
        trace.setParentId(stock.getMaterialStockId());
        // 无流转卡的工单（工单级生产），投料去向记为工单本身，避免无效的 CARD:0 断链
        if (cardId != null) {
            trace.setChildType("CARD");
            trace.setChildId(cardId);
        } else {
            trace.setChildType("WORKORDER");
            trace.setChildId(header.getWorkorderId() != null ? header.getWorkorderId() : 0L);
        }
        trace.setQuantity(qty);
        trace.setUnitOfMeasure(d.getUnitOfMeasure());
        trace.setWorkorderId(header.getWorkorderId());
        trace.setCardId(cardId);
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
