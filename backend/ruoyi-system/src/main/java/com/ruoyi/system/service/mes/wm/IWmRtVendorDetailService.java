package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtVendorDetail;

public interface IWmRtVendorDetailService
{
    public List<WmRtVendorDetail> selectWmRtVendorDetailList(WmRtVendorDetail entity);
    public List<WmRtVendorDetail> selectWmRtVendorDetailAll();
    public WmRtVendorDetail selectWmRtVendorDetailByDetailId(Long detailId);
    public int insertWmRtVendorDetail(WmRtVendorDetail entity);
    public int updateWmRtVendorDetail(WmRtVendorDetail entity);
    public int deleteWmRtVendorDetailByDetailId(Long detailId);
    public int deleteWmRtVendorDetailByDetailIds(Long[] detailIds);
}