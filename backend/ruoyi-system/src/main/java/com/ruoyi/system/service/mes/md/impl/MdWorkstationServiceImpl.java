package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdWorkstation;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMapper;
import com.ruoyi.system.service.mes.md.IMdWorkstationMachineService;
import com.ruoyi.system.service.mes.md.IMdWorkstationService;
import com.ruoyi.system.service.mes.md.IMdWorkstationWorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工作站Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdWorkstationServiceImpl implements IMdWorkstationService
{
    @Autowired
    private MdWorkstationMapper mdWorkstationMapper;

    @Autowired
    private IMdWorkstationMachineService mdWorkstationMachineService;

    @Autowired
    private IMdWorkstationWorkerService mdWorkstationWorkerService;

    @Override
    public MdWorkstation selectMdWorkstationByWorkstationId(Long workstationId)
    {
        return mdWorkstationMapper.selectMdWorkstationByWorkstationId(workstationId);
    }

    @Override
    public boolean checkWorkstationCodeUnique(MdWorkstation mdWorkstation)
    {
        MdWorkstation workstation = mdWorkstationMapper.checkWorkstationCodeUnique(mdWorkstation);
        Long workstationId = mdWorkstation.getWorkstationId() == null ? -1L : mdWorkstation.getWorkstationId();
        if (StringUtils.isNotNull(workstation) && workstation.getWorkstationId().longValue() != workstationId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<MdWorkstation> selectMdWorkstationList(MdWorkstation mdWorkstation)
    {
        return mdWorkstationMapper.selectMdWorkstationList(mdWorkstation);
    }

    @Override
    public int insertMdWorkstation(MdWorkstation mdWorkstation)
    {
        mdWorkstation.setCreateTime(DateUtils.getNowDate());
        return mdWorkstationMapper.insertMdWorkstation(mdWorkstation);
    }

    @Override
    public int updateMdWorkstation(MdWorkstation mdWorkstation)
    {
        mdWorkstation.setUpdateTime(DateUtils.getNowDate());
        return mdWorkstationMapper.updateMdWorkstation(mdWorkstation);
    }

    @Override
    @Transactional
    public int deleteMdWorkstationByWorkstationIds(Long[] workstationIds)
    {
        for (Long workstationId : workstationIds)
        {
            mdWorkstationMachineService.deleteMdWorkstationMachineByWorkstationId(workstationId);
            mdWorkstationWorkerService.deleteMdWorkstationWorkerByWorkstationId(workstationId);
        }
        return mdWorkstationMapper.deleteMdWorkstationByWorkstationIds(workstationIds);
    }

    @Override
    @Transactional
    public int deleteMdWorkstationByWorkstationId(Long workstationId)
    {
        mdWorkstationMachineService.deleteMdWorkstationMachineByWorkstationId(workstationId);
        mdWorkstationWorkerService.deleteMdWorkstationWorkerByWorkstationId(workstationId);
        return mdWorkstationMapper.deleteMdWorkstationByWorkstationId(workstationId);
    }
}
