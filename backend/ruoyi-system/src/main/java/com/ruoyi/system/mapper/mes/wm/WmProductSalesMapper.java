package com.ruoyi.system.mapper.mes.wm;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /**
     * 仅回写 OQC 挂点两列（质检工厂生成出货检验单后回填，两列专用 UPDATE 避免并发覆盖整头；
     * factory_id 由 FactoryIdInterceptor 自动注入）
     */
    public int updateSalesHeaderRefs(@Param("salesId") Long salesId, @Param("oqcId") Long oqcId, @Param("oqcCode") String oqcCode);

    public int deleteWmProductSalesBySalesId(Long salesId);
    public int deleteWmProductSalesBySalesIds(Long[] salesIds);
}