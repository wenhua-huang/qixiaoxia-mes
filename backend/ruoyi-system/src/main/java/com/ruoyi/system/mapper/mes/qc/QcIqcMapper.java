package com.ruoyi.system.mapper.mes.qc;

import java.util.Collection;
import java.util.Date;
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

    /**
     * 按来源单据批量反查多个物料的检验单（gate/生成路径消除 N+1）
     */
    public List<QcIqc> selectBySourceItems(@Param("sourceDocType") String sourceDocType,
                                           @Param("sourceDocId") Long sourceDocId,
                                           @Param("itemIds") Collection<Long> itemIds);

    /**
     * 条件关闭：仅当单据处于 PENDING/INSPECTING 时原子置为 CLOSED。
     * 用数据库行级条件消除"读状态→改状态"的 TOCTOU 竞态（与判定 COMPLETED 并发时判定获胜）。
     *
     * @param id         检验单ID
     * @param updateBy   更新人(可 null)
     * @param updateTime 更新时间
     * @return 受影响行数：1=成功关闭；0=已越过活动态（COMPLETED/CLOSED），调用方应重读判定
     */
    public int closeIfActive(@Param("id") Long id,
                             @Param("updateBy") String updateBy,
                             @Param("updateTime") Date updateTime);
}
