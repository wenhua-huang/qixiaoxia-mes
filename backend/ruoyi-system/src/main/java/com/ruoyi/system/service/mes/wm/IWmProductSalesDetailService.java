package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSalesDetail;

public interface IWmProductSalesDetailService
{
    public List<WmProductSalesDetail> selectWmProductSalesDetailList(WmProductSalesDetail entity);
    public List<WmProductSalesDetail> selectWmProductSalesDetailAll();
    public WmProductSalesDetail selectWmProductSalesDetailByDetailId(Long detailId);
    public int insertWmProductSalesDetail(WmProductSalesDetail entity);
    public int updateWmProductSalesDetail(WmProductSalesDetail entity);
    public int deleteWmProductSalesDetailByDetailId(Long detailId);
    public int deleteWmProductSalesDetailByDetailIds(Long[] detailIds);
}