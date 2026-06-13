package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSales;

public interface IWmProductSalesService
{
    public List<WmProductSales> selectWmProductSalesList(WmProductSales entity);
    public List<WmProductSales> selectWmProductSalesAll();
    public WmProductSales selectWmProductSalesBySalesId(Long salesId);
    public int insertWmProductSales(WmProductSales entity);
    public int updateWmProductSales(WmProductSales entity);
    public int deleteWmProductSalesBySalesId(Long salesId);
    public int deleteWmProductSalesBySalesIds(Long[] salesIds);
}