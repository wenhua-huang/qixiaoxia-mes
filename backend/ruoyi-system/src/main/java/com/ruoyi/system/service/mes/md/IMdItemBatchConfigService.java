package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdItemBatchConfig;

/**
 * 物料批次属性配置Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdItemBatchConfigService
{
    public List<MdItemBatchConfig> selectMdItemBatchConfigList(MdItemBatchConfig config);
    public MdItemBatchConfig selectMdItemBatchConfigByItemId(Long itemId);
    public MdItemBatchConfig selectMdItemBatchConfigById(Long configId);
    public int insertMdItemBatchConfig(MdItemBatchConfig config);
    public int updateMdItemBatchConfig(MdItemBatchConfig config);
    public int deleteMdItemBatchConfigByIds(Long[] configIds);
}
