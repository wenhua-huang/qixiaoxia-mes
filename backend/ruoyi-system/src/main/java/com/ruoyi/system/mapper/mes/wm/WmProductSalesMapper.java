package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmProductSales;

public interface WmProductSalesMapper
{
    public List<WmProductSales> selectWmProductSalesList(WmProductSales entity);
    public List<WmProductSales> selectWmProductSalesAll();
    public WmProductSales selectWmProductSalesBySalesId(Long salesId);
    /** 按出库单编码精确查头（factory_id 由拦截器注入） */
    public WmProductSales selectWmProductSalesBySalesCode(String salesCode);
    public int insertWmProductSales(WmProductSales entity);
    public int updateWmProductSales(WmProductSales entity);
    public int deleteWmProductSalesBySalesId(Long salesId);
    public int deleteWmProductSalesBySalesIds(Long[] salesIds);
}