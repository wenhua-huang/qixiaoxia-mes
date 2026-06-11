package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdWorkstationWorker;

/**
 * 工作站-人员Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdWorkstationWorkerMapper
{
    public MdWorkstationWorker selectMdWorkstationWorkerByRecordId(Long recordId);
    public List<MdWorkstationWorker> selectMdWorkstationWorkerList(MdWorkstationWorker mdWorkstationWorker);
    public int insertMdWorkstationWorker(MdWorkstationWorker mdWorkstationWorker);
    public int updateMdWorkstationWorker(MdWorkstationWorker mdWorkstationWorker);
    public int deleteMdWorkstationWorkerByRecordId(Long recordId);
    public int deleteMdWorkstationWorkerByRecordIds(Long[] recordIds);
    public int deleteMdWorkstationWorkerByWorkstationId(Long workstationId);
}
