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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProCard;
import com.ruoyi.system.domain.mes.pro.ProCardProcess;
import com.ruoyi.system.domain.mes.pro.ProFeedback;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.domain.mes.qc.QcIpqc;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcOqc;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.domain.mes.qc.QcRqc;
import com.ruoyi.system.domain.mes.qc.QcTemplateIndex;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.domain.mes.wm.WmItemRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;
import com.ruoyi.system.domain.mes.wm.WmProductSales;
import com.ruoyi.system.domain.mes.wm.WmProductSalesLine;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmRtIssueLine;
import com.ruoyi.system.mapper.mes.pro.ProCardMapper;
import com.ruoyi.system.mapper.mes.pro.ProCardProcessMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.qc.QcIpqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcOrderLineMapper;
import com.ruoyi.system.mapper.mes.qc.QcRqcMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateIndexMapper;
import com.ruoyi.system.mapper.mes.qc.QcTemplateProductMapper;
import com.ruoyi.system.mapper.mes.wm.WmItemRecptMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptLineMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductRecptMapper;
import com.ruoyi.system.mapper.mes.wm.WmProductSalesMapper;
import com.ruoyi.system.mapper.mes.wm.WmRtIssueMapper;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.QcCodeGenerator;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.qc.QcTodoHelper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import jakarta.annotation.PostConstruct;

