package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProProcess;

/**
 * 生产工序Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface ProProcessMapper
{
    public ProProcess selectProProcessByProcessId(Long processId);

    public List<ProProcess> selectProProcessList(ProProcess proProcess);

    public ProProcess selectProProcessByProcessCode(String processCode);

    public int insertProProcess(ProProcess proProcess);

    public int updateProProcess(ProProcess proProcess);

    public int deleteProProcessByProcessId(Long processId);

    public int deleteProProcessByProcessIds(Long[] processIds);
}
