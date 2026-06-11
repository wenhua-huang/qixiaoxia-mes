package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdFactory;

/**
 * 工厂定义Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdFactoryService
{
    public List<MdFactory> selectMdFactoryList(MdFactory mdFactory);

    public MdFactory selectMdFactoryById(Long factoryId);

    public boolean checkFactoryCodeUnique(MdFactory mdFactory);

    public int insertMdFactory(MdFactory mdFactory);

    public int updateMdFactory(MdFactory mdFactory);

    public int deleteMdFactoryById(Long factoryId);

    public int deleteMdFactoryByIds(Long[] factoryIds);
}
