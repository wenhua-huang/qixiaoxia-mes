package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.qc.QcIpqc;

/**
 * 过程检验单Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
public interface QcIpqcMapper
{
    public List<QcIpqc> selectQcIpqcList(QcIpqc qcipqc);

    public QcIpqc selectQcIpqcByIpqcId(Long ipqcId);

    public QcIpqc checkIpqcCodeUnique(String ipqcCode);

    public int insertQcIpqc(QcIpqc qcipqc);

    public int updateQcIpqc(QcIpqc qcipqc);

    public int deleteQcIpqcByIpqcId(Long ipqcId);

    public int deleteQcIpqcByIpqcIds(Long[] ipqcIds);

    /**
     * 按来源单据反查检验单（gate/factory 核心查询）
     *
     * @param sourceDocType 来源单据类型(pro_card_process/wm_product_recpt)
     * @param sourceDocId   来源单据ID
     * @param itemId        物料ID(可 null：null=整单维度查全部行)
     * @return 检验单列表
     */
    public List<QcIpqc> selectBySource(@Param("sourceDocType") String sourceDocType,
                                       @Param("sourceDocId") Long sourceDocId,
                                       @Param("itemId") Long itemId);
}
