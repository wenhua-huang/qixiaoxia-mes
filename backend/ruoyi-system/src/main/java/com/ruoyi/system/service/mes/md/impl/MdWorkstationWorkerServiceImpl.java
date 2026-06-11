package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.md.MdWorkstationWorker;
import com.ruoyi.system.mapper.mes.md.MdWorkstationWorkerMapper;
import com.ruoyi.system.service.mes.md.IMdWorkstationWorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工作站-人员Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdWorkstationWorkerServiceImpl implements IMdWorkstationWorkerService
{
    @Autowired
    private MdWorkstationWorkerMapper mdWorkstationWorkerMapper;

    @Override
    public MdWorkstationWorker selectMdWorkstationWorkerByRecordId(Long recordId)
    {
        return mdWorkstationWorkerMapper.selectMdWorkstationWorkerByRecordId(recordId);
    }

    @Override
    public List<MdWorkstationWorker> selectMdWorkstationWorkerList(MdWorkstationWorker mdWorkstationWorker)
    {
        return mdWorkstationWorkerMapper.selectMdWorkstationWorkerList(mdWorkstationWorker);
    }

    @Override
    public int insertMdWorkstationWorker(MdWorkstationWorker mdWorkstationWorker)
    {
        if (mdWorkstationWorker.getUserId() == null) mdWorkstationWorker.setUserId(0L);
        mdWorkstationWorker.setCreateTime(DateUtils.getNowDate());
        return mdWorkstationWorkerMapper.insertMdWorkstationWorker(mdWorkstationWorker);
    }

    @Override
    public int updateMdWorkstationWorker(MdWorkstationWorker mdWorkstationWorker)
    {
        mdWorkstationWorker.setUpdateTime(DateUtils.getNowDate());
        return mdWorkstationWorkerMapper.updateMdWorkstationWorker(mdWorkstationWorker);
    }

    @Override
    public int deleteMdWorkstationWorkerByRecordIds(Long[] recordIds)
    {
        return mdWorkstationWorkerMapper.deleteMdWorkstationWorkerByRecordIds(recordIds);
    }

    @Override
    public int deleteMdWorkstationWorkerByRecordId(Long recordId)
    {
        return mdWorkstationWorkerMapper.deleteMdWorkstationWorkerByRecordId(recordId);
    }

    @Override
    public int deleteMdWorkstationWorkerByWorkstationId(Long workstationId)
    {
        return mdWorkstationWorkerMapper.deleteMdWorkstationWorkerByWorkstationId(workstationId);
    }
}
