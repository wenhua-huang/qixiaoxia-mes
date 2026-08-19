package com.ruoyi.system.mapper.mes.qc;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.qc.QcRqc;

/**
 * 退料检验单Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-17
 */
public interface QcRqcMapper
{
    public List<QcRqc> selectQcRqcList(QcRqc qcrqc);

    public QcRqc selectQcRqcByRqcId(Long rqcId);

    public QcRqc checkRqcCodeUnique(String rqcCode);

    public int insertQcRqc(QcRqc qcrqc);

    public int updateQcRqc(QcRqc qcrqc);

    public int deleteQcRqcByRqcId(Long rqcId);

    public int deleteQcRqcByRqcIds(Long[] rqcIds);

    /**
     * 按来源单据反查检验单（gate/factory 核心查询）
     *
     * @param sourceDocType 来源单据类型(wm_rt_issue)
     * @param sourceDocId   来源单据ID
     * @param itemId        物料ID(可 null：null=整单维度查全部行)
     * @return 检验单列表
     */
    public List<QcRqc> selectBySource(@Param("sourceDocType") String sourceDocType,
                                      @Param("sourceDocId") Long sourceDocId,
                                      @Param("itemId") Long itemId);

    /**
     * 按来源单据批量反查多个物料的检验单（gate/生成路径消除 N+1）
     */
    public List<QcRqc> selectBySourceItems(@Param("sourceDocType") String sourceDocType,
                                           @Param("sourceDocId") Long sourceDocId,
                                           @Param("itemIds") Collection<Long> itemIds);

    /**
     * 条件关闭：仅当单据处于 PENDING/INSPECTING 时原子置为 CLOSED。
     *
     * @param id         检验单ID
     * @param updateBy   更新人(可 null)
     * @param updateTime 更新时间
     * @return 1=成功关闭；0=已越过活动态（COMPLETED/CLOSED）
     */
    public int closeIfActive(@Param("id") Long id,
                             @Param("updateBy") String updateBy,
                             @Param("updateTime") Date updateTime);
}
