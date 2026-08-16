package com.ruoyi.system.mapper.mes.qc;

import java.util.List;
import com.ruoyi.system.domain.mes.qc.QcTemplateIndex;

/**
 * 质检模板-检测项行Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-16
 */
public interface QcTemplateIndexMapper
{
    public List<QcTemplateIndex> selectByTemplateId(Long templateId);

    public int insertQcTemplateIndex(QcTemplateIndex qcTemplateIndex);

    public int deleteByTemplateId(Long templateId);
}
