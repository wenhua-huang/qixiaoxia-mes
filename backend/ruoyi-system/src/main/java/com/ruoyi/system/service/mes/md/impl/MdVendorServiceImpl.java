package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdVendor;
import com.ruoyi.system.mapper.mes.md.MdVendorMapper;
import com.ruoyi.system.service.mes.md.IMdVendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 供应商Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdVendorServiceImpl implements IMdVendorService
{
    @Autowired
    private MdVendorMapper mdVendorMapper;

    @Override
    public MdVendor selectMdVendorByVendorId(Long vendorId)
    {
        return mdVendorMapper.selectMdVendorByVendorId(vendorId);
    }

    @Override
    public boolean checkVendorCodeUnique(MdVendor mdVendor)
    {
        MdVendor vendor = mdVendorMapper.checkVendorCodeUnique(mdVendor);
        Long vendorId = mdVendor.getVendorId() == null ? -1L : mdVendor.getVendorId();
        if (StringUtils.isNotNull(vendor) && vendor.getVendorId().longValue() != vendorId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<MdVendor> selectMdVendorList(MdVendor mdVendor)
    {
        return mdVendorMapper.selectMdVendorList(mdVendor);
    }

    @Override
    public List<MdVendor> selectMdVendorAllEnabled()
    {
        return mdVendorMapper.selectMdVendorAllEnabled();
    }

    @Override
    public int insertMdVendor(MdVendor mdVendor)
    {
        mdVendor.setCreateTime(DateUtils.getNowDate());
        return mdVendorMapper.insertMdVendor(mdVendor);
    }

    @Override
    public int updateMdVendor(MdVendor mdVendor)
    {
        mdVendor.setUpdateTime(DateUtils.getNowDate());
        return mdVendorMapper.updateMdVendor(mdVendor);
    }

    @Override
    public int deleteMdVendorByVendorIds(Long[] vendorIds)
    {
        return mdVendorMapper.deleteMdVendorByVendorIds(vendorIds);
    }

    @Override
    public int deleteMdVendorByVendorId(Long vendorId)
    {
        return mdVendorMapper.deleteMdVendorByVendorId(vendorId);
    }
}
