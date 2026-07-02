package com.ruoyi.system.service.mes.wm.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.core.redis.RedisLockTemplate;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.wm.WmRtIssueMapper;
import com.ruoyi.system.mapper.mes.wm.WmRtIssueLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmIssueHeaderMapper;
import com.ruoyi.system.mapper.mes.wm.WmIssueLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmMaterialStockMapper;
import com.ruoyi.system.mapper.mes.wm.WmTransactionMapper;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;
import com.ruoyi.system.domain.mes.wm.WmIssueLine;
import com.ruoyi.system.domain.mes.wm.WmMaterialStock;
import com.ruoyi.system.domain.mes.wm.WmTransaction;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.service.mes.wm.IWmRtIssueService;

/**
 * WmRtIssueService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class WmRtIssueServiceImpl implements IWmRtIssueService
{
    @Autowired
    private WmRtIssueMapper wmRtIssueMapper;

    @Autowired
    private WmRtIssueLineMapper wmRtIssueLineMapper;

    @Autowired
    private WmIssueHeaderMapper wmIssueHeaderMapper;

    @Autowired
    private WmIssueLineMapper wmIssueLineMapper;

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
    public WmRtIssue selectWmRtIssueByRtId(Long rtId) { return wmRtIssueMapper.selectWmRtIssueByRtId(rtId); }

    @Override
    public List<WmRtIssue> selectWmRtIssueList(WmRtIssue e) { return wmRtIssueMapper.selectWmRtIssueList(e); }

    @Override
    public List<WmRtIssue> selectAll() { return wmRtIssueMapper.selectWmRtIssueList(new WmRtIssue()); }

    @Override
    @Transactional
    public int insertWmRtIssue(WmRtIssue e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        if (e.getStatus() == null) e.setStatus("DRAFT");
        return wmRtIssueMapper.insertWmRtIssue(e);
    }

    @Override
    public int updateWmRtIssue(WmRtIssue e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return wmRtIssueMapper.updateWmRtIssue(e);
    }

    @Override
    public int deleteWmRtIssueByRtIds(Long[] rtIds) { return wmRtIssueMapper.deleteWmRtIssueByRtIds(rtIds); }

    @Override
    public int deleteWmRtIssueByRtId(Long rtId) { return wmRtIssueMapper.deleteWmRtIssueByRtId(rtId); }

    // ── 4a. 从领料单创建退料单 ──

    @Override
    @Transactional
    public Long createFromIssue(Long issueId) {
        // 加载领料单头
        WmIssueHeader issueHeader = wmIssueHeaderMapper.selectWmIssueHeaderByIssueId(issueId);
        if (issueHeader == null) throw new ServiceException("领料单不存在");

        // 加载领料单行
        WmIssueLine lineQuery = new WmIssueLine();
        lineQuery.setIssueId(issueId);
        List<WmIssueLine> issueLines = wmIssueLineMapper.selectWmIssueLineList(lineQuery);
        if (issueLines == null || issueLines.isEmpty()) throw new ServiceException("领料单无明细行");

        // 创建退料单头
        WmRtIssue rt = new WmRtIssue();
        rt.setIssueId(issueId);
        rt.setIssueCode(issueHeader.getIssueCode());
        rt.setWorkorderId(issueHeader.getWorkorderId());
        rt.setWorkorderCode(issueHeader.getWorkorderCode());
        rt.setWorkorderName(issueHeader.getWorkorderName());
        rt.setWorkstationId(issueHeader.getWorkstationId());
        rt.setWorkstationCode(issueHeader.getWorkstationCode());
        rt.setWorkstationName(issueHeader.getWorkstationName());
        rt.setWarehouseId(issueHeader.getWarehouseId());
        rt.setWarehouseCode(issueHeader.getWarehouseCode());
        rt.setWarehouseName(issueHeader.getWarehouseName());
        rt.setRtDate(new Date());
        rt.setRtCode("RT" + System.currentTimeMillis());
        rt.setStatus("DRAFT");
        rt.setCreateTime(DateUtils.getNowDate());
        rt.setCreateBy(SecurityUtils.getUsername());
        wmRtIssueMapper.insertWmRtIssue(rt);

        // 复制领料单行 → 退料单行
        BigDecimal totalQty = BigDecimal.ZERO;
        for (WmIssueLine issueLine : issueLines) {
            WmRtIssueLine rtLine = new WmRtIssueLine();
            rtLine.setRtId(rt.getRtId());
            rtLine.setRtCode(rt.getRtCode());
            rtLine.setIssueId(issueId);
            rtLine.setIssueLineId(issueLine.getLineId());
            rtLine.setItemId(issueLine.getItemId());
            rtLine.setItemCode(issueLine.getItemCode());
            rtLine.setItemName(issueLine.getItemName());
            rtLine.setItemSpc(issueLine.getItemSpc());
            rtLine.setUnitOfMeasure(issueLine.getUnitOfMeasure());
            rtLine.setUnitName(issueLine.getUnitName());
            rtLine.setQuantityRt(issueLine.getQuantityIssue());
            rtLine.setQuantityRted(BigDecimal.ZERO);
            rtLine.setBatchId(issueLine.getBatchId());
            rtLine.setBatchCode(issueLine.getBatchCode());
            rtLine.setWarehouseId(issueLine.getWarehouseId());
            rtLine.setLocationId(issueLine.getLocationId());
            rtLine.setAreaId(issueLine.getAreaId());
            rtLine.setCreateTime(DateUtils.getNowDate());
            rtLine.setCreateBy(SecurityUtils.getUsername());
            wmRtIssueLineMapper.insertWmRtIssueLine(rtLine);
            totalQty = totalQty.add(issueLine.getQuantityIssue() != null ? issueLine.getQuantityIssue() : BigDecimal.ZERO);
        }

        // 更新退料单总数
        rt.setQuantityTotal(totalQty);
        rt.setUpdateTime(DateUtils.getNowDate());
        rt.setUpdateBy(SecurityUtils.getUsername());
        wmRtIssueMapper.updateWmRtIssue(rt);

        return rt.getRtId();
    }

    // ── 4b. 执行退库：Redis 锁 + TransactionTemplate ──

    @Override
    public int executeReturn(Long rtId) {
        lockTemplate.execute("wm:rt:lock:" + rtId, 10,
                () -> txTemplate.execute(status -> doExecuteReturn(rtId)));
        return 1;
    }

    private Long doExecuteReturn(Long rtId) {
        // 1. 加载 header + lines
        WmRtIssue header = wmRtIssueMapper.selectWmRtIssueByRtId(rtId);
        if (header == null) throw new ServiceException("退料单不存在");
        if ("POSTED".equals(header.getStatus())) throw new ServiceException("退料单已执行，不能重复执行");

        WmRtIssueLine lineQuery = new WmRtIssueLine();
        lineQuery.setRtId(rtId);
        List<WmRtIssueLine> lines = wmRtIssueLineMapper.selectWmRtIssueLineList(lineQuery);
        if (lines == null || lines.isEmpty()) throw new ServiceException("退料单无明细行");

        for (WmRtIssueLine line : lines) {
            // 2. 查库存 (itemId + batchId + warehouseId + vendorId + workorderId + qualityStatus)
            WmMaterialStock stockQuery = new WmMaterialStock();
            stockQuery.setItemId(line.getItemId());
            stockQuery.setBatchId(line.getBatchId() != null ? line.getBatchId() : 0L);
            stockQuery.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            stockQuery.setVendorId(0L);
            stockQuery.setWorkorderId(header.getWorkorderId() != null ? header.getWorkorderId() : 0L);
            stockQuery.setQualityStatus("NORMAL");

            WmMaterialStock existing = wmMaterialStockMapper.loadMaterialStockForUpdate(stockQuery);
            BigDecimal delta = line.getQuantityRt();

            if (existing != null) {
                // 库存存在 → 增加现有量
                existing.setQuantityOnhand(existing.getQuantityOnhand().add(delta));
                existing.setUpdateTime(new Date());
                wmMaterialStockMapper.updateWmMaterialStock(existing);
            } else {
                // 库存不存在 → 新建库存记录
                stockQuery.setQuantityOnhand(delta);
                stockQuery.setCreateTime(new Date());
                stockQuery.setUpdateTime(new Date());
                stockQuery.setItemCode(line.getItemCode());
                stockQuery.setItemName(line.getItemName());
                stockQuery.setSpecification(line.getItemSpc());
                stockQuery.setUnitOfMeasure(line.getUnitOfMeasure());
                stockQuery.setUnitName(line.getUnitName());
                stockQuery.setQualityStatus("NORMAL");
                stockQuery.setWorkorderId(header.getWorkorderId());
                stockQuery.setWorkorderCode(header.getWorkorderCode());
                wmMaterialStockMapper.insertWmMaterialStock(stockQuery);
                existing = stockQuery;
            }

            // 3. 写库存事务 (RETURN_IN)
            WmTransaction tx = new WmTransaction();
            tx.setTransactionType("RETURN_IN");
            tx.setSourceDocType("RTISSUE");
            tx.setSourceDocId(rtId);
            tx.setSourceDocCode(header.getRtCode());
            tx.setSourceLineId(line.getLineId());
            tx.setMaterialStockId(existing.getMaterialStockId());
            tx.setItemId(line.getItemId());
            tx.setItemCode(line.getItemCode());
            tx.setItemName(line.getItemName());
            tx.setSpecification(line.getItemSpc());
            tx.setUnitOfMeasure(line.getUnitOfMeasure());
            tx.setUnitName(line.getUnitName());
            tx.setQuantity(delta);
            tx.setBatchId(line.getBatchId());
            tx.setBatchCode(line.getBatchCode());
            tx.setWarehouseId(line.getWarehouseId() != null ? line.getWarehouseId() : header.getWarehouseId());
            tx.setWorkorderId(header.getWorkorderId());
            tx.setWorkorderCode(header.getWorkorderCode());
            tx.setTransactionTime(new Date());
            tx.setCreateTime(DateUtils.getNowDate());
            tx.setCreateBy(SecurityUtils.getUsername());
            wmTransactionMapper.insertWmTransaction(tx);

            // 4. 写物料追溯 (RETURN)
            ProMaterialTrace trace = new ProMaterialTrace();
            trace.setTraceType("RETURN");
            trace.setParentType("CARD");
            trace.setParentId(0L);
            trace.setChildType("MATERIAL_STOCK");
            trace.setChildId(existing.getMaterialStockId());
            trace.setQuantity(line.getQuantityRt());
            trace.setUnitOfMeasure(line.getUnitOfMeasure());
            trace.setWorkorderId(header.getWorkorderId());
            trace.setTransactionId(tx.getTransactionId());
            trace.setProcessId(null);
            trace.setTraceTime(new Date());
            trace.setCreateTime(DateUtils.getNowDate());
            trace.setCreateBy(SecurityUtils.getUsername());
            proMaterialTraceMapper.insertProMaterialTrace(trace);
        }

        // 5. 更新 header 状态
        header.setStatus("POSTED");
        header.setUpdateTime(DateUtils.getNowDate());
        header.setUpdateBy(SecurityUtils.getUsername());
        wmRtIssueMapper.updateWmRtIssue(header);

        return rtId;
    }
}
