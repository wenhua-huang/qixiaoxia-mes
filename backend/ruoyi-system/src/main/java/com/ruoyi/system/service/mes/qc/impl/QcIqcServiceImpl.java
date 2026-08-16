package com.ruoyi.system.service.mes.qc.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.mapper.mes.qc.QcDefectRecordMapper;
import com.ruoyi.system.mapper.mes.qc.QcIqcMapper;
import com.ruoyi.system.service.mes.qc.IQcIqcService;
import com.ruoyi.system.service.mes.qc.IQcOrderLineService;
import com.ruoyi.system.service.mes.qc.QcCodeGenerator;
import com.ruoyi.system.service.mes.qc.QcConstants;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 来料检验单Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 头表 + 检验行级联：行采用全删全插（null=本次未提交行集，不清空，防仅改头字段时误删行）。
 * 判定逻辑(judgeIqc)在后续任务实现。
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

    @Autowired(required = false)
    private AutoCodeGenerator autoCodeGenerator;

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
        qciqc.setUpdateTime(DateUtils.getNowDate());
        int rows = qcIqcMapper.updateQcIqc(qciqc);
        // 行集 null=本次未提交，不清空（与模板头行级联同一保护策略）
        if (qciqc.getLines() != null)
        {
            qcOrderLineService.replaceLines(QcConstants.TYPE_IQC, qciqc.getIqcId(), qciqc.getLines());
        }
        return rows;
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
        QcIqc iqc = qcIqcMapper.selectQcIqcByIqcId(iqcId);
        if (iqc == null)
        {
            throw new ServiceException("检验单不存在");
        }
        if (QcConstants.STATUS_CLOSED.equals(iqc.getStatus()))
        {
            throw new ServiceException("检验单已关闭，无需重复操作");
        }
        QcIqc update = new QcIqc();
        update.setIqcId(iqcId);
        update.setStatus(QcConstants.STATUS_CLOSED);
        update.setUpdateBy(SecurityUtils.getUsername());
        update.setUpdateTime(DateUtils.getNowDate());
        qcIqcMapper.updateQcIqc(update);
    }

    @Override
    public void judgeIqc(Long iqcId, String concessionReason)
    {
        // 桩实现：完整判定逻辑（行结果回填/缺陷汇总/三档缺陷率/CONCESSION 必填校验）在后续判定任务交付
        throw new UnsupportedOperationException("judgeIqc 待判定任务实现");
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
