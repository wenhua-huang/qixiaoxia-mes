package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProProcess;

/**
 * 生产工序Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProProcessService
{
    public ProProcess selectProProcessByProcessId(Long processId);

    public List<ProProcess> selectProProcessList(ProProcess proProcess);

    public List<ProProcess> selectProProcessAll();

    public int insertProProcess(ProProcess proProcess);

    public int updateProProcess(ProProcess proProcess);

    public int deleteProProcessByProcessIds(Long[] processIds);

    public int deleteProProcessByProcessId(Long processId);

    public boolean checkProcessCodeUnique(ProProcess proProcess);
}
