package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.ProRouteProductBom;

/**
 * 工艺路线产品BOMService接口
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
public interface IProRouteProductBomService
{
    public ProRouteProductBom selectProRouteProductBomByRecordId(Long recordId);

    public List<ProRouteProductBom> selectProRouteProductBomList(ProRouteProductBom proRouteProductBom);

    public List<ProRouteProductBom> selectProRouteProductBomByRouteId(Long routeId);

    public int insertProRouteProductBom(ProRouteProductBom proRouteProductBom);

    public int updateProRouteProductBom(ProRouteProductBom proRouteProductBom);

    public int deleteProRouteProductBomByRecordIds(Long[] recordIds);

    public int deleteProRouteProductBomByRecordId(Long recordId);

    public int deleteProRouteProductBomByRouteId(Long routeId);

    /** 按路线ID和产品ID删除BOM行（用于SKU变体路线BOM替换） */
    public int deleteByRouteIdAndProductId(Long routeId, Long productId);
}
