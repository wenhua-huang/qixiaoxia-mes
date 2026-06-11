package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdVendor;

/**
 * 供应商Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdVendorMapper
{
    public MdVendor selectMdVendorByVendorId(Long vendorId);
    public MdVendor checkVendorCodeUnique(MdVendor mdVendor);
    public List<MdVendor> selectMdVendorList(MdVendor mdVendor);
    public List<MdVendor> selectMdVendorAllEnabled();
    public int insertMdVendor(MdVendor mdVendor);
    public int updateMdVendor(MdVendor mdVendor);
    public int deleteMdVendorByVendorId(Long vendorId);
    public int deleteMdVendorByVendorIds(Long[] vendorIds);
}
