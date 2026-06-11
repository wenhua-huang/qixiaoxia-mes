package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstationMachine;

/**
 * 工作站-设备Service接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdWorkstationMachineService
{
    public MdWorkstationMachine selectMdWorkstationMachineByRecordId(Long recordId);
    public List<MdWorkstationMachine> selectMdWorkstationMachineList(MdWorkstationMachine mdWorkstationMachine);
    public int insertMdWorkstationMachine(MdWorkstationMachine mdWorkstationMachine);
    public int updateMdWorkstationMachine(MdWorkstationMachine mdWorkstationMachine);
    public int deleteMdWorkstationMachineByRecordIds(Long[] recordIds);
    public int deleteMdWorkstationMachineByRecordId(Long recordId);
    public int deleteMdWorkstationMachineByWorkstationId(Long workstationId);
}
