package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.mes.pro.ProChangeover;
import com.ruoyi.system.mapper.mes.pro.ProChangeoverMapper;
import com.ruoyi.system.service.mes.pro.IProChangeoverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工序换型时间Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-27
 */
@Service
public class ProChangeoverServiceImpl implements IProChangeoverService
{
    @Autowired
    private ProChangeoverMapper proChangeoverMapper;

    @Override
    public ProChangeover selectProChangeoverById(Long id)
    {
        return proChangeoverMapper.selectProChangeoverById(id);
    }

    @Override
    public List<ProChangeover> selectProChangeoverList(ProChangeover proChangeover)
    {
        return proChangeoverMapper.selectProChangeoverList(proChangeover);
    }

    @Override
    public Integer getChangeoverMinutes(Long fromProcessId, Long toProcessId, Long workstationId, Long factoryId)
    {
        if (fromProcessId == null || toProcessId == null) {
            return 0;
        }
        ProChangeover query = new ProChangeover();
        query.setFromProcessId(fromProcessId);
        query.setToProcessId(toProcessId);
        query.setWorkstationId(workstationId);
        query.setFactoryId(factoryId);
        ProChangeover result = proChangeoverMapper.selectChangeover(query);
        return result != null ? result.getDurationMinutes() : 0;
    }

    @Override
    @Transactional
    public int insertProChangeover(ProChangeover proChangeover)
    {
        proChangeover.setCreateTime(DateUtils.getNowDate());
        proChangeover.setCreateBy(SecurityUtils.getUsername());
        return proChangeoverMapper.insertProChangeover(proChangeover);
    }

    @Override
    public int updateProChangeover(ProChangeover proChangeover)
    {
        proChangeover.setUpdateTime(DateUtils.getNowDate());
        proChangeover.setUpdateBy(SecurityUtils.getUsername());
        return proChangeoverMapper.updateProChangeover(proChangeover);
    }

    @Override
    public int deleteProChangeoverByIds(Long[] ids)
    {
        return proChangeoverMapper.deleteProChangeoverByIds(ids);
    }
}
