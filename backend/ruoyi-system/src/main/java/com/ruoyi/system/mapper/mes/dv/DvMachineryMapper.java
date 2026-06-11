package com.ruoyi.system.mapper.mes.dv;

import java.util.List;
import com.ruoyi.system.domain.mes.dv.DvMachinery;

/**
 * 设备台账Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
public interface DvMachineryMapper
{
    public List<DvMachinery> selectDvMachineryList(DvMachinery dvMachinery);

    public DvMachinery selectDvMachineryById(Long machineryId);

    public DvMachinery checkMachineryCodeUnique(DvMachinery dvMachinery);

    public int insertDvMachinery(DvMachinery dvMachinery);

    public int updateDvMachinery(DvMachinery dvMachinery);

    public int deleteDvMachineryById(Long machineryId);

    public int deleteDvMachineryByIds(Long[] machineryIds);
}
