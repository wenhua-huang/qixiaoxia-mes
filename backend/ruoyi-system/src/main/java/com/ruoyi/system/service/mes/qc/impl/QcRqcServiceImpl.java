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
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.domain.mes.qc.QcRqc;
import com.ruoyi.system.mapper.mes.qc.QcDefectRecordMapper;
import com.ruoyi.system.mapper.mes.qc.QcRqcMapper;
import com.ruoyi.system.service.mes.qc.IQcFactoryService;
import com.ruoyi.system.service.mes.qc.IQcJudgeService;
import com.ruoyi.system.service.mes.qc.IQcOrderLineService;
import com.ruoyi.system.service.mes.qc.IQcRqcService;
import com.ruoyi.system.service.mes.qc.QcCodeGenerator;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.qc.QcTodoHelper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;

import jakarta.annotation.PostConstruct;

/**
 * 退料检验单Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 头表 + 检验行级联：行采用全删全插（null=本次未提交行集，不清空）。
 * 判定(judgeRqc)：锁+事务内复用 IQcJudgeService，COMPLETED 后判定域字段不可再编辑；
 * 判定完成后联动关闭 QC_CHECK 待办。
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
@Service
public class QcRqcServiceImpl implements IQcRqcService
{
    @Autowired
    private QcRqcMapper qcRqcMapper;

    @Autowired
    private QcDefectRecordMapper qcDefectRecordMapper;

    @Autowired
    private IQcOrderLineService qcOrderLineService;

    @Autowired
    private IQcJudgeService qcJudgeService;

    @Autowired
    private IQcFactoryService qcFactoryService;

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
    public List<QcRqc> selectQcRqcList(QcRqc qcrqc)
    {
        return qcRqcMapper.selectQcRqcList(qcrqc);
    }

    @Override
    public QcRqc selectQcRqcByRqcId(Long rqcId)
    {
        QcRqc rqc = qcRqcMapper.selectQcRqcByRqcId(rqcId);
        if (rqc != null)
        {
            rqc.setLines(qcOrderLineService.selectByOrder(QcConstants.TYPE_RQC, rqcId));
            rqc.setDefectRecords(qcDefectRecordMapper.selectByOrder(QcConstants.TYPE_RQC, rqcId));
        }
        return rqc;
    }

    @Override
    @Transactional
    public int insertQcRqc(QcRqc qcrqc)
    {
        validateCreate(qcrqc);
        ensureRqcCode(qcrqc);
        if (qcRqcMapper.checkRqcCodeUnique(qcrqc.getRqcCode()) != null)
        {
            throw new ServiceException("检验单编码已存在");
        }
        if (qcrqc.getStatus() == null || qcrqc.getStatus().isEmpty())
        {
            qcrqc.setStatus(QcConstants.STATUS_PENDING);
        }
        qcrqc.setCreateTime(DateUtils.getNowDate());
        int rows = qcRqcMapper.insertQcRqc(qcrqc);
        if (qcrqc.getLines() == null || qcrqc.getLines().isEmpty())
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_RQC, qcrqc.getRqcId(),
                qcFactoryService.buildLinesFromTemplate(qcrqc.getTemplateId(), QcConstants.TYPE_RQC, qcrqc.getRqcId()));
        }
        else
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_RQC, qcrqc.getRqcId(), qcrqc.getLines());
        }
        return rows;
    }

    /** 新增校验：退料类型枚举 + 物料/模板必填（退料单必须可追溯到检验标准） */
    private void validateCreate(QcRqc qcrqc)
    {
        if (qcrqc.getRqcType() == null)
        {
            qcrqc.setRqcType(QcConstants.RQC_TYPE_PROD_RETURN);
        }
        if (qcrqc.getItemId() == null)
        {
            throw new ServiceException("物料不能为空");
        }
        if (qcrqc.getTemplateId() == null)
        {
            throw new ServiceException("检验模板不能为空");
        }
    }

    @Override
    @Transactional
    public int updateQcRqc(QcRqc qcrqc)
    {
        if (qcrqc.getRqcId() == null)
        {
            throw new ServiceException("检验单主键不能为空");
        }
        QcRqc current = qcRqcMapper.selectQcRqcByRqcId(qcrqc.getRqcId());
        if (current == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_COMPLETED.equals(current.getStatus())
            || QcConstants.STATUS_CLOSED.equals(current.getStatus()))
        {
            throw new ServiceException("已判定的检验单不可编辑");
        }
        keepJudgementFields(qcrqc, current);
        qcrqc.setUpdateTime(DateUtils.getNowDate());
        int rows = qcRqcMapper.updateQcRqc(qcrqc);
        if (qcrqc.getLines() != null)
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_RQC, qcrqc.getRqcId(), qcrqc.getLines());
        }
        if (qcrqc.getDefectRecords() != null)
        {
            replaceDefectRecords(qcrqc.getRqcId(), qcrqc.getDefectRecords());
        }
        return rows;
    }

    /** 缺陷记录全删全插（null 语义在调用处判断） */
    private void replaceDefectRecords(Long rqcId, List<QcDefectRecord> defects)
    {
        qcDefectRecordMapper.deleteByOrder(QcConstants.TYPE_RQC, rqcId);
        if (defects == null || defects.isEmpty())
        {
            return;
        }
        for (QcDefectRecord defect : defects)
        {
            defect.setRecordId(null);
            defect.setQcType(QcConstants.TYPE_RQC);
            defect.setQcId(rqcId);
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
    private void keepJudgementFields(QcRqc target, QcRqc current)
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
        target.setInspectDate(current.getInspectDate());
        target.setInspector(current.getInspector());
    }

    @Override
    @Transactional
    public int deleteQcRqcByRqcId(Long rqcId)
    {
        qcOrderLineService.deleteByOrder(QcConstants.TYPE_RQC, rqcId);
        qcDefectRecordMapper.deleteByOrder(QcConstants.TYPE_RQC, rqcId);
        return qcRqcMapper.deleteQcRqcByRqcId(rqcId);
    }

    @Override
    @Transactional
    public int deleteQcRqcByRqcIds(Long[] rqcIds)
    {
        int rows = 0;
        for (Long rqcId : rqcIds)
        {
            rows += deleteQcRqcByRqcId(rqcId);
        }
        return rows;
    }

    @Override
    public List<QcRqc> listBySource(String sourceDocType, Long sourceDocId)
    {
        return qcRqcMapper.selectBySource(sourceDocType, sourceDocId, null);
    }

    @Override
    public void closeRqc(Long rqcId)
    {
        QcRqc rqc = qcRqcMapper.selectQcRqcByRqcId(rqcId);
        if (rqc == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_CLOSED.equals(rqc.getStatus()))
        {
            return;  // 幂等：已关闭直接返回
        }
        if (QcConstants.STATUS_COMPLETED.equals(rqc.getStatus()))
        {
            throw new ServiceException("已判定的检验单不可关闭（判定结果可能已驱动下游退料）");
        }
        QcRqc update = new QcRqc();
        update.setRqcId(rqcId);
        update.setStatus(QcConstants.STATUS_CLOSED);
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        qcRqcMapper.updateQcRqc(update);
    }

    /**
     * 执行判定：先锁后事务（锁防并发重复判定，事务保证 行回填+头回写+待办关闭 原子）。
     * FAIL 可携带让步理由升级为 CONCESSION；CONCESSION 必填理由。
     */
    @Override
    public void judgeRqc(Long rqcId, String concessionReason)
    {
        String lockKey = QcConstants.LOCK_JUDGE + "RQC:" + rqcId;
        lockTemplate.execute(lockKey, () -> {
            txTemplate.execute(tx -> {
                doJudgeRqc(rqcId, concessionReason);
                return null;
            });
        });
    }

    /** 锁+事务内判定：守卫 → 载入行/缺陷 → 引擎判定 → 让步处理 → 行结果回填 → 头回写 → 关闭待办 */
    private void doJudgeRqc(Long rqcId, String concessionReason)
    {
        QcRqc rqc = qcRqcMapper.selectQcRqcByRqcId(rqcId);
        if (rqc == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_COMPLETED.equals(rqc.getStatus())
            || QcConstants.STATUS_CLOSED.equals(rqc.getStatus()))
        {
            throw new ServiceException("已完成或已关闭的检验单不可判定");
        }
        List<QcOrderLine> lines = qcOrderLineService.selectByOrder(QcConstants.TYPE_RQC, rqcId);
        if (lines.isEmpty())
        {
            throw new ServiceException("检验单无检测项");
        }
        List<QcDefectRecord> defects = qcDefectRecordMapper.selectByOrder(QcConstants.TYPE_RQC, rqcId);
        QcJudgeResult r = qcJudgeService.judge(lines, defects, buildJudgeConfig(rqc));
        String finalResult = resolveFinalResult(r.getResult(), concessionReason);
        qcOrderLineService.replaceLines(QcConstants.TYPE_RQC, rqcId, lines);
        rqc.setCheckResult(finalResult);
        rqc.setConcessionReason(QcConstants.RESULT_CONCESSION.equals(finalResult) ? concessionReason : null);
        rqc.setQuantityUnqualified(BigDecimal.valueOf(r.getQuantityUnqualified()));
        rqc.setQuantityQualified(qualifiedQty(rqc.getQuantityCheck(), r.getQuantityUnqualified()));
        rqc.setCrQuantity(r.getCrQuantity());
        rqc.setMajQuantity(r.getMajQuantity());
        rqc.setMinQuantity(r.getMinQuantity());
        rqc.setCrRate(BigDecimal.valueOf(r.getCrRate()));
        rqc.setMajRate(BigDecimal.valueOf(r.getMajRate()));
        rqc.setMinRate(BigDecimal.valueOf(r.getMinRate()));
        rqc.setStatus(QcConstants.STATUS_COMPLETED);
        rqc.setInspectDate(DateUtils.getNowDate());
        rqc.setInspector(SecurityUtils.getUsername());
        qcRqcMapper.updateQcRqc(rqc);
        qcTodoHelper.completeTodo(QcConstants.TYPE_RQC, rqcId, finalResult);
    }

    /** 合格数 = 实际检测数 - 不合格数，不为负（退料数量可为小数，用 BigDecimal） */
    private BigDecimal qualifiedQty(BigDecimal checked, int unqualified)
    {
        BigDecimal base = checked == null ? BigDecimal.ZERO : checked;
        BigDecimal result = base.subtract(BigDecimal.valueOf(unqualified));
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    /** 判定配置取 RQC 头快照（Ac 值/三档缺陷率阈值）+ 实际检测数 */
    private QcJudgeConfig buildJudgeConfig(QcRqc rqc)
    {
        QcJudgeConfig cfg = new QcJudgeConfig();
        cfg.setQuantityCheck(rqc.getQuantityCheck() == null ? null : rqc.getQuantityCheck().intValue());
        cfg.setAcQuantity(rqc.getQuantityMaxUnqualified());
        cfg.setCrRateLimit(dbl(rqc.getCrRateLimit()));
        cfg.setMajRateLimit(dbl(rqc.getMajRateLimit()));
        cfg.setMinRateLimit(dbl(rqc.getMinRateLimit()));
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

    private double dbl(BigDecimal v)
    {
        return v == null ? 0d : v.doubleValue();
    }

    /** 编码缺省走 QcCodeGenerator（规则编码优先，失败时 RQC+时间戳+4位随机兜底） */
    private void ensureRqcCode(QcRqc qcrqc)
    {
        if (qcrqc.getRqcCode() != null && !qcrqc.getRqcCode().isEmpty())
        {
            return;
        }
        qcrqc.setRqcCode(QcCodeGenerator.genRqcCode(autoCodeGenerator));
    }
}
