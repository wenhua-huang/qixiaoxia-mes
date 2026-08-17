package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.qc.QcOqc;

/**
 * 出货检验单Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcOqcMapper
{
    public List<QcOqc> selectQcOqcList(QcOqc qcoqc);

    public QcOqc selectQcOqcByOqcId(Long oqcId);

    public QcOqc checkOqcCodeUnique(String oqcCode);

    public int insertQcOqc(QcOqc qcoqc);

    public int updateQcOqc(QcOqc qcoqc);

    public int deleteQcOqcByOqcId(Long oqcId);

    public int deleteQcOqcByOqcIds(Long[] oqcIds);

    /**
     * 按来源单据反查检验单（gate/factory 核心查询）
     *
     * @param sourceDocType 来源单据类型(如 wm_product_sales)
     * @param sourceDocId   来源单据ID
     * @param itemId        物料ID(可 null：null=整单维度查全部行)
     * @return 检验单列表
     */
    public List<QcOqc> selectBySource(@Param("sourceDocType") String sourceDocType,
                                      @Param("sourceDocId") Long sourceDocId,
                                      @Param("itemId") Long itemId);
}
