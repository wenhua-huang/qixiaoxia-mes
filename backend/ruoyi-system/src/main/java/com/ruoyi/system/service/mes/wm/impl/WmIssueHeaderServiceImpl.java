package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
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
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmTransactionMapper;
import com.ruoyi.system.mapper.mes.pro.ProWorkorderBomMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.domain.mes.pro.ProWorkorderBom;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
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
    private ProWorkorderBomMapper proWorkorderBomMapper;

    @Autowired
    private WmMaterialStockMapper wmMaterialStockMapper;

    @Autowired
    private WmTransactionMapper wmTransactionMapper;

    @Autowired
    private ProMaterialTraceMapper proMaterialTraceMapper;

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
        if (e.getStatus() == null) e.setStatus("DRAFT");
        return wmIssueHeaderMapper.insertWmIssueHeader(e);
    }

    @Override
    public int updateWmIssueHeader(WmIssueHeader e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return wmIssueHeaderMapper.updateWmIssueHeader(e);
    }

    @Override
    public int deleteWmIssueHeaderByIssueIds(Long[] issueIds) { return wmIssueHeaderMapper.deleteWmIssueHeaderByIssueIds(issueIds); }

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
        if (!"DRAFT".equals(header.getStatus())) throw new ServiceException("只有草稿状态的领料单才能确认，当前状态：" + header.getStatus());

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
            stockQuery.setQualityStatus("NORMAL");

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
            tx.setTransactionType("ALLOCATE");
            tx.setSourceDocType("ISSUE");
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
        header.setStatus("CONFIRMED");
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
        if (!"CONFIRMED".equals(header.getStatus())) throw new ServiceException("只有已确认状态的领料单才能释放预占，当前状态：" + header.getStatus());

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
            stockQuery.setQualityStatus("NORMAL");

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
            tx.setTransactionType("RELEASE");
            tx.setSourceDocType("ISSUE");
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

        // 恢复为草稿状态
        header.setStatus("DRAFT");
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
        if ("POSTED".equals(header.getStatus())) throw new ServiceException("领料单已执行，不能重复执行");
        if (!"CONFIRMED".equals(header.getStatus())) throw new ServiceException("只有已确认状态的领料单才能执行出库，当前状态：" + header.getStatus());

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
            stockQuery.setQualityStatus("NORMAL");

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
            tx.setTransactionType("ISSUE_OUT");
            tx.setSourceDocType("ISSUE");
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
        header.setStatus("POSTED");
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmIssueHeaderMapper.updateWmIssueHeader(header);

        return issueId;
    }
}
