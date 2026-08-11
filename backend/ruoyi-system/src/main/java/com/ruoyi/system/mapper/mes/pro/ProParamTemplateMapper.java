package com.ruoyi.system.mapper.mes.pro;

import java.util.Collection;
import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProParamTemplate;
import org.apache.ibatis.annotations.Param;

/**
 * 工序参数模版Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface ProParamTemplateMapper
{
    public ProParamTemplate selectProParamTemplateByTemplateId(Long templateId);

    public List<ProParamTemplate> selectProParamTemplateList(ProParamTemplate proParamTemplate);

    public List<ProParamTemplate> selectProParamTemplateByProcessId(Long processId);

    /**
     * 按 templateId 集合批量查参数模板（消除偏差检测 N+1，factory_id 由拦截器注入）。
     */
    public List<ProParamTemplate> selectByTemplateIds(@Param("templateIds") Collection<Long> templateIds);

    public int insertProParamTemplate(ProParamTemplate proParamTemplate);

    public int updateProParamTemplate(ProParamTemplate proParamTemplate);

    public int deleteProParamTemplateByTemplateId(Long templateId);

    public int deleteProParamTemplateByTemplateIds(Long[] templateIds);

    public int deleteProParamTemplateByProcessId(Long processId);
}
