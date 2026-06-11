package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstationWorker;

/**
 * 工作站-人员Service接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdWorkstationWorkerService
{
    public MdWorkstationWorker selectMdWorkstationWorkerByRecordId(Long recordId);
    public List<MdWorkstationWorker> selectMdWorkstationWorkerList(MdWorkstationWorker mdWorkstationWorker);
    public int insertMdWorkstationWorker(MdWorkstationWorker mdWorkstationWorker);
    public int updateMdWorkstationWorker(MdWorkstationWorker mdWorkstationWorker);
    public int deleteMdWorkstationWorkerByRecordIds(Long[] recordIds);
    public int deleteMdWorkstationWorkerByRecordId(Long recordId);
    public int deleteMdWorkstationWorkerByWorkstationId(Long workstationId);
}