/**
 * 检验单生成工厂实现（IQC/IPQC/OQC/RQC 四类；生成成功后联动建 QC_CHECK 待办）
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
    private QcOqcMapper oqcMapper;

    @Autowired
    private WmProductSalesMapper wmProductSalesMapper;

    @Autowired
    private QcIpqcMapper ipqcMapper;

    @Autowired
    private ProRouteProcessMapper proRouteProcessMapper;

    @Autowired
    private ProCardProcessMapper proCardProcessMapper;

    @Autowired
    private ProCardMapper proCardMapper;

    @Autowired
    private WmProductRecptMapper wmProductRecptMapper;

    @Autowired
    private WmProductRecptLineMapper wmProductRecptLineMapper;

    @Autowired
    private QcOrderLineMapper lineMapper;

    @Autowired
    private QcTemplateIndexMapper templateIndexMapper;

    @Autowired
    private WmItemRecptMapper wmItemRecptMapper;

    @Autowired
    private QcRqcMapper rqcMapper;

    @Autowired
    private WmRtIssueMapper wmRtIssueMapper;

    @Autowired
    private QcTodoHelper qcTodoHelper;

    @Autowired(required = false)
    private AutoCodeGenerator autoCodeGenerator;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    /**
     * 独立新事务模板（REQUIRES_NEW）：专供报工触发的工序检生成等"弱拦截"链路。
     * confirmFeedback 以 try-catch 包裹生成调用并要求失败不阻断报工；若用 REQUIRED 加入外层
     * @Transactional，生成抛异常会把外层事务标记 rollback-only，即使被 catch 提交时仍抛
     * UnexpectedRollbackException，弱拦截失效。独立事务自管提交/回滚，与报工确认解耦。
     */
    private TransactionTemplate txTemplateRequiresNew;

    @PostConstruct
    void initTx()
    {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
        this.txTemplateRequiresNew = new TransactionTemplate(transactionManager);
        this.txTemplateRequiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.txTemplateRequiresNew.setTimeout(30);
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
        qcTodoHelper.createTodo(QcConstants.TYPE_IQC, iqc.getIqcId(), iqc.getIqcCode(), iqc.getItemName());
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
        if (QcConstants.SOURCE_ITEM_RECPT.equals(sourceDocType))
        {
            List<QcIqc> orders = iqcMapper.selectBySource(sourceDocType, sourceDocId, null);
            for (QcIqc order : orders)
            {
                closeIfActive(order);
            }
            return;
        }
        if (QcConstants.SOURCE_PRODUCT_SALES.equals(sourceDocType))
        {
            List<QcOqc> orders = oqcMapper.selectBySource(sourceDocType, sourceDocId, null);
            for (QcOqc order : orders)
            {
                closeIfActive(order);
            }
            return;
        }
        if (QcConstants.SOURCE_PRODUCT_RECPT.equals(sourceDocType))
        {
            List<QcIpqc> orders = ipqcMapper.selectBySource(sourceDocType, sourceDocId, null);
            for (QcIpqc order : orders)
            {
                closeIfActive(order);
            }
            return;
        }
        if (QcConstants.SOURCE_RT_ISSUE.equals(sourceDocType))
        {
            List<QcRqc> orders = rqcMapper.selectBySource(sourceDocType, sourceDocId, null);
            for (QcRqc order : orders)
            {
                closeIfActive(order);
            }
            return;
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

    /** OQC 版 closeIfActive：同 IQC 语义（仅 PENDING/INSPECTING 置 CLOSED） */
    private void closeIfActive(QcOqc order)
    {
        boolean active = QcConstants.STATUS_PENDING.equals(order.getStatus())
            || QcConstants.STATUS_INSPECTING.equals(order.getStatus());
        if (!active)
        {
            return;
        }
        QcOqc update = new QcOqc();
        update.setOqcId(order.getOqcId());
        update.setStatus(QcConstants.STATUS_CLOSED);
        update.setUpdateTime(new Date());
        oqcMapper.updateQcOqc(update);
    }

    /** IPQC 版 closeIfActive：同 IQC/OQC 语义（仅 PENDING/INSPECTING 置 CLOSED） */
    private void closeIfActive(QcIpqc order)
    {
        boolean active = QcConstants.STATUS_PENDING.equals(order.getStatus())
            || QcConstants.STATUS_INSPECTING.equals(order.getStatus());
        if (!active)
        {
            return;
        }
        QcIpqc update = new QcIpqc();
        update.setIpqcId(order.getIpqcId());
        update.setStatus(QcConstants.STATUS_CLOSED);
        update.setUpdateTime(new Date());
        ipqcMapper.updateQcIpqc(update);
    }

    /** RQC 版 closeIfActive：同 IQC/OQC/IPQC 语义（仅 PENDING/INSPECTING 置 CLOSED） */
    private void closeIfActive(QcRqc order)
    {
        boolean active = QcConstants.STATUS_PENDING.equals(order.getStatus())
            || QcConstants.STATUS_INSPECTING.equals(order.getStatus());
        if (!active)
        {
            return;
        }
        QcRqc update = new QcRqc();
        update.setRqcId(order.getRqcId());
        update.setStatus(QcConstants.STATUS_CLOSED);
        update.setUpdateTime(new Date());
        rqcMapper.updateQcRqc(update);
    }

    @Override
    public void generateOqcForProductSales(WmProductSales header, List<WmProductSalesLine> lines)
    {
        if (header == null || lines == null || lines.isEmpty())
        {
            return;
        }
        // 多行同物料合并为一组 → 一张检验单（与 IQC 同范式）
        Map<Long, List<WmProductSalesLine>> byItem = lines.stream()
            .filter(l -> l.getItemId() != null)
            .collect(Collectors.groupingBy(WmProductSalesLine::getItemId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<WmProductSalesLine>> e : byItem.entrySet())
        {
            QcTemplateProduct bind = resolveTemplate(QcConstants.TYPE_OQC, e.getKey(), null);
            if (bind == null)
            {
                continue;  // 未绑定模板 = 免检
            }
            generateOneOqc(header, e.getValue(), bind);
        }
    }

    /** 先锁后事务：锁防同单并发重入，事务保证 头/行/挂点 三写原子（防零行活动单卡死幂等检查） */
    private void generateOneOqc(WmProductSales header, List<WmProductSalesLine> group, QcTemplateProduct bind)
    {
        String lockKey = QcConstants.LOCK_GENERATE + "oqc:" + header.getSalesId() + ":" + group.get(0).getItemId();
        // 块状 void lambda 显式绑定 Runnable 重载（表达式 lambda 会歧义绑定到 Supplier 重载）
        lockTemplate.execute(lockKey, () -> {
            txTemplate.execute(tx -> {
                doGenerateOneOqc(header, group, bind);
                return null;
            });
        });
    }

    /** 锁+事务内生成单物料 OQC：幂等检查 → 快照建单 → 建行 → 回填头挂点 */
    private void doGenerateOneOqc(WmProductSales header, List<WmProductSalesLine> group, QcTemplateProduct bind)
    {
        Long itemId = group.get(0).getItemId();
        List<QcOqc> exist = oqcMapper.selectBySource(QcConstants.SOURCE_PRODUCT_SALES, header.getSalesId(), itemId);
        boolean hasActive = exist.stream().anyMatch(o -> !QcConstants.STATUS_CLOSED.equals(o.getStatus()));
        if (hasActive)
        {
            return;  // 幂等：同来源+物料已有未关闭单
        }
        QcOqc oqc = buildOqc(header, group, bind);
        oqcMapper.insertQcOqc(oqc);
        lineMapper.batchInsert(buildLinesFromTemplate(bind.getTemplateId(), QcConstants.TYPE_OQC, oqc.getOqcId()));
        backfillSalesHeaderRefs(header, oqc);
        qcTodoHelper.createTodo(QcConstants.TYPE_OQC, oqc.getOqcId(), oqc.getOqcCode(), oqc.getItemName());
    }

    /** 按绑定快照阈值构建 OQC 单头（客户三件套取出库单头，quantity_out=行数量和） */
    private QcOqc buildOqc(WmProductSales header, List<WmProductSalesLine> group, QcTemplateProduct bind)
    {
        WmProductSalesLine first = group.get(0);
        BigDecimal quantityOut = group.stream().map(WmProductSalesLine::getQuantitySales).map(this::nvl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Date now = new Date();
        QcOqc oqc = new QcOqc();
        oqc.setOqcCode(QcCodeGenerator.genOqcCode(autoCodeGenerator));
        oqc.setOqcName("出货检验-" + StringUtils.defaultString(first.getItemName()));
        oqc.setTemplateId(bind.getTemplateId());
        oqc.setSourceDocId(header.getSalesId());
        oqc.setSourceDocType(QcConstants.SOURCE_PRODUCT_SALES);
        oqc.setSourceDocCode(header.getSalesCode());
        oqc.setClientId(header.getClientId());
        oqc.setClientCode(header.getClientCode());
        oqc.setClientName(header.getClientName());
        oqc.setItemId(first.getItemId());
        oqc.setItemCode(first.getItemCode());
        oqc.setItemName(first.getItemName());
        oqc.setSpecification(first.getSpecification());
        oqc.setUnitOfMeasure(first.getUnitOfMeasure());
        oqc.setQuantityOut(quantityOut);
        oqc.setQuantityMinCheck(bind.getQuantityCheck());
        oqc.setQuantityMaxUnqualified(bind.getQuantityUnqualified());
        oqc.setCrRateLimit(bind.getCrRate());
        oqc.setMajRateLimit(bind.getMajRate());
        oqc.setMinRateLimit(bind.getMinRate());
        oqc.setOutDate(now);
        oqc.setCreateTime(now);
        oqc.setStatus(QcConstants.STATUS_PENDING);
        return oqc;
    }

    /** 回填出库单头 OQC 挂点（仅首张；两列专用 UPDATE 避免并发覆盖整头） */
    private void backfillSalesHeaderRefs(WmProductSales header, QcOqc oqc)
    {
        if (header.getOqcId() != null)
        {
            return;
        }
        header.setOqcId(oqc.getOqcId());
        header.setOqcCode(oqc.getOqcCode());
        wmProductSalesMapper.updateSalesHeaderRefs(header.getSalesId(), oqc.getOqcId(), oqc.getOqcCode());
    }

    @Override
    public void generateIpqcForProductRecpt(WmProductRecpt header)
    {
        if (header == null || header.getRecptId() == null || header.getProduceId() == null)
        {
            return;
        }
        QcTemplateProduct bind = resolveTemplate(QcConstants.TYPE_IPQC, header.getProduceId(), null);
        if (bind == null)
        {
            return;  // 产品未绑定 IPQC 模板 = 免检
        }
        String lockKey = QcConstants.LOCK_GENERATE + "ipqc:pr:" + header.getRecptId();
        // 块状 void lambda 显式绑定 Runnable 重载（表达式 lambda 会歧义绑定到 Supplier 重载）
        lockTemplate.execute(lockKey, () -> {
            txTemplate.execute(tx -> {
                doGenerateRecptIpqc(header, bind);
                return null;
            });
        });
    }

    /** 锁+事务内生成完工检 IPQC：幂等检查 → 快照建单（物料信息取首行）→ 建行 → 回填入库单头挂点 */
    private void doGenerateRecptIpqc(WmProductRecpt header, QcTemplateProduct bind)
    {
        List<QcIpqc> exist = ipqcMapper.selectBySource(
            QcConstants.SOURCE_PRODUCT_RECPT, header.getRecptId(), header.getProduceId());
        if (exist.stream().anyMatch(o -> !QcConstants.STATUS_CLOSED.equals(o.getStatus())))
        {
            return;  // 幂等：同入库单+产品已有未关闭单
        }
        WmProductRecptLine first = firstRecptLine(header.getRecptId());
        String itemName = first != null && first.getItemName() != null ? first.getItemName() : header.getProduceCode();
        Date now = new Date();
        QcIpqc ipqc = new QcIpqc();
        ipqc.setIpqcCode(QcCodeGenerator.genIpqcCode(autoCodeGenerator));
        ipqc.setIpqcName("完工检-" + StringUtils.defaultString(itemName));
        ipqc.setIpqcType(QcConstants.IPQC_LAST);
        ipqc.setTemplateId(bind.getTemplateId());
        ipqc.setSourceDocId(header.getRecptId());
        ipqc.setSourceDocType(QcConstants.SOURCE_PRODUCT_RECPT);
        ipqc.setSourceDocCode(header.getRecptCode());
        ipqc.setWorkorderId(header.getWorkorderId());
        ipqc.setWorkorderCode(header.getWorkorderCode());
        ipqc.setItemId(header.getProduceId());
        ipqc.setItemCode(first != null ? first.getItemCode() : header.getProduceCode());
        ipqc.setItemName(itemName);
        ipqc.setSpecification(first != null ? first.getSpecification() : null);
        ipqc.setUnitOfMeasure(first != null ? first.getUnitOfMeasure() : null);
        snapshotBind(ipqc, bind);
        ipqc.setStatus(QcConstants.STATUS_PENDING);
        ipqc.setCreateTime(now);
        ipqcMapper.insertQcIpqc(ipqc);
        lineMapper.batchInsert(buildLinesFromTemplate(bind.getTemplateId(), QcConstants.TYPE_IPQC, ipqc.getIpqcId()));
        if (header.getIpqcId() == null)
        {
            header.setIpqcId(ipqc.getIpqcId());
            header.setIpqcCode(ipqc.getIpqcCode());
            wmProductRecptMapper.updateProductRecptHeaderRefs(header.getRecptId(), ipqc.getIpqcId(), ipqc.getIpqcCode());
        }
        qcTodoHelper.createTodo(QcConstants.TYPE_IPQC, ipqc.getIpqcId(), ipqc.getIpqcCode(), ipqc.getItemName());
    }

    /** 取入库单首行（完工检单头物料四件套快照来源；无行时返回 null 用头字段兜底） */
    private WmProductRecptLine firstRecptLine(Long recptId)
    {
        WmProductRecptLine query = new WmProductRecptLine();
        query.setRecptId(recptId);
        List<WmProductRecptLine> lines = wmProductRecptLineMapper.selectWmProductRecptLineList(query);
        return lines.isEmpty() ? null : lines.get(0);
    }

    @Override
    public String generateIpqcForFeedback(ProFeedback feedback)
    {
        if (feedback == null || feedback.getRouteId() == null || feedback.getProcessId() == null
            || feedback.getItemId() == null || feedback.getCardId() == null)
        {
            return null;
        }
        ProRouteProcess rp = proRouteProcessMapper.selectByRouteAndProcess(feedback.getRouteId(), feedback.getProcessId());
        if (rp == null || !"Y".equals(rp.getIsCheck()))
        {
            return null;  // 非检验工序不生成
        }
        QcTemplateProduct bind = resolveTemplate(QcConstants.TYPE_IPQC, feedback.getItemId(), feedback.getProcessId());
        if (bind == null)
        {
            return null;  // 未绑定模板 = 免检
        }
        String lockKey = QcConstants.LOCK_GENERATE + "ipqc:cp:" + feedback.getCardId() + ":" + feedback.getProcessId();
        // 报工链路是弱拦截（confirmFeedback try-catch 吞异常仅告警），必须用独立新事务，
        // 否则生成失败会标记外层报工事务 rollback-only 导致提交时 UnexpectedRollbackException。
        // 锁+事务内"查或建"流转卡工序记录，保证并发报工不重复播种。
        return lockTemplate.execute(lockKey, () ->
            txTemplateRequiresNew.execute(tx -> {
                ProCardProcess cp = proCardProcessMapper.selectByCardAndProcess(feedback.getCardId(), feedback.getProcessId());
                if (cp == null) {
                    cp = createCardProcess(feedback, rp);
                }
                return doGenerateFeedbackIpqc(feedback, cp, bind);
            }));
    }

    /**
     * 惰性补建流转卡工序记录（历史卡建卡时未播种工序）。字段取报工数据与工艺路线工序，
     * 不与报工产出数量强绑定——card_process 是追溯挂点，数量以 feedback 为准。
     */
    private ProCardProcess createCardProcess(ProFeedback feedback, ProRouteProcess rp) {
        ProCard card = proCardMapper.selectProCardByCardId(feedback.getCardId());
        ProCardProcess cp = new ProCardProcess();
        cp.setCardId(feedback.getCardId());
        cp.setCardCode(card != null ? card.getCardCode() : null);
        cp.setSeqNum(rp != null && rp.getOrderNum() != null ? rp.getOrderNum() : 0);
        cp.setProcessId(feedback.getProcessId());
        cp.setProcessCode(feedback.getProcessCode());
        cp.setProcessName(feedback.getProcessName());
        cp.setTaskId(feedback.getTaskId());
        cp.setTaskCode(feedback.getTaskCode());
        cp.setWorkstationId(feedback.getWorkstationId());
        cp.setWorkstationCode(feedback.getWorkstationCode());
        cp.setWorkstationName(feedback.getWorkstationName());
        cp.setUserId(SecurityUtils.getUserId());
        // App/PC 报工 recordUser/recordNick 常为空，回退到 user_name/nick_name
        cp.setUserName(StringUtils.defaultIfBlank(feedback.getRecordUser(), feedback.getUserName()));
        cp.setNickName(StringUtils.defaultIfBlank(feedback.getRecordNick(), feedback.getNickName()));
        cp.setFeedbackId(feedback.getRecordId());
        cp.setOutputTime(feedback.getFeedbackTime());
        cp.setQuantityOutput(feedback.getQuantityQualified());
        cp.setQuantityUnqualified(feedback.getQuantityUnqualified());
        cp.setCreateBy(SecurityUtils.getUsername());
        cp.setCreateTime(new Date());
        proCardProcessMapper.insertProCardProcess(cp);
        return cp;
    }

    /** 锁+事务内生成工序检 IPQC：幂等检查 → 快照建单 → 建行 → 回填流转卡工序挂点；返回编码或 null */
    private String doGenerateFeedbackIpqc(ProFeedback feedback, ProCardProcess cp, QcTemplateProduct bind)
    {
        List<QcIpqc> exist = ipqcMapper.selectBySource(QcConstants.SOURCE_CARD_PROCESS, cp.getRecordId(), null);
        if (exist.stream().anyMatch(o -> !QcConstants.STATUS_CLOSED.equals(o.getStatus())))
        {
            return null;  // 幂等：同流转卡工序已有未关闭单
        }
        Date now = new Date();
        QcIpqc ipqc = new QcIpqc();
        ipqc.setIpqcCode(QcCodeGenerator.genIpqcCode(autoCodeGenerator));
        ipqc.setIpqcName("工序检-" + StringUtils.defaultString(feedback.getProcessName()));
        ipqc.setIpqcType(QcConstants.IPQC_LAST);   // 报工确认触发的工序完成检
        ipqc.setTemplateId(bind.getTemplateId());
        ipqc.setSourceDocId(cp.getRecordId());
        ipqc.setSourceDocType(QcConstants.SOURCE_CARD_PROCESS);
        ipqc.setSourceDocCode(cp.getCardCode());
        ipqc.setWorkorderId(feedback.getWorkorderId());
        ipqc.setWorkorderCode(feedback.getWorkorderCode());
        ipqc.setWorkorderName(feedback.getWorkorderName());
        ipqc.setCardId(feedback.getCardId());
        ipqc.setCardCode(cp.getCardCode());
        ipqc.setTaskId(feedback.getTaskId());
        ipqc.setTaskCode(feedback.getTaskCode());
        ipqc.setProcessId(feedback.getProcessId());
        ipqc.setProcessCode(feedback.getProcessCode());
        ipqc.setProcessName(feedback.getProcessName());
        ipqc.setWorkstationId(feedback.getWorkstationId());
        ipqc.setWorkstationCode(feedback.getWorkstationCode());
        ipqc.setWorkstationName(feedback.getWorkstationName());
        ipqc.setItemId(feedback.getItemId());
        ipqc.setItemCode(feedback.getItemCode());
        ipqc.setItemName(feedback.getItemName());
        ipqc.setSpecification(feedback.getSpecification());
        ipqc.setUnitOfMeasure(feedback.getUnitOfMeasure());
        snapshotBind(ipqc, bind);
        ipqc.setStatus(QcConstants.STATUS_PENDING);
        ipqc.setCreateTime(now);
        ipqcMapper.insertQcIpqc(ipqc);
        lineMapper.batchInsert(buildLinesFromTemplate(bind.getTemplateId(), QcConstants.TYPE_IPQC, ipqc.getIpqcId()));
        proCardProcessMapper.updateCardProcessRefs(cp.getRecordId(), ipqc.getIpqcId(), ipqc.getIpqcCode());
        qcTodoHelper.createTodo(QcConstants.TYPE_IPQC, ipqc.getIpqcId(), ipqc.getIpqcCode(), ipqc.getItemName());
        return ipqc.getIpqcCode();
    }

    /** 按绑定快照 IPQC 头阈值：样本量/Ac 值/三档缺陷率（与 IQC/OQC 建单同一快照口径） */
    private void snapshotBind(QcIpqc ipqc, QcTemplateProduct bind)
    {
        ipqc.setQuantityMinCheck(bind.getQuantityCheck());
        ipqc.setQuantityMaxUnqualified(bind.getQuantityUnqualified());
        ipqc.setCrRateLimit(bind.getCrRate());
        ipqc.setMajRateLimit(bind.getMajRate());
        ipqc.setMinRateLimit(bind.getMinRate());
    }

    @Override
    public void generateRqcForRtIssue(WmRtIssue header, List<WmRtIssueLine> lines)
    {
        if (header == null || header.getRtId() == null || lines == null || lines.isEmpty())
        {
            return;
        }
        // 多行同物料合并为一组 → 一张退料检验单（与 IQC/OQC 同范式）
        Map<Long, List<WmRtIssueLine>> byItem = lines.stream()
            .filter(l -> l.getItemId() != null)
            .collect(Collectors.groupingBy(WmRtIssueLine::getItemId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<WmRtIssueLine>> e : byItem.entrySet())
        {
            QcTemplateProduct bind = resolveTemplate(QcConstants.TYPE_RQC, e.getKey(), null);
            if (bind == null)
            {
                continue;  // 未绑定模板 = 免检
            }
            generateOneRqc(header, e.getValue(), bind);
        }
    }

    /** 先锁后事务：锁防同单并发重入，事务保证 头/行/挂点/待办 四写原子 */
    private void generateOneRqc(WmRtIssue header, List<WmRtIssueLine> group, QcTemplateProduct bind)
    {
        String lockKey = QcConstants.LOCK_GENERATE + "rqc:" + header.getRtId() + ":" + group.get(0).getItemId();
        lockTemplate.execute(lockKey, () -> {
            txTemplate.execute(tx -> {
                doGenerateOneRqc(header, group, bind);
                return null;
            });
        });
    }

    /** 锁+事务内生成单物料 RQC：幂等检查 → 快照建单 → 建行 → 回填挂点 → 建待办 */
    private void doGenerateOneRqc(WmRtIssue header, List<WmRtIssueLine> group, QcTemplateProduct bind)
    {
        Long itemId = group.get(0).getItemId();
        List<QcRqc> exist = rqcMapper.selectBySource(QcConstants.SOURCE_RT_ISSUE, header.getRtId(), itemId);
        if (exist.stream().anyMatch(o -> !QcConstants.STATUS_CLOSED.equals(o.getStatus())))
        {
            return;  // 幂等：同退料单+物料已有未关闭单
        }
        QcRqc rqc = buildRqc(header, group, bind);
        rqcMapper.insertQcRqc(rqc);
        lineMapper.batchInsert(buildLinesFromTemplate(bind.getTemplateId(), QcConstants.TYPE_RQC, rqc.getRqcId()));
        backfillRtHeaderRefs(header, rqc);
        qcTodoHelper.createTodo(QcConstants.TYPE_RQC, rqc.getRqcId(), rqc.getRqcCode(), rqc.getItemName());
    }

    /** 按绑定快照阈值构建 RQC 单头（退料数量=ΣquantityRt，批次/工单取头行快照） */
    private QcRqc buildRqc(WmRtIssue header, List<WmRtIssueLine> group, QcTemplateProduct bind)
    {
        WmRtIssueLine first = group.get(0);
        BigDecimal quantityRt = group.stream().map(WmRtIssueLine::getQuantityRt).map(this::nvl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Date now = new Date();
        QcRqc rqc = new QcRqc();
        rqc.setRqcCode(QcCodeGenerator.genRqcCode(autoCodeGenerator));
        rqc.setRqcName("退料检验-" + StringUtils.defaultString(first.getItemName()));
        rqc.setRqcType(QcConstants.RQC_TYPE_PROD_RETURN);
        rqc.setTemplateId(bind.getTemplateId());
        rqc.setSourceDocId(header.getRtId());
        rqc.setSourceDocType(QcConstants.SOURCE_RT_ISSUE);
        rqc.setSourceDocCode(header.getRtCode());
        rqc.setSourceLineId(first.getLineId());
        rqc.setWorkorderId(header.getWorkorderId());
        rqc.setWorkorderCode(header.getWorkorderCode());
        rqc.setItemId(first.getItemId());
        rqc.setItemCode(first.getItemCode());
        rqc.setItemName(first.getItemName());
        rqc.setSpecification(first.getItemSpc());
        rqc.setUnitOfMeasure(first.getUnitOfMeasure());
        rqc.setBatchCode(first.getBatchCode());
        rqc.setQuantityCheck(quantityRt);
        rqc.setQuantityMinCheck(bind.getQuantityCheck());
        rqc.setQuantityMaxUnqualified(bind.getQuantityUnqualified());
        rqc.setCrRateLimit(bind.getCrRate());
        rqc.setMajRateLimit(bind.getMajRate());
        rqc.setMinRateLimit(bind.getMinRate());
        rqc.setStatus(QcConstants.STATUS_PENDING);
        rqc.setCreateTime(now);
        return rqc;
    }

    /** 回填退料单头 RQC 挂点（仅首张；两列专用 UPDATE 避免并发覆盖整头） */
    private void backfillRtHeaderRefs(WmRtIssue header, QcRqc rqc)
    {
        if (header.getRqcId() != null)
        {
            return;
        }
        header.setRqcId(rqc.getRqcId());
        header.setRqcCode(rqc.getRqcCode());
        wmRtIssueMapper.updateRtIssueHeaderRefs(header.getRtId(), rqc.getRqcId(), rqc.getRqcCode());
    }

    private BigDecimal nvl(BigDecimal v)
    {
        return v == null ? BigDecimal.ZERO : v;
    }
}
