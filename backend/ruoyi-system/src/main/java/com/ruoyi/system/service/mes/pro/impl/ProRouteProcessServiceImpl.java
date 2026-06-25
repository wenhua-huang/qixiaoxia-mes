package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.domain.mes.pro.ProRouteProcess;
import com.ruoyi.system.service.mes.pro.IProRouteProcessService;

/**
 * 工艺路线-工序组成Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProRouteProcessServiceImpl implements IProRouteProcessService
{
    @Autowired
    private ProRouteProcessMapper qxxProRouteProcessMapper;

    @Override
    public ProRouteProcess selectProRouteProcessByRecordId(Long recordId)
    {
        return qxxProRouteProcessMapper.selectProRouteProcessByRecordId(recordId);
    }

    @Override
    public List<ProRouteProcess> selectProRouteProcessList(ProRouteProcess proRouteProcess)
    {
        return qxxProRouteProcessMapper.selectProRouteProcessList(proRouteProcess);
    }

    @Override
    public List<ProRouteProcess> selectProRouteProcessByRouteId(Long routeId)
    {
        return qxxProRouteProcessMapper.selectProRouteProcessByRouteId(routeId);
    }

    @Override
    @Transactional
    public int insertProRouteProcess(ProRouteProcess proRouteProcess)
    {
        proRouteProcess.setCreateTime(DateUtils.getNowDate());
        proRouteProcess.setCreateBy(SecurityUtils.getUsername());
        if (proRouteProcess.getLinkType() == null) {
            proRouteProcess.setLinkType("SS");
        }
        if (proRouteProcess.getColorCode() == null) {
            proRouteProcess.setColorCode("#00AEF3");
        }
        if (proRouteProcess.getKeyFlag() == null) {
            proRouteProcess.setKeyFlag("N");
        }
        if (proRouteProcess.getIsCheck() == null) {
            proRouteProcess.setIsCheck("N");
        }
        if (proRouteProcess.getIsOutsource() == null) {
            proRouteProcess.setIsOutsource("0");
        }
        return qxxProRouteProcessMapper.insertProRouteProcess(proRouteProcess);
    }

    @Override
    public int updateProRouteProcess(ProRouteProcess proRouteProcess)
    {
        proRouteProcess.setUpdateTime(DateUtils.getNowDate());
        proRouteProcess.setUpdateBy(SecurityUtils.getUsername());
        return qxxProRouteProcessMapper.updateProRouteProcess(proRouteProcess);
    }

    @Override
    public int deleteProRouteProcessByRecordIds(Long[] recordIds)
    {
        return qxxProRouteProcessMapper.deleteProRouteProcessByRecordIds(recordIds);
    }

    @Override
    public int deleteProRouteProcessByRecordId(Long recordId)
    {
        return qxxProRouteProcessMapper.deleteProRouteProcessByRecordId(recordId);
    }

    @Override
    public int deleteProRouteProcessByRouteId(Long routeId)
    {
        return qxxProRouteProcessMapper.deleteProRouteProcessByRouteId(routeId);
    }
}
