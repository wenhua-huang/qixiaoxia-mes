package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProParamTemplate;

/**
 * 工序参数模版Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProParamTemplateService
{
    public ProParamTemplate selectProParamTemplateByTemplateId(Long templateId);

    public List<ProParamTemplate> selectProParamTemplateList(ProParamTemplate proParamTemplate);

    public List<ProParamTemplate> selectProParamTemplateByProcessId(Long processId);

    public int insertProParamTemplate(ProParamTemplate proParamTemplate);

    public int updateProParamTemplate(ProParamTemplate proParamTemplate);

    public int deleteProParamTemplateByTemplateIds(Long[] templateIds);

    public int deleteProParamTemplateByTemplateId(Long templateId);

    public int deleteProParamTemplateByProcessId(Long processId);

    public boolean checkParamCodeUnique(ProParamTemplate proParamTemplate);
}
