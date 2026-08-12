package com.ruoyi.system.mapper.mes.pro;

import java.util.Collection;
import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProProcess;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 按 processId 集合批量查工序（消除偏差检测 N+1，factory_id 由拦截器注入）。
     */
    public List<ProProcess> selectByProcessIds(@Param("processIds") Collection<Long> processIds);

    public int insertProProcess(ProProcess proProcess);

    public int updateProProcess(ProProcess proProcess);

    public int deleteProProcessByProcessId(Long processId);

    public int deleteProProcessByProcessIds(Long[] processIds);
}
