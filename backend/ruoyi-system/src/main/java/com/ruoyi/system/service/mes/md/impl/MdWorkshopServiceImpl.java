package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdWorkshop;
import com.ruoyi.system.mapper.mes.md.MdWorkshopMapper;
import com.ruoyi.system.service.mes.md.IMdWorkshopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 车间管理Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdWorkshopServiceImpl implements IMdWorkshopService
{
    @Autowired
    private MdWorkshopMapper mdWorkshopMapper;

    @Override
    public List<MdWorkshop> selectMdWorkshopList(MdWorkshop mdWorkshop)
    {
        return mdWorkshopMapper.selectMdWorkshopList(mdWorkshop);
    }

    @Override
    public MdWorkshop selectMdWorkshopById(Long workshopId)
    {
        return mdWorkshopMapper.selectMdWorkshopById(workshopId);
    }

    @Override
    public boolean checkWorkshopCodeUnique(MdWorkshop mdWorkshop)
    {
        MdWorkshop workshop = mdWorkshopMapper.checkWorkshopCodeUnique(mdWorkshop);
        Long workshopId = mdWorkshop.getWorkshopId() == null ? -1L : mdWorkshop.getWorkshopId();
        if (StringUtils.isNotNull(workshop) && workshop.getWorkshopId().longValue() != workshopId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int insertMdWorkshop(MdWorkshop mdWorkshop)
    {
        mdWorkshop.setCreateTime(DateUtils.getNowDate());
        return mdWorkshopMapper.insertMdWorkshop(mdWorkshop);
    }

    @Override
    public int updateMdWorkshop(MdWorkshop mdWorkshop)
    {
        mdWorkshop.setUpdateTime(DateUtils.getNowDate());
        return mdWorkshopMapper.updateMdWorkshop(mdWorkshop);
    }

    @Override
    public int deleteMdWorkshopById(Long workshopId)
    {
        return mdWorkshopMapper.deleteMdWorkshopById(workshopId);
    }

    @Override
    public int deleteMdWorkshopByIds(Long[] workshopIds)
    {
        return mdWorkshopMapper.deleteMdWorkshopByIds(workshopIds);
    }
}
