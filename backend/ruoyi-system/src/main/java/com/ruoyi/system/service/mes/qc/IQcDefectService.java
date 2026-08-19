package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcDefect;

/**
 * 质检缺陷字典Service接口
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcDefectService
{
    public List<QcDefect> selectQcDefectList(QcDefect qcDefect);

    public QcDefect selectQcDefectByDefectId(Long defectId);

    public int insertQcDefect(QcDefect qcDefect);

    public int updateQcDefect(QcDefect qcDefect);

    public int deleteQcDefectByDefectId(Long defectId);

    public int deleteQcDefectByDefectIds(Long[] defectIds);
}
