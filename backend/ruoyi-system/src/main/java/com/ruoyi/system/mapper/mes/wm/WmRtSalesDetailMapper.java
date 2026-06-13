package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtSalesDetail;

public interface WmRtSalesDetailMapper
{
    public List<WmRtSalesDetail> selectWmRtSalesDetailList(WmRtSalesDetail entity);
    public List<WmRtSalesDetail> selectWmRtSalesDetailAll();
    public WmRtSalesDetail selectWmRtSalesDetailByDetailId(Long detailId);
    public int insertWmRtSalesDetail(WmRtSalesDetail entity);
    public int updateWmRtSalesDetail(WmRtSalesDetail entity);
    public int deleteWmRtSalesDetailByDetailId(Long detailId);
    public int deleteWmRtSalesDetailByDetailIds(Long[] detailIds);
}