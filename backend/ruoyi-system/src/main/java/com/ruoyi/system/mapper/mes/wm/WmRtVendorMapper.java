package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtVendor;

public interface WmRtVendorMapper
{
    public List<WmRtVendor> selectWmRtVendorList(WmRtVendor entity);
    public List<WmRtVendor> selectWmRtVendorAll();
    public WmRtVendor selectWmRtVendorByRtId(Long rtId);
    public int insertWmRtVendor(WmRtVendor entity);
    public int updateWmRtVendor(WmRtVendor entity);
    public int deleteWmRtVendorByRtId(Long rtId);
    public int deleteWmRtVendorByRtIds(Long[] rtIds);
}