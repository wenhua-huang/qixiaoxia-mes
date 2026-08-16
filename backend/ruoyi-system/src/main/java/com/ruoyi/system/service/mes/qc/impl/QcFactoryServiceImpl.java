package com.ruoyi.system.service.mes.qc.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.domain.mes.qc.QcTemplateIndex;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOrderLineMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateIndexMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateProductMapper;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.QcCodeGenerator;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import jakarta.annotation.PostConstruct;

/**
 * 检验单生成工厂实现（IQC 部分已实现，OQC/IPQC/RQC 见各桩方法注释的交付任务）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@Service
public class QcFactoryServiceImpl implements IQcFactoryService
{
    @Autowired
    private QcTemplateProductMapper bindMapper;

    @Autowired
    private QcIqcMapper iqcMapper;

    @Autowired
    private QcOrderLineMapper lineMapper;

    @Autowired
    private QcTemplateIndexMapper templateIndexMapper;

    @Autowired
    private WmItemRecptMapper wmItemRecptMapper;

    @Autowired(required = false)
    private AutoCodeGenerator autoCodeGenerator;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx()
    {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    @Override
    public QcTemplateProduct resolveTemplate(String qcType, Long itemId, Long processId)
    {
        if (processId != null)
        {
            QcTemplateProduct bind = bindMapper.selectEnabledBindExact(qcType, itemId, processId);
            if (bind != null)
            {
                return bind;
            }
        }
        // processId=null 时 Exact(process_id = null) 匹配不到行，直接查通用绑定
        return bindMapper.selectEnabledBindCommon(qcType, itemId);
    }

    @Override
    public void generateIqcForItemRecpt(WmItemRecpt header, List<WmItemRecptLine> lines)
    {
        if (header == null || lines == null || lines.isEmpty())
        {
            return;
        }
        // 多行同物料合并为一组 → 一张检验单
        Map<Long, List<WmItemRecptLine>> byItem = lines.stream()
            .collect(Collectors.groupingBy(WmItemRecptLine::getItemId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<WmItemRecptLine>> e : byItem.entrySet())
        {
            QcTemplateProduct bind = resolveTemplate(QcConstants.TYPE_IQC, e.getKey(), null);
            if (bind == null)
            {
                continue;  // 未绑定模板 = 免检
            }
            generateOneIqc(header, e.getValue(), bind);
        }
    }

    /** 先锁后事务：锁防同单并发重入，事务保证 头/行/挂点 三写原子（防零行活动单卡死幂等检查） */
    private void generateOneIqc(WmItemRecpt header, List<WmItemRecptLine> group, QcTemplateProduct bind)
    {
        String lockKey = QcConstants.LOCK_GENERATE + "iqc:" + header.getRecptId() + ":" + group.get(0).getItemId();
        // 块状 void lambda 显式绑定 Runnable 重载（表达式 lambda 会歧义绑定到 Supplier 重载）
        lockTemplate.execute(lockKey, () -> {
            txTemplate.execute(tx -> {
                doGenerateOneIqc(header, group, bind);
                return null;
            });
        });
    }

    /** 锁+事务内生成单物料检验单：幂等检查 → 快照建单 → 建行 → 回填头挂点 */
    private void doGenerateOneIqc(WmItemRecpt header, List<WmItemRecptLine> group, QcTemplateProduct bind)
    {
        Long itemId = group.get(0).getItemId();
        List<QcIqc> exist = iqcMapper.selectBySource(QcConstants.SOURCE_ITEM_RECPT, header.getRecptId(), itemId);
        boolean hasActive = exist.stream().anyMatch(o -> !QcConstants.STATUS_CLOSED.equals(o.getStatus()));
        if (hasActive)
        {
            return;  // 幂等：同来源+物料已有未关闭单
        }
        QcIqc iqc = buildIqc(header, group, bind);
        iqcMapper.insertQcIqc(iqc);
        lineMapper.batchInsert(buildLinesFromTemplate(bind.getTemplateId(), QcConstants.TYPE_IQC, iqc.getIqcId()));
        backfillHeaderRefs(header, iqc);
    }

    /** 按绑定快照阈值构建 IQC 单头（factory_id 由拦截器注入，此处不设） */
    private QcIqc buildIqc(WmItemRecpt header, List<WmItemRecptLine> group, QcTemplateProduct bind)
    {
        WmItemRecptLine first = group.get(0);
        BigDecimal received = group.stream().map(WmItemRecptLine::getQuantityRecpt).map(this::nvl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Date now = new Date();
        QcIqc iqc = new QcIqc();
        iqc.setIqcCode(QcCodeGenerator.genIqcCode(autoCodeGenerator));
        iqc.setIqcName("来料检验-" + StringUtils.defaultString(first.getItemName()));
        iqc.setTemplateId(bind.getTemplateId());
        iqc.setSourceDocId(header.getRecptId());
        iqc.setSourceDocType(QcConstants.SOURCE_ITEM_RECPT);
        iqc.setSourceDocCode(header.getRecptCode());
        iqc.setVendorId(header.getVendorId());
        iqc.setVendorCode(header.getVendorCode());
        iqc.setVendorName(header.getVendorName());
        iqc.setItemId(first.getItemId());
        iqc.setItemCode(first.getItemCode());
        iqc.setItemName(first.getItemName());
        iqc.setSpecification(first.getSpecification());
        iqc.setUnitOfMeasure(first.getUnitOfMeasure());
        iqc.setQuantityReceived(received);
        iqc.setQuantityMinCheck(bind.getQuantityCheck());
        iqc.setQuantityMaxUnqualified(bind.getQuantityUnqualified());
        iqc.setCrRateLimit(bind.getCrRate());
        iqc.setMajRateLimit(bind.getMajRate());
        iqc.setMinRateLimit(bind.getMinRate());
        iqc.setReceiveDate(now);
        iqc.setCreateTime(now);
        iqc.setStatus(QcConstants.STATUS_PENDING);
        return iqc;
    }

    /** 回填入库单头 IQC 挂点（仅首张；两列专用 UPDATE 避免并发覆盖整头） */
    private void backfillHeaderRefs(WmItemRecpt header, QcIqc iqc)
    {
        if (header.getIqcId() != null)
        {
            return;
        }
        header.setIqcId(iqc.getIqcId());
        header.setIqcCode(iqc.getIqcCode());
        wmItemRecptMapper.updateWmItemRecptHeaderRefs(header.getRecptId(), iqc.getIqcId(), iqc.getIqcCode());
    }

    @Override
    public List<QcOrderLine> buildLinesFromTemplate(Long templateId, String qcType, Long qcId)
    {
        List<QcOrderLine> lines = new ArrayList<>();
        List<QcTemplateIndex> indexes = templateIndexMapper.selectByTemplateId(templateId);
        if (indexes == null)
        {
            return lines;
        }
        for (QcTemplateIndex idx : indexes)
        {
            lines.add(snapshotLine(idx, qcType, qcId));
        }
        return lines;
    }

    /** 模板检测项 → 检验行快照 */
    private QcOrderLine snapshotLine(QcTemplateIndex idx, String qcType, Long qcId)
    {
        QcOrderLine l = new QcOrderLine();
        l.setQcType(qcType);
        l.setQcId(qcId);
        l.setIndexId(idx.getIndexId());
        l.setIndexCode(idx.getIndexCode());
        l.setIndexName(idx.getIndexName());
        l.setIndexType(idx.getIndexType());
        l.setQcTool(idx.getQcTool());
        l.setQcResultType(idx.getQcResultType());
        l.setCheckMethod(idx.getCheckMethod());
        l.setStanderVal(idx.getStanderVal());
        l.setUnitOfMeasure(idx.getUnitOfMeasure());
        l.setThresholdMin(idx.getThresholdMin());
        l.setThresholdMax(idx.getThresholdMax());
        l.setOrderNum(idx.getOrderNum());
        return l;
    }

    @Override
    public void closeBySource(String sourceDocType, Long sourceDocId)
    {
        if (!QcConstants.SOURCE_ITEM_RECPT.equals(sourceDocType))
        {
            return;  // 其余来源类型（成品入库/销售出库/退料）由 Task 12/14/16 扩展
        }
        List<QcIqc> orders = iqcMapper.selectBySource(sourceDocType, sourceDocId, null);
        for (QcIqc order : orders)
        {
            closeIfActive(order);
        }
    }

    /** 仅 PENDING/INSPECTING 置 CLOSED；COMPLETED 是质量档案保留 */
    private void closeIfActive(QcIqc order)
    {
        boolean active = QcConstants.STATUS_PENDING.equals(order.getStatus())
            || QcConstants.STATUS_INSPECTING.equals(order.getStatus());
        if (!active)
        {
            return;
        }
        QcIqc update = new QcIqc();
        update.setIqcId(order.getIqcId());
        update.setStatus(QcConstants.STATUS_CLOSED);
        update.setUpdateTime(new Date());
        iqcMapper.updateQcIqc(update);
    }

    @Override
    public void generateOqcForProductSales(WmProductSales header, List<WmProductSalesLine> lines)
    {
        // 桩：OQC 单据 Mapper 在 Task 12 交付后实现
        throw new UnsupportedOperationException("generateOqcForProductSales 待 Task 12 实现");
    }

    @Override
    public void generateIpqcForProductRecpt(WmProductRecpt header)
    {
        // 桩：IPQC 单据 Mapper 在 Task 14 交付后实现
        throw new UnsupportedOperationException("generateIpqcForProductRecpt 待 Task 14 实现");
    }

    @Override
    public String generateIpqcForFeedback(ProFeedback feedback)
    {
        // 桩：IPQC 单据 Mapper 在 Task 14 交付后实现；返回 null=未生成
        throw new UnsupportedOperationException("generateIpqcForFeedback 待 Task 14 实现");
    }

    @Override
    public void generateRqcForRtIssue(WmRtIssue header, List<WmRtIssueLine> lines)
    {
        // 桩：RQC 单据 Mapper 在 Task 16 交付后实现
        throw new UnsupportedOperationException("generateRqcForRtIssue 待 Task 16 实现");
    }

    private BigDecimal nvl(BigDecimal v)
    {
        return v == null ? BigDecimal.ZERO : v;
    }
}
