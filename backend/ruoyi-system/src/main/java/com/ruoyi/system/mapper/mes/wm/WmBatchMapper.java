package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmBatch;

public interface WmBatchMapper
{
    /** 按批次属性查重：(item_id, vendor_id, produce_date, expire_date, lot_number) */
    WmBatch selectBatchByAttributes(WmBatch params);

    public List<WmBatch> selectWmBatchList(WmBatch entity);
    public List<WmBatch> selectWmBatchAll();
    public WmBatch selectWmBatchByBatchId(Long batchId);
    public int insertWmBatch(WmBatch entity);
    public int updateWmBatch(WmBatch entity);
    public int deleteWmBatchByBatchId(Long batchId);
    public int deleteWmBatchByBatchIds(Long[] batchIds);
}