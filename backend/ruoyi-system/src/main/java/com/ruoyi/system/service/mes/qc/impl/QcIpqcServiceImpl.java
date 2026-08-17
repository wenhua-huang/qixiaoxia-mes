package com.ruoyi.system.service.mes.qc.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.ruoyi.common.core.redis.RedisLockTemplate;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.qc.QcDefectRecord;
import com.ruoyi.system.domain.mes.qc.QcIpqc;
import com.ruoyi.system.domain.mes.qc.QcJudgeConfig;
import com.ruoyi.system.domain.mes.qc.QcJudgeResult;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.mapper.mes.qc.QcDefectRecordMapper;
import com.ruoyi.system.mapper.mes.qc.QcIpqcMapper;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.IQcIpqcService;
import com.ruoyi.system.service.mes.qc.IQcJudgeService;
import com.ruoyi.system.service.mes.qc.IQcOrderLineService;
import com.ruoyi.system.service.mes.qc.QcCodeGenerator;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import jakarta.annotation.PostConstruct;

/**
 * 过程检验单Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 头表 + 检验行级联：行采用全删全插（null=本次未提交行集，不清空，防仅改头字段时误删行）。
 * 判定(judgeIpqc)：锁+事务内复用 IQcJudgeService，COMPLETED 后判定域字段不可再编辑。
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
@Service
public class QcIpqcServiceImpl implements IQcIpqcService
{

    /** 手工创建允许的检验类型（报工/入库触发固定 LAST_CHECK，不走此入口） */
    private static final Set<String> MANUAL_IPQC_TYPES = Set.of(
        QcConstants.IPQC_FIRST, QcConstants.IPQC_TOUR, QcConstants.IPQC_SPOT, QcConstants.IPQC_LAST);

    @Autowired
    private QcIpqcMapper qcIpqcMapper;

    @Autowired
    private QcDefectRecordMapper qcDefectRecordMapper;

    @Autowired
    private IQcOrderLineService qcOrderLineService;

    @Autowired
    private IQcJudgeService qcJudgeService;

    @Autowired
    private IQcFactoryService qcFactoryService;

    @Autowired
    private RedisLockTemplate lockTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired(required = false)
    private AutoCodeGenerator autoCodeGenerator;

    private TransactionTemplate txTemplate;

    @PostConstruct
    void initTx()
    {
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setTimeout(30);
    }

    @Override
    public List<QcIpqc> selectQcIpqcList(QcIpqc qcipqc)
    {
        return qcIpqcMapper.selectQcIpqcList(qcipqc);
    }

    @Override
    public QcIpqc selectQcIpqcByIpqcId(Long ipqcId)
    {
        QcIpqc ipqc = qcIpqcMapper.selectQcIpqcByIpqcId(ipqcId);
        if (ipqc != null)
        {
            ipqc.setLines(qcOrderLineService.selectByOrder(QcConstants.TYPE_IPQC, ipqcId));
            ipqc.setDefectRecords(qcDefectRecordMapper.selectByOrder(QcConstants.TYPE_IPQC, ipqcId));
        }
        return ipqc;
    }

    @Override
    @Transactional
    public int insertQcIpqc(QcIpqc qcipqc)
    {
        validateManualCreate(qcipqc);
        ensureIpqcCode(qcipqc);
        if (qcIpqcMapper.checkIpqcCodeUnique(qcipqc.getIpqcCode()) != null)
        {
            throw new ServiceException("检验单编码已存在");
        }
        if (qcipqc.getStatus() == null || qcipqc.getStatus().isEmpty())
        {
            qcipqc.setStatus(QcConstants.STATUS_PENDING);
        }
        qcipqc.setCreateTime(DateUtils.getNowDate());
        int rows = qcIpqcMapper.insertQcIpqc(qcipqc);
        // 未显式携带检验行时按模板快照自动生成（手工建单前端只选模板，检验项由后端拥有）
        if (qcipqc.getLines() == null || qcipqc.getLines().isEmpty())
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_IPQC, qcipqc.getIpqcId(),
                qcFactoryService.buildLinesFromTemplate(qcipqc.getTemplateId(), QcConstants.TYPE_IPQC, qcipqc.getIpqcId()));
        }
        else
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_IPQC, qcipqc.getIpqcId(), qcipqc.getLines());
        }
        return rows;
    }

    /** 手工创建校验：检验类型枚举 + 工单/物料/模板必填（手工单必须可追溯到工单与检验标准） */
    private void validateManualCreate(QcIpqc qcipqc)
    {
        if (qcipqc.getIpqcType() == null || !MANUAL_IPQC_TYPES.contains(qcipqc.getIpqcType()))
        {
            throw new ServiceException("检验类型非法，仅支持 FIRST_CHECK/TOUR_CHECK/SPOT_CHECK/LAST_CHECK");
        }
        if (qcipqc.getWorkorderId() == null)
        {
            throw new ServiceException("工单不能为空");
        }
        if (qcipqc.getItemId() == null)
        {
            throw new ServiceException("物料不能为空");
        }
        if (qcipqc.getTemplateId() == null)
        {
            throw new ServiceException("检验模板不能为空");
        }
    }

    @Override
    @Transactional
    public int updateQcIpqc(QcIpqc qcipqc)
    {
        if (qcipqc.getIpqcId() == null)
        {
            throw new ServiceException("检验单主键不能为空");
        }
        QcIpqc current = qcIpqcMapper.selectQcIpqcByIpqcId(qcipqc.getIpqcId());
        if (current == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_COMPLETED.equals(current.getStatus())
            || QcConstants.STATUS_CLOSED.equals(current.getStatus()))
        {
            throw new ServiceException("已判定的检验单不可编辑");
        }
        keepJudgementFields(qcipqc, current);
        qcipqc.setUpdateTime(DateUtils.getNowDate());
        int rows = qcIpqcMapper.updateQcIpqc(qcipqc);
        // 行集/缺陷集 null=本次未提交，不清空（与模板头行级联同一保护策略）
        if (qcipqc.getLines() != null)
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_IPQC, qcipqc.getIpqcId(), qcipqc.getLines());
        }
        if (qcipqc.getDefectRecords() != null)
        {
            replaceDefectRecords(QcConstants.TYPE_IPQC, qcipqc.getIpqcId(), qcipqc.getDefectRecords());
        }
        return rows;
    }

    /** 缺陷记录全删全插（编辑频度低，替换策略最简单且无孤儿行；null 语义在调用处判断） */
    private void replaceDefectRecords(String qcType, Long qcId, List<QcDefectRecord> defects)
    {
        qcDefectRecordMapper.deleteByOrder(qcType, qcId);
        if (defects == null || defects.isEmpty())
        {
            return;
        }
        for (QcDefectRecord defect : defects)
        {
            defect.setRecordId(null);
            defect.setQcType(qcType);
            defect.setQcId(qcId);
            if (defect.getCreateTime() == null)
            {
                defect.setCreateTime(DateUtils.getNowDate());
            }
        }
        qcDefectRecordMapper.batchInsert(defects);
    }

    /**
     * 判定域+模板快照字段服务端强制以 DB 现值为准（edit 请求中传入的值一律忽略），
     * 防止绕过 judge 流程篡改判定结果/状态，及判定前篡改 Ac 值/三率阈值放宽标准。
     */
    private void keepJudgementFields(QcIpqc target, QcIpqc current)
    {
        target.setStatus(current.getStatus());
        target.setCheckResult(current.getCheckResult());
        target.setConcessionReason(current.getConcessionReason());
        target.setCrRate(current.getCrRate());
        target.setMajRate(current.getMajRate());
        target.setMinRate(current.getMinRate());
        target.setCrQuantity(current.getCrQuantity());
        target.setMajQuantity(current.getMajQuantity());
        target.setMinQuantity(current.getMinQuantity());
        target.setQuantityQualified(current.getQuantityQualified());
        target.setQuantityUnqualified(current.getQuantityUnqualified());
        target.setQuantityMinCheck(current.getQuantityMinCheck());
        target.setQuantityMaxUnqualified(current.getQuantityMaxUnqualified());
        target.setCrRateLimit(current.getCrRateLimit());
        target.setMajRateLimit(current.getMajRateLimit());
        target.setMinRateLimit(current.getMinRateLimit());
    }

    @Override
    @Transactional
    public int deleteQcIpqcByIpqcId(Long ipqcId)
    {
        qcOrderLineService.deleteByOrder(QcConstants.TYPE_IPQC, ipqcId);
        qcDefectRecordMapper.deleteByOrder(QcConstants.TYPE_IPQC, ipqcId);
        return qcIpqcMapper.deleteQcIpqcByIpqcId(ipqcId);
    }

    @Override
    @Transactional
    public int deleteQcIpqcByIpqcIds(Long[] ipqcIds)
    {
        int rows = 0;
        for (Long ipqcId : ipqcIds)
        {
            rows += deleteQcIpqcByIpqcId(ipqcId);
        }
        return rows;
    }

    @Override
    public List<QcIpqc> listBySource(String sourceDocType, Long sourceDocId)
    {
        return qcIpqcMapper.selectBySource(sourceDocType, sourceDocId, null);
    }

    @Override
    public void closeIpqc(Long ipqcId)
    {
        QcIpqc ipqc = qcIpqcMapper.selectQcIpqcByIpqcId(ipqcId);
        if (ipqc == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_CLOSED.equals(ipqc.getStatus()))
        {
            return;  // 幂等：已关闭直接返回
        }
        if (QcConstants.STATUS_COMPLETED.equals(ipqc.getStatus()))
        {
            throw new ServiceException("已判定的检验单不可关闭（判定结果可能已驱动下游入库）");
        }
        QcIpqc update = new QcIpqc();
        update.setIpqcId(ipqcId);
        update.setStatus(QcConstants.STATUS_CLOSED);
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        qcIpqcMapper.updateQcIpqc(update);
    }

    /**
     * 执行判定：先锁后事务（锁防并发重复判定，事务保证 行回填+头回写 原子）。
     * FAIL 可携带让步理由升级为 CONCESSION；CONCESSION 必填理由。
     * 判定通过后不自动流转流转卡，只完成检验单（流转卡推进仍由报工链路负责）。
     */
    @Override
    public void judgeIpqc(Long ipqcId, String concessionReason)
    {
        String lockKey = QcConstants.LOCK_JUDGE + "IPQC:" + ipqcId;
        // 块状 void lambda 显式绑定 Runnable 重载（表达式 lambda 会歧义绑定到 Supplier 重载）
        lockTemplate.execute(lockKey, () -> {
            txTemplate.execute(tx -> {
                doJudgeIpqc(ipqcId, concessionReason);
                return null;
            });
        });
    }

    /** 锁+事务内判定：守卫 → 载入行/缺陷 → 引擎判定 → 让步处理 → 行结果回填 → 头回写 */
    private void doJudgeIpqc(Long ipqcId, String concessionReason)
    {
        QcIpqc ipqc = qcIpqcMapper.selectQcIpqcByIpqcId(ipqcId);
        if (ipqc == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_COMPLETED.equals(ipqc.getStatus())
            || QcConstants.STATUS_CLOSED.equals(ipqc.getStatus()))
        {
            throw new ServiceException("已完成或已关闭的检验单不可判定");
        }
        List<QcOrderLine> lines = qcOrderLineService.selectByOrder(QcConstants.TYPE_IPQC, ipqcId);
        if (lines.isEmpty())
        {
            throw new ServiceException("检验单无检测项");
        }
        List<QcDefectRecord> defects = qcDefectRecordMapper.selectByOrder(QcConstants.TYPE_IPQC, ipqcId);
        QcJudgeResult r = qcJudgeService.judge(lines, defects, buildJudgeConfig(ipqc));
        String finalResult = resolveFinalResult(r.getResult(), concessionReason);
        qcOrderLineService.replaceLines(QcConstants.TYPE_IPQC, ipqcId, lines);   // 回填行结果
        ipqc.setCheckResult(finalResult);
        ipqc.setConcessionReason(QcConstants.RESULT_CONCESSION.equals(finalResult) ? concessionReason : null);
        ipqc.setQuantityUnqualified(r.getQuantityUnqualified());
        ipqc.setQuantityQualified(Math.max(nvl(ipqc.getQuantityCheck()) - r.getQuantityUnqualified(), 0));
        ipqc.setCrQuantity(r.getCrQuantity());
        ipqc.setMajQuantity(r.getMajQuantity());
        ipqc.setMinQuantity(r.getMinQuantity());
        ipqc.setCrRate(BigDecimal.valueOf(r.getCrRate()));
        ipqc.setMajRate(BigDecimal.valueOf(r.getMajRate()));
        ipqc.setMinRate(BigDecimal.valueOf(r.getMinRate()));
        ipqc.setStatus(QcConstants.STATUS_COMPLETED);
        ipqc.setInspectDate(DateUtils.getNowDate());
        ipqc.setInspector(SecurityUtils.getUsername());
        qcIpqcMapper.updateQcIpqc(ipqc);
    }

    /** 判定配置取 IPQC 头快照（Ac 值/三档缺陷率阈值）+ 实际检测数 */
    private QcJudgeConfig buildJudgeConfig(QcIpqc ipqc)
    {
        QcJudgeConfig cfg = new QcJudgeConfig();
        cfg.setQuantityCheck(ipqc.getQuantityCheck());
        cfg.setAcQuantity(ipqc.getQuantityMaxUnqualified());
        cfg.setCrRateLimit(dbl(ipqc.getCrRateLimit()));
        cfg.setMajRateLimit(dbl(ipqc.getMajRateLimit()));
        cfg.setMinRateLimit(dbl(ipqc.getMinRateLimit()));
        return cfg;
    }

    /** 引擎结果让步处理：FAIL+有让步理由→CONCESSION；CONCESSION 必填理由 */
    private String resolveFinalResult(String result, String concessionReason)
    {
        if (QcConstants.RESULT_FAIL.equals(result) && StringUtils.isNotBlank(concessionReason))
        {
            return QcConstants.RESULT_CONCESSION;
        }
        if (QcConstants.RESULT_CONCESSION.equals(result) && StringUtils.isBlank(concessionReason))
        {
            throw new ServiceException("让步接收必须填写让步理由");
        }
        return result;
    }

    private int nvl(Integer v)
    {
        return v == null ? 0 : v;
    }

    private double dbl(BigDecimal v)
    {
        return v == null ? 0d : v.doubleValue();
    }

    /**
     * 生成或校验检验单编码：已带编码直接用；否则走 QcCodeGenerator（规则编码优先，
     * 失败/未配置时 IPQC+时间戳+4位随机兜底，与生成工厂共用同一语义）。
     * DB 唯一约束 uk_ipqc_code 是最终防线，冲突时抛 ServiceException。
     */
    private void ensureIpqcCode(QcIpqc qcipqc)
    {
        if (qcipqc.getIpqcCode() != null && !qcipqc.getIpqcCode().isEmpty())
        {
            return;
        }
        qcipqc.setIpqcCode(QcCodeGenerator.genIpqcCode(autoCodeGenerator));
    }
}
