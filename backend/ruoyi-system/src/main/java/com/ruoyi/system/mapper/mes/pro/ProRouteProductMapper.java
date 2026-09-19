package com.ruoyi.system.mapper.mes.pro;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    /** 按多个物料ID批量拉绑定（批量解析默认路线用，避免逐物料查询） */
    public List<ProRouteProduct> selectByItemIds(@Param("itemIds") Collection<Long> itemIds);

    /** 按多个绑定记录ID批量拉取（订单保存批量校验手选路线用，避免逐行查询） */
    public List<ProRouteProduct> selectByRecordIds(@Param("recordIds") Collection<Long> recordIds);

    public int insertProRouteProduct(ProRouteProduct proRouteProduct);

    public int updateProRouteProduct(ProRouteProduct proRouteProduct);

    public int deleteProRouteProductByRecordId(Long recordId);

    public int deleteProRouteProductByRecordIds(Long[] recordIds);

    public int deleteProRouteProductByRouteId(Long routeId);
}
