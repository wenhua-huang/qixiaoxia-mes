package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.mes.md.MdItemBatchConfig;
import com.ruoyi.system.mapper.mes.md.MdItemBatchConfigMapper;
import com.ruoyi.system.service.mes.md.IMdItemBatchConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 物料批次属性配置Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdItemBatchConfigServiceImpl implements IMdItemBatchConfigService
{
    @Autowired
    private MdItemBatchConfigMapper mapper;

    @Override
    public List<MdItemBatchConfig> selectMdItemBatchConfigList(MdItemBatchConfig config)
    {
        return mapper.selectMdItemBatchConfigList(config);
    }

    @Override
    public MdItemBatchConfig selectMdItemBatchConfigByItemId(Long itemId)
    {
        return mapper.selectByItemId(itemId);
    }

    @Override
    public MdItemBatchConfig selectMdItemBatchConfigById(Long configId)
    {
        return mapper.selectMdItemBatchConfigById(configId);
    }

    @Override
    public int insertMdItemBatchConfig(MdItemBatchConfig config)
    {
        config.setCreateTime(DateUtils.getNowDate());
        return mapper.insertMdItemBatchConfig(config);
    }

    @Override
    public int updateMdItemBatchConfig(MdItemBatchConfig config)
    {
        config.setUpdateTime(DateUtils.getNowDate());
        return mapper.updateMdItemBatchConfig(config);
    }

    @Override
    public int deleteMdItemBatchConfigByIds(Long[] configIds)
    {
        return mapper.deleteMdItemBatchConfigByIds(configIds);
    }
}
