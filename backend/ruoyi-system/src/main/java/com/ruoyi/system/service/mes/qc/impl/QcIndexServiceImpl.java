package com.ruoyi.system.service.mes.qc.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.qc.QcIndex;
import com.ruoyi.system.mapper.mes.qc.QcIndexMapper;
import com.ruoyi.system.service.mes.qc.IQcIndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 质检检测项Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@Service
public class QcIndexServiceImpl implements IQcIndexService
{
    @Autowired
    private QcIndexMapper qcIndexMapper;

    @Override
    public List<QcIndex> selectQcIndexList(QcIndex qcIndex)
    {
        return qcIndexMapper.selectQcIndexList(qcIndex);
    }

    @Override
    public QcIndex selectQcIndexByIndexId(Long indexId)
    {
        return qcIndexMapper.selectQcIndexByIndexId(indexId);
    }

    @Override
    public int insertQcIndex(QcIndex qcIndex)
    {
        // 编码唯一校验
        QcIndex query = new QcIndex();
        query.setIndexCode(qcIndex.getIndexCode());
        if (qcIndexMapper.selectQcIndexList(query).size() > 0)
        {
            throw new ServiceException("检测项编码已存在");
        }
        qcIndex.setCreateTime(DateUtils.getNowDate());
        return qcIndexMapper.insertQcIndex(qcIndex);
    }

    @Override
    public int updateQcIndex(QcIndex qcIndex)
    {
        qcIndex.setUpdateTime(DateUtils.getNowDate());
        return qcIndexMapper.updateQcIndex(qcIndex);
    }

    @Override
    public int deleteQcIndexByIndexId(Long indexId)
    {
        return qcIndexMapper.deleteQcIndexByIndexId(indexId);
    }

    @Override
    public int deleteQcIndexByIndexIds(Long[] indexIds)
    {
        return qcIndexMapper.deleteQcIndexByIndexIds(indexIds);
    }
}
