package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProParamTemplate;

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

    public int insertProParamTemplate(ProParamTemplate proParamTemplate);

    public int updateProParamTemplate(ProParamTemplate proParamTemplate);

    public int deleteProParamTemplateByTemplateId(Long templateId);

    public int deleteProParamTemplateByTemplateIds(Long[] templateIds);

    public int deleteProParamTemplateByProcessId(Long processId);
}
