package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.common.annotation.SkipFactoryId;
import com.ruoyi.system.domain.mes.md.MdFactory;

/**
 * 工厂定义Mapper接口（所有方法加 @SkipFactoryId — 工厂是租户隔离的根，不应被拦截器注入/过滤）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdFactoryMapper
{
    @SkipFactoryId
    public List<MdFactory> selectMdFactoryList(MdFactory mdFactory);

    @SkipFactoryId
    public MdFactory selectMdFactoryById(Long factoryId);

    @SkipFactoryId
    public MdFactory checkFactoryCodeUnique(MdFactory mdFactory);

    @SkipFactoryId
    public int insertMdFactory(MdFactory mdFactory);

    @SkipFactoryId
    public int updateMdFactory(MdFactory mdFactory);

    @SkipFactoryId
    public int deleteMdFactoryById(Long factoryId);

    @SkipFactoryId
    public int deleteMdFactoryByIds(Long[] factoryIds);
}
