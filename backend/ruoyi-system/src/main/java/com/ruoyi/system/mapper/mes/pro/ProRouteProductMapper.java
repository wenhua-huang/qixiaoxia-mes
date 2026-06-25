package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;

/**
 * 工艺路线产品Mapper接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface ProRouteProductMapper
{
    public ProRouteProduct selectProRouteProductByRecordId(Long recordId);

    public List<ProRouteProduct> selectProRouteProductList(ProRouteProduct proRouteProduct);

    public List<ProRouteProduct> selectProRouteProductByRouteId(Long routeId);

    public int insertProRouteProduct(ProRouteProduct proRouteProduct);

    public int updateProRouteProduct(ProRouteProduct proRouteProduct);

    public int deleteProRouteProductByRecordId(Long recordId);

    public int deleteProRouteProductByRecordIds(Long[] recordIds);

    public int deleteProRouteProductByRouteId(Long routeId);
}
