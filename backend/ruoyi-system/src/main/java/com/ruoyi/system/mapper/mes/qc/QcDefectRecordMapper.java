package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.qc.QcDefectRecord;

/**
 * 检验缺陷记录Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcDefectRecordMapper
{
    /**
     * 按单据查缺陷记录（多态 qc_type+qc_id）
     *
     * @param qcType 检验单类型(IQC/IPQC/OQC/RQC)
     * @param qcId   检验单ID
     * @return 缺陷记录列表
     */
    public List<QcDefectRecord> selectByOrder(@Param("qcType") String qcType, @Param("qcId") Long qcId);

    /**
     * 按单据删除全部缺陷记录（检验单删除时级联清理）
     *
     * @param qcType 检验单类型
     * @param qcId   检验单ID
     * @return 删除行数
     */
    public int deleteByOrder(@Param("qcType") String qcType, @Param("qcId") Long qcId);

    /**
     * 批量插入缺陷记录（与检验行同款全删全插策略；factory_id 由拦截器注入）
     *
     * @param records 缺陷记录列表（qcType/qcId 由调用方回填）
     * @return 插入行数
     */
    public int batchInsert(@Param("records") List<QcDefectRecord> records);
}
