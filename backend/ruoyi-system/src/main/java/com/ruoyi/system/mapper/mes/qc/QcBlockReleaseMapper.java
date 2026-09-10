package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.qc.QcBlockRelease;

/**
 * 质检不合格拦截-放行记录Mapper（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 */
public interface QcBlockReleaseMapper
{
    /** 新增放行记录（factory_id 不写，由拦截器注入） */
    public int insertQcBlockRelease(QcBlockRelease release);

    /**
     * 指定 IPQC + 被拦任务是否已放行（门控逐条任务判定）
     */
    public boolean existsByIpqcAndTask(@Param("ipqcId") Long ipqcId,
                                      @Param("targetTaskId") Long targetTaskId);

    /**
     * 指定 IPQC 是否已有任一放行记录（taskId 缺失的兜底退化判定）
     */
    public boolean existsByIpqc(@Param("ipqcId") Long ipqcId);

    /**
     * 查某 IPQC 的全部放行记录（追溯/排查用）
     */
    public List<QcBlockRelease> selectByIpqc(@Param("ipqcId") Long ipqcId);
}
