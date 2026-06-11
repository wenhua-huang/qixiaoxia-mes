package com.ruoyi.system.service.mes.dv.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.dv.DvMachinery;
import com.ruoyi.system.mapper.mes.dv.DvMachineryMapper;
import com.ruoyi.system.service.mes.dv.IDvMachineryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 设备台账Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
@Service
public class DvMachineryServiceImpl implements IDvMachineryService
{
    @Autowired
    private DvMachineryMapper dvMachineryMapper;

    @Override
    public List<DvMachinery> selectDvMachineryList(DvMachinery dvMachinery)
    {
        return dvMachineryMapper.selectDvMachineryList(dvMachinery);
    }

    @Override
    public DvMachinery selectDvMachineryById(Long machineryId)
    {
        return dvMachineryMapper.selectDvMachineryById(machineryId);
    }

    @Override
    public boolean checkMachineryCodeUnique(DvMachinery dvMachinery)
    {
        DvMachinery existing = dvMachineryMapper.checkMachineryCodeUnique(dvMachinery);
        Long machineryId = dvMachinery.getMachineryId() == null ? -1L : dvMachinery.getMachineryId();
        if (StringUtils.isNotNull(existing) && existing.getMachineryId().longValue() != machineryId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int insertDvMachinery(DvMachinery dvMachinery)
    {
        dvMachinery.setCreateTime(DateUtils.getNowDate());
        return dvMachineryMapper.insertDvMachinery(dvMachinery);
    }

    @Override
    public int updateDvMachinery(DvMachinery dvMachinery)
    {
        dvMachinery.setUpdateTime(DateUtils.getNowDate());
        return dvMachineryMapper.updateDvMachinery(dvMachinery);
    }

    @Override
    public int deleteDvMachineryById(Long machineryId)
    {
        return dvMachineryMapper.deleteDvMachineryById(machineryId);
    }

    @Override
    public int deleteDvMachineryByIds(Long[] machineryIds)
    {
        return dvMachineryMapper.deleteDvMachineryByIds(machineryIds);
    }
}
