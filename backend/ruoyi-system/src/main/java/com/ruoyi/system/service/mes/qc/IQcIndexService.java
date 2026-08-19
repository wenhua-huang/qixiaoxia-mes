package com.ruoyi.system.service.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcIndex;

/**
 * 质检检测项Service接口
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface IQcIndexService
{
    public List<QcIndex> selectQcIndexList(QcIndex qcIndex);

    public QcIndex selectQcIndexByIndexId(Long indexId);

    public int insertQcIndex(QcIndex qcIndex);

    public int updateQcIndex(QcIndex qcIndex);

    public int deleteQcIndexByIndexId(Long indexId);

    public int deleteQcIndexByIndexIds(Long[] indexIds);
}
