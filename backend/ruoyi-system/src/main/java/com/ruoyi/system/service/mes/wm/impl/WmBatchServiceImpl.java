package com.ruoyi.system.service.mes.wm.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.wm.WmBatch;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.mapper.mes.wm.WmBatchMapper;
import com.ruoyi.system.service.mes.sys.generator.AutoCodeGenerator;
import com.ruoyi.system.service.mes.wm.IWmBatchService;

@Service
public class WmBatchServiceImpl implements IWmBatchService
{
    @Autowired
    private WmBatchMapper wmBatchMapper;

    @Autowired
    private MdItemMapper mdItemMapper;

    @Autowired
    private AutoCodeGenerator autoCodeGenerator;

    @Override
    public List<WmBatch> selectWmBatchList(WmBatch entity) {
        return wmBatchMapper.selectWmBatchList(entity);
    }

    @Override
    public List<WmBatch> selectWmBatchAll() {
        return wmBatchMapper.selectWmBatchAll();
    }

    @Override
    public WmBatch selectWmBatchByBatchId(Long batchId) {
        return wmBatchMapper.selectWmBatchByBatchId(batchId);
    }

    @Override
    @Transactional
    public int insertWmBatch(WmBatch entity) {
        entity.setCreateTime(DateUtils.getNowDate());
        return wmBatchMapper.insertWmBatch(entity);
    }

    @Override
    @Transactional
    public int updateWmBatch(WmBatch entity) {
        entity.setUpdateTime(DateUtils.getNowDate());
        return wmBatchMapper.updateWmBatch(entity);
    }

    @Override
    @Transactional
    public int deleteWmBatchByBatchId(Long batchId) {
        return wmBatchMapper.deleteWmBatchByBatchId(batchId);
    }

    @Override
    @Transactional
    public int deleteWmBatchByBatchIds(Long[] batchIds) {
        return wmBatchMapper.deleteWmBatchByBatchIds(batchIds);
    }

    /**
     * 获取或生成批次号。
     * 物料启用批次管理 → 按 (item_id+vendor_id+produce_date+expire_date+lot_number) 查重
     * → 命中则复用，未命中则 genSerialCode("BATCH_CODE") 生成新批次。
     * 物料未启用批次管理 → 返回 null。
     */
    @Override
    @Transactional
    public WmBatch getOrGenerateBatchCode(WmBatch params) {
        // 1. 检查物料是否启用批次管理
        var item = mdItemMapper.selectMdItemById(params.getItemId());
        if (item == null || !"1".equals(item.getBatchFlag())) return null;

        // 2. 查重
        WmBatch existing = wmBatchMapper.selectBatchByAttributes(params);
        if (existing != null) return existing;

        // 3. 生成新批次号
        String code = autoCodeGenerator.genSerialCode("BATCH_CODE", "");
        params.setBatchCode(code);
        params.setStatus("ACTIVE");
        params.setCreateTime(DateUtils.getNowDate());
        wmBatchMapper.insertWmBatch(params);
        return params;
    }
}