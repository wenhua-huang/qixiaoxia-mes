package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.qc.QcIqc;

/**
 * 来料检验单Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcIqcMapper
{
    public List<QcIqc> selectQcIqcList(QcIqc qciqc);

    public QcIqc selectQcIqcByIqcId(Long iqcId);

    public QcIqc checkIqcCodeUnique(String iqcCode);

    public int insertQcIqc(QcIqc qciqc);

    public int updateQcIqc(QcIqc qciqc);

    public int deleteQcIqcByIqcId(Long iqcId);

    public int deleteQcIqcByIqcIds(Long[] iqcIds);

    /**
     * 按来源单据反查检验单（gate/factory 核心查询）
     *
     * @param sourceDocType 来源单据类型(如 wm_item_recpt)
     * @param sourceDocId   来源单据ID
     * @param itemId        物料ID(可 null：null=整单维度查全部行)
     * @return 检验单列表
     */
    public List<QcIqc> selectBySource(@Param("sourceDocType") String sourceDocType,
                                      @Param("sourceDocId") Long sourceDocId,
                                      @Param("itemId") Long itemId);
}
