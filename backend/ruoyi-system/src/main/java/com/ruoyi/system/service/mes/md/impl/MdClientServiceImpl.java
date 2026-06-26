package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdClient;
import com.ruoyi.system.mapper.mes.md.MdClientMapper;
import com.ruoyi.system.service.mes.md.IMdClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 客户Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdClientServiceImpl implements IMdClientService
{
    @Autowired
    private MdClientMapper mdClientMapper;

    @Override
    public MdClient selectMdClientByClientId(Long clientId)
    {
        return mdClientMapper.selectMdClientByClientId(clientId);
    }

    @Override
    public boolean checkClientCodeUnique(MdClient mdClient)
    {
        MdClient client = mdClientMapper.checkClientCodeUnique(mdClient);
        Long clientId = mdClient.getClientId() == null ? -1L : mdClient.getClientId();
        if (StringUtils.isNotNull(client) && client.getClientId().longValue() != clientId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<MdClient> selectMdClientList(MdClient mdClient)
    {
        return mdClientMapper.selectMdClientList(mdClient);
    }

    @Override
    public List<MdClient> selectMdClientAllEnabled()
    {
        return mdClientMapper.selectMdClientAllEnabled();
    }

    @Override
    public int insertMdClient(MdClient mdClient)
    {
        mdClient.setCreateTime(DateUtils.getNowDate());
        return mdClientMapper.insertMdClient(mdClient);
    }    @Override
    public int updateMdClient(MdClient mdClient)
    {
        mdClient.setUpdateTime(DateUtils.getNowDate());
        return mdClientMapper.updateMdClient(mdClient);
    }

    @Override
    public int deleteMdClientByClientIds(Long[] clientIds)
    {
        return mdClientMapper.deleteMdClientByClientIds(clientIds);
    }

    @Override
    public int deleteMdClientByClientId(Long clientId)
    {
        return mdClientMapper.deleteMdClientByClientId(clientId);
    }
}
