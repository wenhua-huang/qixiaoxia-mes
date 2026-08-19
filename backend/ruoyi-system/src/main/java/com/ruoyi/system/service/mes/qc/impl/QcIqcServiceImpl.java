package com.ruoyi.system.service.mes.qc.impl;

import java.math.BigDecimal;
import java.util.List;

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
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.qc.QcJudgeConfig;
import com.ruoyi.system.domain.mes.qc.QcJudgeResult;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.mapper.mes.qc.QcDefectRecordMapper;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.service.mes.qc.IQcIqcService;
import com.ruoyi.system.service.mes.qc.IQcJudgeService;
import com.ruoyi.system.service.mes.qc.IQcOrderLineService;
import com.ruoyi.system.service.mes.qc.QcCodeGenerator;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.qc.QcTodoHelper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import jakarta.annotation.PostConstruct;

/**
 * 来料检验单Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 头表 + 检验行级联：行采用全删全插（null=本次未提交行集，不清空，防仅改头字段时误删行）。
 * 判定(judgeIqc)：锁+事务内复用 IQcJudgeService，COMPLETED 后判定域字段不可再编辑。
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@Service
public class QcIqcServiceImpl implements IQcIqcService
{

    @Autowired
    private QcIqcMapper qcIqcMapper;

    @Autowired
    private QcDefectRecordMapper qcDefectRecordMapper;

    @Autowired
    private IQcOrderLineService qcOrderLineService;

    @Autowired
    private IQcJudgeService qcJudgeService;

    @Autowired
    private QcTodoHelper qcTodoHelper;

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
    public List<QcIqc> selectQcIqcList(QcIqc qciqc)
    {
        return qcIqcMapper.selectQcIqcList(qciqc);
    }

    @Override
    public QcIqc selectQcIqcByIqcId(Long iqcId)
    {
        QcIqc iqc = qcIqcMapper.selectQcIqcByIqcId(iqcId);
        if (iqc != null)
        {
            iqc.setLines(qcOrderLineService.selectByOrder(QcConstants.TYPE_IQC, iqcId));
            iqc.setDefectRecords(qcDefectRecordMapper.selectByOrder(QcConstants.TYPE_IQC, iqcId));
        }
        return iqc;
    }

    @Override
    @Transactional
    public int insertQcIqc(QcIqc qciqc)
    {
        ensureIqcCode(qciqc);
        if (qcIqcMapper.checkIqcCodeUnique(qciqc.getIqcCode()) != null)
        {
            throw new ServiceException("检验单编码已存在");
        }
        qciqc.setCreateTime(DateUtils.getNowDate());
        int rows = qcIqcMapper.insertQcIqc(qciqc);
        qcOrderLineService.replaceLines(QcConstants.TYPE_IQC, qciqc.getIqcId(), qciqc.getLines());
        return rows;
    }

    @Override
    @Transactional
    public int updateQcIqc(QcIqc qciqc)
    {
        if (qciqc.getIqcId() == null)
        {
            throw new ServiceException("检验单主键不能为空");
        }
        QcIqc current = qcIqcMapper.selectQcIqcByIqcId(qciqc.getIqcId());
        if (current == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_COMPLETED.equals(current.getStatus())
            || QcConstants.STATUS_CLOSED.equals(current.getStatus()))
        {
            throw new ServiceException("已判定的检验单不可编辑");
        }
        keepJudgementFields(qciqc, current);
        qciqc.setUpdateTime(DateUtils.getNowDate());
        int rows = qcIqcMapper.updateQcIqc(qciqc);
        // 行集/缺陷集 null=本次未提交，不清空（与模板头行级联同一保护策略）
        if (qciqc.getLines() != null)
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_IQC, qciqc.getIqcId(), qciqc.getLines());
        }
        if (qciqc.getDefectRecords() != null)
        {
            replaceDefectRecords(QcConstants.TYPE_IQC, qciqc.getIqcId(), qciqc.getDefectRecords());
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
    private void keepJudgementFields(QcIqc target, QcIqc current)
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
    public int deleteQcIqcByIqcId(Long iqcId)
    {
        qcOrderLineService.deleteByOrder(QcConstants.TYPE_IQC, iqcId);
        qcDefectRecordMapper.deleteByOrder(QcConstants.TYPE_IQC, iqcId);
        return qcIqcMapper.deleteQcIqcByIqcId(iqcId);
    }

    @Override
    @Transactional
    public int deleteQcIqcByIqcIds(Long[] iqcIds)
    {
        int rows = 0;
        for (Long iqcId : iqcIds)
        {
            rows += deleteQcIqcByIqcId(iqcId);
        }
        return rows;
    }

    @Override
    public List<QcIqc> listBySource(String sourceDocType, Long sourceDocId)
    {
        return qcIqcMapper.selectBySource(sourceDocType, sourceDocId, null);
    }

    @Override
    public void closeIqc(Long iqcId)
    {
        // 与判定共用 LOCK_JUDGE 串行化，锁内条件 UPDATE 消除读-改竞态
        String lockKey = QcConstants.LOCK_JUDGE + "IQC:" + iqcId;
        lockTemplate.execute(lockKey, () -> {
            int rows = qcIqcMapper.closeIfActive(iqcId, SecurityUtils.getUsername(), DateUtils.getNowDate());
            if (rows > 0)
            {
                return;
            }
            // 0 行：不存在 / 已关闭(幂等) / 已判定(拒绝)，重读给出精确反馈
            QcIqc existing = qcIqcMapper.selectQcIqcByIqcId(iqcId);
            if (existing == null)
            {
                throw new ServiceException("检验单不存在");
            }
            if (QcConstants.STATUS_CLOSED.equals(existing.getStatus()))
            {
                return;
            }
            if (QcConstants.STATUS_COMPLETED.equals(existing.getStatus()))
            {
                throw new ServiceException("已判定的检验单不可关闭（判定结果可能已驱动下游入库）");
            }
            throw new ServiceException("当前检验单状态不可关闭：" + existing.getStatus());
        });
    }

    /**
     * 执行判定：先锁后事务（锁防并发重复判定，事务保证 行回填+头回写 原子）。
     * FAIL 可携带让步理由升级为 CONCESSION；CONCESSION 必填理由。
     */
    @Override
    public void judgeIqc(Long iqcId, String concessionReason)
    {
        String lockKey = QcConstants.LOCK_JUDGE + "IQC:" + iqcId;
        // 块状 void lambda 显式绑定 Runnable 重载（表达式 lambda 会歧义绑定到 Supplier 重载）
        lockTemplate.execute(lockKey, () -> {
            txTemplate.execute(tx -> {
                doJudgeIqc(iqcId, concessionReason);
                return null;
            });
        });
    }

    /** 锁+事务内判定：守卫 → 载入行/缺陷 → 引擎判定 → 让步处理 → 行结果回填 → 头回写 */
    private void doJudgeIqc(Long iqcId, String concessionReason)
    {
        QcIqc iqc = qcIqcMapper.selectQcIqcByIqcId(iqcId);
        if (iqc == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_COMPLETED.equals(iqc.getStatus())
            || QcConstants.STATUS_CLOSED.equals(iqc.getStatus()))
        {
            throw new ServiceException("已完成或已关闭的检验单不可判定");
        }
        List<QcOrderLine> lines = qcOrderLineService.selectByOrder(QcConstants.TYPE_IQC, iqcId);
        if (lines.isEmpty())
        {
            throw new ServiceException("检验单无检测项");
        }
        List<QcDefectRecord> defects = qcDefectRecordMapper.selectByOrder(QcConstants.TYPE_IQC, iqcId);
        QcJudgeResult r = qcJudgeService.judge(lines, defects, buildJudgeConfig(iqc));
        String finalResult = resolveFinalResult(r.getResult(), concessionReason);
        qcOrderLineService.replaceLines(QcConstants.TYPE_IQC, iqcId, lines);   // 回填行结果
        iqc.setCheckResult(finalResult);
        iqc.setConcessionReason(QcConstants.RESULT_CONCESSION.equals(finalResult) ? concessionReason : null);
        iqc.setQuantityUnqualified(r.getQuantityUnqualified());
        iqc.setQuantityQualified(Math.max(nvl(iqc.getQuantityCheck()) - r.getQuantityUnqualified(), 0));
        iqc.setCrQuantity(r.getCrQuantity());
        iqc.setMajQuantity(r.getMajQuantity());
        iqc.setMinQuantity(r.getMinQuantity());
        iqc.setCrRate(BigDecimal.valueOf(r.getCrRate()));
        iqc.setMajRate(BigDecimal.valueOf(r.getMajRate()));
        iqc.setMinRate(BigDecimal.valueOf(r.getMinRate()));
        iqc.setStatus(QcConstants.STATUS_COMPLETED);
        iqc.setInspectDate(DateUtils.getNowDate());
        iqc.setInspector(SecurityUtils.getUsername());
        qcIqcMapper.updateQcIqc(iqc);
        qcTodoHelper.completeTodo(QcConstants.TYPE_IQC, iqcId, finalResult);
    }

    /** 判定配置取 IQC 头快照（Ac 值/三档缺陷率阈值）+ 实际检测数 */
    private QcJudgeConfig buildJudgeConfig(QcIqc iqc)
    {
        QcJudgeConfig cfg = new QcJudgeConfig();
        cfg.setQuantityCheck(iqc.getQuantityCheck());
        cfg.setAcQuantity(iqc.getQuantityMaxUnqualified());
        cfg.setCrRateLimit(dbl(iqc.getCrRateLimit()));
        cfg.setMajRateLimit(dbl(iqc.getMajRateLimit()));
        cfg.setMinRateLimit(dbl(iqc.getMinRateLimit()));
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
     * 失败/未配置时 IQC+时间戳+4位随机兜底，与生成工厂共用同一语义）。
     * DB 唯一约束 uk_iqc_code 是最终防线，冲突时抛 ServiceException。
     */
    private void ensureIqcCode(QcIqc qciqc)
    {
        if (qciqc.getIqcCode() != null && !qciqc.getIqcCode().isEmpty())
        {
            return;
        }
        qciqc.setIqcCode(QcCodeGenerator.genIqcCode(autoCodeGenerator));
    }
}
