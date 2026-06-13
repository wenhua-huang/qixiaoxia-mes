package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmBatch;

public interface IWmBatchService
{
    /** 获取或生成批次号（物料启用批次管理时自动生成，未启用返回null） */
    WmBatch getOrGenerateBatchCode(WmBatch params);

    public List<WmBatch> selectWmBatchList(WmBatch entity);
    public List<WmBatch> selectWmBatchAll();
    public WmBatch selectWmBatchByBatchId(Long batchId);
    public int insertWmBatch(WmBatch entity);
    public int updateWmBatch(WmBatch entity);
    public int deleteWmBatchByBatchId(Long batchId);
    public int deleteWmBatchByBatchIds(Long[] batchIds);
}