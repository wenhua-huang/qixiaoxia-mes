package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdVendor;

/**
 * 供应商Service接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdVendorService
{
    public MdVendor selectMdVendorByVendorId(Long vendorId);
    public boolean checkVendorCodeUnique(MdVendor mdVendor);
    public List<MdVendor> selectMdVendorList(MdVendor mdVendor);
    public List<MdVendor> selectMdVendorAllEnabled();
        public int insertMdVendor(MdVendor mdVendor);    public int updateMdVendor(MdVendor mdVendor);
    public int deleteMdVendorByVendorIds(Long[] vendorIds);
    public int deleteMdVendorByVendorId(Long vendorId);
}
