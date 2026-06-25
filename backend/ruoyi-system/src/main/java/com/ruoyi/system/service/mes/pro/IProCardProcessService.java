package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProCardProcess;

/**
 * ProCardProcessService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IProCardProcessService
{
    public ProCardProcess selectProCardProcessByRecordId(Long recordId);
    public List<ProCardProcess> selectProCardProcessList(ProCardProcess e);
    public List<ProCardProcess> selectAll();
    public int insertProCardProcess(ProCardProcess e);
    public int updateProCardProcess(ProCardProcess e);
    public int deleteProCardProcessByRecordIds(Long[] recordIds);
    public int deleteProCardProcessByRecordId(Long recordId);
}
