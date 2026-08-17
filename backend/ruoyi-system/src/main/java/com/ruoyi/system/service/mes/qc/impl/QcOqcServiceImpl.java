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
import com.ruoyi.system.domain.mes.qc.QcJudgeConfig;
import com.ruoyi.system.domain.mes.qc.QcJudgeResult;
import com.ruoyi.system.domain.mes.qc.QcOqc;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.mapper.mes.qc.QcDefectRecordMapper;
import com.ruoyi.system.mapper.mes.qc.QcOqcMapper;
import com.ruoyi.system.service.mes.qc.IQcJudgeService;
import com.ruoyi.system.service.mes.qc.IQcOqcService;
import com.ruoyi.system.service.mes.qc.IQcOrderLineService;
import com.ruoyi.system.service.mes.qc.QcCodeGenerator;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import jakarta.annotation.PostConstruct;

/**
 * 出货检验单Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 头表 + 检验行级联：行采用全删全插（null=本次未提交行集，不清空，防仅改头字段时误删行）。
 * 判定(judgeOqc)：锁+事务内复用 IQcJudgeService，COMPLETED 后判定域字段不可再编辑。
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@Service
public class QcOqcServiceImpl implements IQcOqcService
{

    @Autowired
    private QcOqcMapper qcOqcMapper;

    @Autowired
    private QcDefectRecordMapper qcDefectRecordMapper;

    @Autowired
    private IQcOrderLineService qcOrderLineService;

    @Autowired
    private IQcJudgeService qcJudgeService;

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
    public List<QcOqc> selectQcOqcList(QcOqc qcoqc)
    {
        return qcOqcMapper.selectQcOqcList(qcoqc);
    }

    @Override
    public QcOqc selectQcOqcByOqcId(Long oqcId)
    {
        QcOqc oqc = qcOqcMapper.selectQcOqcByOqcId(oqcId);
        if (oqc != null)
        {
            oqc.setLines(qcOrderLineService.selectByOrder(QcConstants.TYPE_OQC, oqcId));
            oqc.setDefectRecords(qcDefectRecordMapper.selectByOrder(QcConstants.TYPE_OQC, oqcId));
        }
        return oqc;
    }

    @Override
    @Transactional
    public int insertQcOqc(QcOqc qcoqc)
    {
        ensureOqcCode(qcoqc);
        if (qcOqcMapper.checkOqcCodeUnique(qcoqc.getOqcCode()) != null)
        {
            throw new ServiceException("检验单编码已存在");
        }
        qcoqc.setCreateTime(DateUtils.getNowDate());
        int rows = qcOqcMapper.insertQcOqc(qcoqc);
        qcOrderLineService.replaceLines(QcConstants.TYPE_OQC, qcoqc.getOqcId(), qcoqc.getLines());
        return rows;
    }

    @Override
    @Transactional
    public int updateQcOqc(QcOqc qcoqc)
    {
        if (qcoqc.getOqcId() == null)
        {
            throw new ServiceException("检验单主键不能为空");
        }
        QcOqc current = qcOqcMapper.selectQcOqcByOqcId(qcoqc.getOqcId());
        if (current == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_COMPLETED.equals(current.getStatus())
            || QcConstants.STATUS_CLOSED.equals(current.getStatus()))
        {
            throw new ServiceException("已判定的检验单不可编辑");
        }
        keepJudgementFields(qcoqc, current);
        qcoqc.setUpdateTime(DateUtils.getNowDate());
        int rows = qcOqcMapper.updateQcOqc(qcoqc);
        // 行集/缺陷集 null=本次未提交，不清空（与模板头行级联同一保护策略）
        if (qcoqc.getLines() != null)
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_OQC, qcoqc.getOqcId(), qcoqc.getLines());
        }
        if (qcoqc.getDefectRecords() != null)
        {
            replaceDefectRecords(QcConstants.TYPE_OQC, qcoqc.getOqcId(), qcoqc.getDefectRecords());
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
    private void keepJudgementFields(QcOqc target, QcOqc current)
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
    public int deleteQcOqcByOqcId(Long oqcId)
    {
        qcOrderLineService.deleteByOrder(QcConstants.TYPE_OQC, oqcId);
        qcDefectRecordMapper.deleteByOrder(QcConstants.TYPE_OQC, oqcId);
        return qcOqcMapper.deleteQcOqcByOqcId(oqcId);
    }

    @Override
    @Transactional
    public int deleteQcOqcByOqcIds(Long[] oqcIds)
    {
        int rows = 0;
        for (Long oqcId : oqcIds)
        {
            rows += deleteQcOqcByOqcId(oqcId);
        }
        return rows;
    }

    @Override
    public List<QcOqc> listBySource(String sourceDocType, Long sourceDocId)
    {
        return qcOqcMapper.selectBySource(sourceDocType, sourceDocId, null);
    }

    @Override
    public void closeOqc(Long oqcId)
    {
        QcOqc oqc = qcOqcMapper.selectQcOqcByOqcId(oqcId);
        if (oqc == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_CLOSED.equals(oqc.getStatus()))
        {
            return;  // 幂等：已关闭直接返回
        }
        if (QcConstants.STATUS_COMPLETED.equals(oqc.getStatus()))
        {
            throw new ServiceException("已判定的检验单不可关闭（判定结果可能已驱动下游出库）");
        }
        QcOqc update = new QcOqc();
        update.setOqcId(oqcId);
        update.setStatus(QcConstants.STATUS_CLOSED);
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        qcOqcMapper.updateQcOqc(update);
    }

    /**
     * 执行判定：先锁后事务（锁防并发重复判定，事务保证 行回填+头回写 原子）。
     * FAIL 可携带让步理由升级为 CONCESSION；CONCESSION 必填理由。
     * 头回写只动 inspectDate/inspector 与判定汇总，outDate（出货日期）保持不变。
     */
    @Override
    public void judgeOqc(Long oqcId, String concessionReason)
    {
        String lockKey = QcConstants.LOCK_JUDGE + "OQC:" + oqcId;
        // 块状 void lambda 显式绑定 Runnable 重载（表达式 lambda 会歧义绑定到 Supplier 重载）
        lockTemplate.execute(lockKey, () -> {
            txTemplate.execute(tx -> {
                doJudgeOqc(oqcId, concessionReason);
                return null;
            });
        });
    }

    /** 锁+事务内判定：守卫 → 载入行/缺陷 → 引擎判定 → 让步处理 → 行结果回填 → 头回写 */
    private void doJudgeOqc(Long oqcId, String concessionReason)
    {
        QcOqc oqc = qcOqcMapper.selectQcOqcByOqcId(oqcId);
        if (oqc == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_COMPLETED.equals(oqc.getStatus())
            || QcConstants.STATUS_CLOSED.equals(oqc.getStatus()))
        {
            throw new ServiceException("已完成或已关闭的检验单不可判定");
        }
        List<QcOrderLine> lines = qcOrderLineService.selectByOrder(QcConstants.TYPE_OQC, oqcId);
        if (lines.isEmpty())
        {
            throw new ServiceException("检验单无检测项");
        }
        List<QcDefectRecord> defects = qcDefectRecordMapper.selectByOrder(QcConstants.TYPE_OQC, oqcId);
        QcJudgeResult r = qcJudgeService.judge(lines, defects, buildJudgeConfig(oqc));
        String finalResult = resolveFinalResult(r.getResult(), concessionReason);
        qcOrderLineService.replaceLines(QcConstants.TYPE_OQC, oqcId, lines);   // 回填行结果
        oqc.setCheckResult(finalResult);
        oqc.setConcessionReason(QcConstants.RESULT_CONCESSION.equals(finalResult) ? concessionReason : null);
        oqc.setQuantityUnqualified(r.getQuantityUnqualified());
        oqc.setQuantityQualified(Math.max(nvl(oqc.getQuantityCheck()) - r.getQuantityUnqualified(), 0));
        oqc.setCrQuantity(r.getCrQuantity());
        oqc.setMajQuantity(r.getMajQuantity());
        oqc.setMinQuantity(r.getMinQuantity());
        oqc.setCrRate(BigDecimal.valueOf(r.getCrRate()));
        oqc.setMajRate(BigDecimal.valueOf(r.getMajRate()));
        oqc.setMinRate(BigDecimal.valueOf(r.getMinRate()));
        oqc.setStatus(QcConstants.STATUS_COMPLETED);
        oqc.setInspectDate(DateUtils.getNowDate());
        oqc.setInspector(SecurityUtils.getUsername());
        qcOqcMapper.updateQcOqc(oqc);
    }

    /** 判定配置取 OQC 头快照（Ac 值/三档缺陷率阈值）+ 实际检测数 */
    private QcJudgeConfig buildJudgeConfig(QcOqc oqc)
    {
        QcJudgeConfig cfg = new QcJudgeConfig();
        cfg.setQuantityCheck(oqc.getQuantityCheck());
        cfg.setAcQuantity(oqc.getQuantityMaxUnqualified());
        cfg.setCrRateLimit(dbl(oqc.getCrRateLimit()));
        cfg.setMajRateLimit(dbl(oqc.getMajRateLimit()));
        cfg.setMinRateLimit(dbl(oqc.getMinRateLimit()));
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
     * 失败/未配置时 OQC+时间戳+4位随机兜底，与生成工厂共用同一语义）。
     * DB 唯一约束 uk_oqc_code 是最终防线，冲突时抛 ServiceException。
     */
    private void ensureOqcCode(QcOqc qcoqc)
    {
        if (qcoqc.getOqcCode() != null && !qcoqc.getOqcCode().isEmpty())
        {
            return;
        }
        qcoqc.setOqcCode(QcCodeGenerator.genOqcCode(autoCodeGenerator));
    }
}
