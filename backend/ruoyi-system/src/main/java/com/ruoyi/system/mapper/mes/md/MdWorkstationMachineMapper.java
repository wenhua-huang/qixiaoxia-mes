package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstationMachine;

/**
 * 工作站-设备Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdWorkstationMachineMapper
{
    public MdWorkstationMachine selectMdWorkstationMachineByRecordId(Long recordId);
    public List<MdWorkstationMachine> selectMdWorkstationMachineList(MdWorkstationMachine mdWorkstationMachine);
    public int insertMdWorkstationMachine(MdWorkstationMachine mdWorkstationMachine);
    public int updateMdWorkstationMachine(MdWorkstationMachine mdWorkstationMachine);
    public int deleteMdWorkstationMachineByRecordId(Long recordId);
    public int deleteMdWorkstationMachineByRecordIds(Long[] recordIds);
    public int deleteMdWorkstationMachineByWorkstationId(Long workstationId);
}
