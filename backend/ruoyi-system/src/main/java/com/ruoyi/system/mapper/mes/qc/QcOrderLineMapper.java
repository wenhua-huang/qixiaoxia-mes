package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.qc.QcOrderLine;

/**
 * 检验单行Mapper接口（IQC/IPQC/OQC/RQC 四类单据共用；factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcOrderLineMapper
{
    /**
     * 按单据查检验行（多态 qc_type+qc_id）
     *
     * @param qcType 检验单类型(IQC/IPQC/OQC/RQC)
     * @param qcId   检验单ID
     * @return 检验行列表（按 order_num 排序）
     */
    public List<QcOrderLine> selectByOrder(@Param("qcType") String qcType, @Param("qcId") Long qcId);

    /**
     * 批量插入检验行（factory 按模板生成行快照用；不回填自增主键）
     *
     * @param lines 检验行列表（调用方须已设置 qcType/qcId）
     * @return 插入行数
     */
    public int batchInsert(@Param("lines") List<QcOrderLine> lines);

    /**
     * 按单据删除全部检验行
     *
     * @param qcType 检验单类型
     * @param qcId   检验单ID
     * @return 删除行数
     */
    public int deleteByOrder(@Param("qcType") String qcType, @Param("qcId") Long qcId);
}
