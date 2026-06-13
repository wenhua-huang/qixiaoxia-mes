package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtVendorLine;

public interface WmRtVendorLineMapper
{
    public List<WmRtVendorLine> selectWmRtVendorLineList(WmRtVendorLine entity);
    public List<WmRtVendorLine> selectWmRtVendorLineAll();
    public WmRtVendorLine selectWmRtVendorLineByLineId(Long lineId);
    public int insertWmRtVendorLine(WmRtVendorLine entity);
    public int updateWmRtVendorLine(WmRtVendorLine entity);
    public int deleteWmRtVendorLineByLineId(Long lineId);
    public int deleteWmRtVendorLineByLineIds(Long[] lineIds);
}