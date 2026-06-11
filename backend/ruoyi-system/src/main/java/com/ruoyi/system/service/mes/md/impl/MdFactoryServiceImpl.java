package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdFactory;
import com.ruoyi.system.mapper.mes.md.MdFactoryMapper;
import com.ruoyi.system.service.mes.md.IMdFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工厂定义Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdFactoryServiceImpl implements IMdFactoryService
{
    @Autowired
    private MdFactoryMapper mdFactoryMapper;

    @Override
    public List<MdFactory> selectMdFactoryList(MdFactory mdFactory)
    {
        return mdFactoryMapper.selectMdFactoryList(mdFactory);
    }

    @Override
    public MdFactory selectMdFactoryById(Long factoryId)
    {
        return mdFactoryMapper.selectMdFactoryById(factoryId);
    }

    @Override
    public boolean checkFactoryCodeUnique(MdFactory mdFactory)
    {
        MdFactory factory = mdFactoryMapper.checkFactoryCodeUnique(mdFactory);
        Long factoryId = mdFactory.getFactoryId() == null ? -1L : mdFactory.getFactoryId();
        if (StringUtils.isNotNull(factory) && factory.getFactoryId().longValue() != factoryId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int insertMdFactory(MdFactory mdFactory)
    {
        mdFactory.setCreateTime(DateUtils.getNowDate());
        return mdFactoryMapper.insertMdFactory(mdFactory);
    }

    @Override
    public int updateMdFactory(MdFactory mdFactory)
    {
        mdFactory.setUpdateTime(DateUtils.getNowDate());
        return mdFactoryMapper.updateMdFactory(mdFactory);
    }

    @Override
    public int deleteMdFactoryById(Long factoryId)
    {
        return mdFactoryMapper.deleteMdFactoryById(factoryId);
    }

    @Override
    public int deleteMdFactoryByIds(Long[] factoryIds)
    {
        return mdFactoryMapper.deleteMdFactoryByIds(factoryIds);
    }
}
