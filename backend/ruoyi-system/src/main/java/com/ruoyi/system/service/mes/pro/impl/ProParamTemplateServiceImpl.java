package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProParamTemplateMapper;
import com.ruoyi.system.domain.mes.pro.ProParamTemplate;
import com.ruoyi.system.service.mes.pro.IProParamTemplateService;

/**
 * 工序参数模版Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProParamTemplateServiceImpl implements IProParamTemplateService
{
    @Autowired
    private ProParamTemplateMapper qxxProParamTemplateMapper;

    @Override
    public ProParamTemplate selectProParamTemplateByTemplateId(Long templateId)
    {
        return qxxProParamTemplateMapper.selectProParamTemplateByTemplateId(templateId);
    }

    @Override
    public List<ProParamTemplate> selectProParamTemplateList(ProParamTemplate proParamTemplate)
    {
        return qxxProParamTemplateMapper.selectProParamTemplateList(proParamTemplate);
    }

    @Override
    public List<ProParamTemplate> selectProParamTemplateByProcessId(Long processId)
    {
        return qxxProParamTemplateMapper.selectProParamTemplateByProcessId(processId);
    }

    @Override
    @Transactional
    public int insertProParamTemplate(ProParamTemplate proParamTemplate)
    {
        proParamTemplate.setCreateTime(DateUtils.getNowDate());
        proParamTemplate.setCreateBy(SecurityUtils.getUsername());
        if (proParamTemplate.getSortOrder() == null) {
            proParamTemplate.setSortOrder(1);
        }
        if (proParamTemplate.getIsRequired() == null) {
            proParamTemplate.setIsRequired("Y");
        }
        if (proParamTemplate.getIsReportVisible() == null) {
            proParamTemplate.setIsReportVisible("Y");
        }
        return qxxProParamTemplateMapper.insertProParamTemplate(proParamTemplate);
    }

    @Override
    public int updateProParamTemplate(ProParamTemplate proParamTemplate)
    {
        proParamTemplate.setUpdateTime(DateUtils.getNowDate());
        proParamTemplate.setUpdateBy(SecurityUtils.getUsername());
        return qxxProParamTemplateMapper.updateProParamTemplate(proParamTemplate);
    }

    @Override
    public int deleteProParamTemplateByTemplateIds(Long[] templateIds)
    {
        return qxxProParamTemplateMapper.deleteProParamTemplateByTemplateIds(templateIds);
    }

    @Override
    public int deleteProParamTemplateByTemplateId(Long templateId)
    {
        return qxxProParamTemplateMapper.deleteProParamTemplateByTemplateId(templateId);
    }

    @Override
    public int deleteProParamTemplateByProcessId(Long processId)
    {
        return qxxProParamTemplateMapper.deleteProParamTemplateByProcessId(processId);
    }

    @Override
    public boolean checkParamCodeUnique(ProParamTemplate proParamTemplate)
    {
        ProParamTemplate existing = qxxProParamTemplateMapper.selectProParamTemplateByProcessId(
                proParamTemplate.getProcessId()).stream()
                .filter(t -> t.getParamCode().equals(proParamTemplate.getParamCode()))
                .findFirst().orElse(null);
        if (existing == null) return true;
        if (existing.getTemplateId().equals(proParamTemplate.getTemplateId())) return true;
        throw new ServiceException("该工序下参数编码[" + proParamTemplate.getParamCode() + "]已存在");
    }
}
