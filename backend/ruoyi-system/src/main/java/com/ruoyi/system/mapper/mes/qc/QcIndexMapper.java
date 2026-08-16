package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcIndex;

/**
 * 质检检测项Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcIndexMapper
{
    public List<QcIndex> selectQcIndexList(QcIndex qcIndex);

    public QcIndex selectQcIndexByIndexId(Long indexId);

    public QcIndex checkIndexCodeUnique(String indexCode);

    public int insertQcIndex(QcIndex qcIndex);

    public int updateQcIndex(QcIndex qcIndex);

    public int deleteQcIndexByIndexId(Long indexId);

    public int deleteQcIndexByIndexIds(Long[] indexIds);
}
