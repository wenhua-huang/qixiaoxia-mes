package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdItemBatchConfig;

/**
 * 物料批次属性配置Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 */
public interface MdItemBatchConfigMapper
{
    public List<MdItemBatchConfig> selectMdItemBatchConfigList(MdItemBatchConfig config);
    public MdItemBatchConfig selectByItemId(Long itemId);
    public MdItemBatchConfig selectMdItemBatchConfigById(Long configId);
    public int insertMdItemBatchConfig(MdItemBatchConfig config);
    public int updateMdItemBatchConfig(MdItemBatchConfig config);
    public int deleteMdItemBatchConfigById(Long configId);
    public int deleteMdItemBatchConfigByIds(Long[] configIds);
    public int deleteByItemId(Long itemId);
}
