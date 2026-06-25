package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;

/**
 * 工艺路线产品Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProRouteProductService
{
    public ProRouteProduct selectProRouteProductByRecordId(Long recordId);

    public List<ProRouteProduct> selectProRouteProductList(ProRouteProduct proRouteProduct);

    public List<ProRouteProduct> selectProRouteProductByRouteId(Long routeId);

    public int insertProRouteProduct(ProRouteProduct proRouteProduct);

    public int updateProRouteProduct(ProRouteProduct proRouteProduct);

    public int deleteProRouteProductByRecordIds(Long[] recordIds);

    public int deleteProRouteProductByRecordId(Long recordId);

    public int deleteProRouteProductByRouteId(Long routeId);

    /**
     * 为SKU变体复制父产品的工艺路线关联（含BOM + 参数）
     */
    public int copyRouteProductForSku(Long parentItemId, Long skuItemId, String skuItemCode, String skuItemName);
}
