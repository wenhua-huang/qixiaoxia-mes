package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.mes.md.MdItemVendor;
import com.ruoyi.system.mapper.mes.md.MdItemVendorMapper;
import com.ruoyi.system.service.mes.md.IMdItemVendorService;

/**
 * 物料供应商关系Service实现
 *
 * @author qixiaoxia
 * @date 2026-07-01
 */
@Service
public class MdItemVendorServiceImpl implements IMdItemVendorService
{
    @Autowired
    private MdItemVendorMapper mapper;

    @Override
    public MdItemVendor selectByRecordId(Long recordId) { return mapper.selectByRecordId(recordId); }

    @Override
    public List<MdItemVendor> selectList(MdItemVendor query) { return mapper.selectList(query); }

    @Override
    public List<MdItemVendor> selectByVendorId(Long vendorId) { return mapper.selectByVendorId(vendorId); }

    @Override
    public List<MdItemVendor> selectByItemId(Long itemId) { return mapper.selectByItemId(itemId); }

    @Override
    public int insert(MdItemVendor entity) { return mapper.insert(entity); }

    @Override
    public int update(MdItemVendor entity) { return mapper.update(entity); }

    @Override
    public int deleteByRecordIds(Long[] recordIds) { return mapper.deleteByRecordIds(recordIds); }
}
