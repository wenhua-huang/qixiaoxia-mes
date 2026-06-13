package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtSales;

public interface IWmRtSalesService
{
    public List<WmRtSales> selectWmRtSalesList(WmRtSales entity);
    public List<WmRtSales> selectWmRtSalesAll();
    public WmRtSales selectWmRtSalesByRtId(Long rtId);
    public int insertWmRtSales(WmRtSales entity);
    public int updateWmRtSales(WmRtSales entity);
    public int deleteWmRtSalesByRtId(Long rtId);
    public int deleteWmRtSalesByRtIds(Long[] rtIds);
}