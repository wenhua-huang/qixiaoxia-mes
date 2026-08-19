package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcDefect;

/**
 * 质检缺陷字典Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcDefectMapper
{
    public List<QcDefect> selectQcDefectList(QcDefect qcDefect);

    public QcDefect selectQcDefectByDefectId(Long defectId);

    public QcDefect checkDefectCodeUnique(String defectCode);

    public int insertQcDefect(QcDefect qcDefect);

    public int updateQcDefect(QcDefect qcDefect);

    public int deleteQcDefectByDefectId(Long defectId);

    public int deleteQcDefectByDefectIds(Long[] defectIds);
}
