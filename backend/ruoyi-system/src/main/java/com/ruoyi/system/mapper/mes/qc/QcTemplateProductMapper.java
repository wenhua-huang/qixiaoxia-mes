package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.qc.QcTemplateProduct;

/**
 * 质检模板-物料绑定行Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入；
 * 本表无 enable_flag，"启用"以头模板 enable_flag='1' 为准）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcTemplateProductMapper
{
    public List<QcTemplateProduct> selectByTemplateId(Long templateId);

    public int insertQcTemplateProduct(QcTemplateProduct qcTemplateProduct);

    public int deleteByTemplateId(Long templateId);

    /**
     * 工序级精确查找启用绑定（生成检验单用，Java 侧先 Exact 后 Common）
     *
     * @param qcType    检验类型(IQC/IPQC/OQC/RQC)，匹配头模板 qc_types
     * @param itemId    物料ID
     * @param processId 工序ID（精确匹配 tp.process_id = ?）
     */
    public QcTemplateProduct selectEnabledBindExact(@Param("qcType") String qcType, @Param("itemId") Long itemId, @Param("processId") Long processId);

    /**
     * 通用绑定查找（无工序限制，tp.process_id IS NULL）
     */
    public QcTemplateProduct selectEnabledBindCommon(@Param("qcType") String qcType, @Param("itemId") Long itemId);

    /**
     * 按检验类型+物料查全部启用绑定（可选按工序过滤），波次 2 使用
     */
    public List<QcTemplateProduct> selectEnabledByItem(@Param("qcType") String qcType, @Param("itemId") Long itemId, @Param("processId") Long processId);

    /**
     * 统计本模板之外、同检验维度已存在的启用绑定数（保存前唯一性校验用）
     */
    public int countEnabledBindExclude(@Param("templateId") Long templateId, @Param("itemId") Long itemId,
                                        @Param("processId") Long processId, @Param("qcType") String qcType,
                                        @Param("factoryId") Long factoryId);
}
