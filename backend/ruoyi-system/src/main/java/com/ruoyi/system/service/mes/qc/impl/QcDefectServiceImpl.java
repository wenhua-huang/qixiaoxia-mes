package com.ruoyi.system.service.mes.qc.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.qc.QcDefect;
import com.ruoyi.system.mapper.mes.qc.QcDefectMapper;
import com.ruoyi.system.service.mes.qc.IQcDefectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 质检缺陷字典Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
@Service
public class QcDefectServiceImpl implements IQcDefectService
{
    @Autowired
    private QcDefectMapper qcDefectMapper;

    @Override
    public List<QcDefect> selectQcDefectList(QcDefect qcDefect)
    {
        return qcDefectMapper.selectQcDefectList(qcDefect);
    }

    @Override
    public QcDefect selectQcDefectByDefectId(Long defectId)
    {
        return qcDefectMapper.selectQcDefectByDefectId(defectId);
    }

    @Override
    public int insertQcDefect(QcDefect qcDefect)
    {
        // 编码唯一校验（精确等值匹配，避免 LIKE 子串误判）
        if (qcDefectMapper.checkDefectCodeUnique(qcDefect.getDefectCode()) != null)
        {
            throw new ServiceException("缺陷编码已存在");
        }
        qcDefect.setCreateTime(DateUtils.getNowDate());
        return qcDefectMapper.insertQcDefect(qcDefect);
    }

    @Override
    public int updateQcDefect(QcDefect qcDefect)
    {
        qcDefect.setUpdateTime(DateUtils.getNowDate());
        return qcDefectMapper.updateQcDefect(qcDefect);
    }

    @Override
    public int deleteQcDefectByDefectId(Long defectId)
    {
        return qcDefectMapper.deleteQcDefectByDefectId(defectId);
    }

    @Override
    public int deleteQcDefectByDefectIds(Long[] defectIds)
    {
        return qcDefectMapper.deleteQcDefectByDefectIds(defectIds);
    }
}
