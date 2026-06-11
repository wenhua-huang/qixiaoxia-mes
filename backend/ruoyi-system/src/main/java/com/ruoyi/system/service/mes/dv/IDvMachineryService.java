package com.ruoyi.system.service.mes.dv;

import java.util.List;
import com.ruoyi.system.domain.mes.dv.DvMachinery;

/**
 * 设备台账Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
public interface IDvMachineryService
{
    public List<DvMachinery> selectDvMachineryList(DvMachinery dvMachinery);

    public DvMachinery selectDvMachineryById(Long machineryId);

    public boolean checkMachineryCodeUnique(DvMachinery dvMachinery);

    public int insertDvMachinery(DvMachinery dvMachinery);

    public int updateDvMachinery(DvMachinery dvMachinery);

    public int deleteDvMachineryById(Long machineryId);

    public int deleteDvMachineryByIds(Long[] machineryIds);
}
