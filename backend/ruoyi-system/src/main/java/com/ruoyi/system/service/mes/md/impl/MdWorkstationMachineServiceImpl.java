package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.md.MdWorkstationMachine;
import com.ruoyi.system.mapper.mes.md.MdWorkstationMachineMapper;
import com.ruoyi.system.service.mes.md.IMdWorkstationMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工作站-设备Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdWorkstationMachineServiceImpl implements IMdWorkstationMachineService
{
    @Autowired
    private MdWorkstationMachineMapper mdWorkstationMachineMapper;

    @Override
    public MdWorkstationMachine selectMdWorkstationMachineByRecordId(Long recordId)
    {
        return mdWorkstationMachineMapper.selectMdWorkstationMachineByRecordId(recordId);
    }

    @Override
    public List<MdWorkstationMachine> selectMdWorkstationMachineList(MdWorkstationMachine mdWorkstationMachine)
    {
        return mdWorkstationMachineMapper.selectMdWorkstationMachineList(mdWorkstationMachine);
    }

    @Override
    public int insertMdWorkstationMachine(MdWorkstationMachine mdWorkstationMachine)
    {
        if (mdWorkstationMachine.getMachineryId() == null) mdWorkstationMachine.setMachineryId(0L);
        mdWorkstationMachine.setCreateTime(DateUtils.getNowDate());
        return mdWorkstationMachineMapper.insertMdWorkstationMachine(mdWorkstationMachine);
    }

    @Override
    public int updateMdWorkstationMachine(MdWorkstationMachine mdWorkstationMachine)
    {
        mdWorkstationMachine.setUpdateTime(DateUtils.getNowDate());
        return mdWorkstationMachineMapper.updateMdWorkstationMachine(mdWorkstationMachine);
    }

    @Override
    public int deleteMdWorkstationMachineByRecordIds(Long[] recordIds)
    {
        return mdWorkstationMachineMapper.deleteMdWorkstationMachineByRecordIds(recordIds);
    }

    @Override
    public int deleteMdWorkstationMachineByRecordId(Long recordId)
    {
        return mdWorkstationMachineMapper.deleteMdWorkstationMachineByRecordId(recordId);
    }

    @Override
    public int deleteMdWorkstationMachineByWorkstationId(Long workstationId)
    {
        return mdWorkstationMachineMapper.deleteMdWorkstationMachineByWorkstationId(workstationId);
    }
}
