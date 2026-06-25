package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessParamMapper;
import com.ruoyi.system.mapper.mes.pro.ProParamTemplateMapper;
import com.ruoyi.system.domain.mes.pro.ProRouteProcessParam;
import com.ruoyi.system.domain.mes.pro.ProParamTemplate;
import com.ruoyi.system.service.mes.pro.IProRouteProcessParamService;

/**
 * 工艺路线工序参数Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProRouteProcessParamServiceImpl implements IProRouteProcessParamService
{
    @Autowired
    private ProRouteProcessParamMapper qxxProRouteProcessParamMapper;

    @Autowired
    private ProParamTemplateMapper qxxProParamTemplateMapper;

    @Override
    public ProRouteProcessParam selectProRouteProcessParamByRecordId(Long recordId)
    {
        return qxxProRouteProcessParamMapper.selectProRouteProcessParamByRecordId(recordId);
    }

    @Override
    public List<ProRouteProcessParam> selectProRouteProcessParamList(ProRouteProcessParam proRouteProcessParam)
    {
        return qxxProRouteProcessParamMapper.selectProRouteProcessParamList(proRouteProcessParam);
    }

    @Override
    public List<ProRouteProcessParam> selectProRouteProcessParamByRouteProductId(Long routeProductId)
    {
        return qxxProRouteProcessParamMapper.selectProRouteProcessParamByRouteProductId(routeProductId);
    }

    @Override
    @Transactional
    public int insertProRouteProcessParam(ProRouteProcessParam proRouteProcessParam)
    {
        proRouteProcessParam.setCreateTime(DateUtils.getNowDate());
        proRouteProcessParam.setCreateBy(SecurityUtils.getUsername());
        return qxxProRouteProcessParamMapper.insertProRouteProcessParam(proRouteProcessParam);
    }

    @Override
    public int updateProRouteProcessParam(ProRouteProcessParam proRouteProcessParam)
    {
        proRouteProcessParam.setUpdateTime(DateUtils.getNowDate());
        proRouteProcessParam.setUpdateBy(SecurityUtils.getUsername());
        return qxxProRouteProcessParamMapper.updateProRouteProcessParam(proRouteProcessParam);
    }

    @Override
    public int deleteProRouteProcessParamByRecordIds(Long[] recordIds)
    {
        return qxxProRouteProcessParamMapper.deleteProRouteProcessParamByRecordIds(recordIds);
    }

    @Override
    public int deleteProRouteProcessParamByRecordId(Long recordId)
    {
        return qxxProRouteProcessParamMapper.deleteProRouteProcessParamByRecordId(recordId);
    }

    @Override
    public int deleteProRouteProcessParamByRouteProductId(Long routeProductId)
    {
        return qxxProRouteProcessParamMapper.deleteProRouteProcessParamByRouteProductId(routeProductId);
    }

    @Override
    @Transactional
    public int batchInsertFromTemplate(Long routeProductId, Long processId)
    {
        // 查询工序对应的参数模版列表
        List<ProParamTemplate> templates = qxxProParamTemplateMapper.selectProParamTemplateByProcessId(processId);
        if (templates == null || templates.isEmpty())
        {
            return 0;
        }

        int count = 0;
        for (ProParamTemplate template : templates)
        {
            ProRouteProcessParam param = new ProRouteProcessParam();
            param.setRouteProductId(routeProductId);
            param.setProcessId(processId);
            param.setTemplateId(template.getTemplateId());
            param.setParamValue(template.getDefaultValue());
            param.setCreateTime(DateUtils.getNowDate());
            param.setCreateBy(SecurityUtils.getUsername());
            qxxProRouteProcessParamMapper.insertProRouteProcessParam(param);
            count++;
        }
        return count;
    }

    @Override
    @Transactional
    public int batchUpdate(List<ProRouteProcessParam> list) {
        int count = 0;
        for (ProRouteProcessParam p : list) {
            p.setUpdateTime(DateUtils.getNowDate());
            p.setUpdateBy(SecurityUtils.getUsername());
            count += qxxProRouteProcessParamMapper.updateProRouteProcessParam(p);
        }
        return count;
    }
}
