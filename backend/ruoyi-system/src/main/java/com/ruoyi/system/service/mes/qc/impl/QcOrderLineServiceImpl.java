package com.ruoyi.system.service.mes.qc.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;
import com.ruoyi.system.mapper.mes.qc.QcOrderLineMapper;
import com.ruoyi.system.service.mes.qc.IQcOrderLineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检验单行Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * 行编辑采用全删全插策略（行数少、编辑频度低，替换策略最简单且无孤儿行）。
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@Service
public class QcOrderLineServiceImpl implements IQcOrderLineService
{
    @Autowired
    private QcOrderLineMapper qcOrderLineMapper;

    @Override
    public List<QcOrderLine> selectByOrder(String qcType, Long qcId)
    {
        return qcOrderLineMapper.selectByOrder(qcType, qcId);
    }

    @Override
    @Transactional
    public int replaceLines(String qcType, Long qcId, List<QcOrderLine> lines)
    {
        qcOrderLineMapper.deleteByOrder(qcType, qcId);
        if (lines == null || lines.isEmpty())
        {
            return 0;
        }
        for (QcOrderLine line : lines)
        {
            line.setLineId(null);
            line.setQcType(qcType);
            line.setQcId(qcId);
            if (line.getCreateTime() == null)
            {
                line.setCreateTime(DateUtils.getNowDate());
            }
        }
        return qcOrderLineMapper.batchInsert(lines);
    }

    @Override
    public int deleteByOrder(String qcType, Long qcId)
    {
        return qcOrderLineMapper.deleteByOrder(qcType, qcId);
    }
}
