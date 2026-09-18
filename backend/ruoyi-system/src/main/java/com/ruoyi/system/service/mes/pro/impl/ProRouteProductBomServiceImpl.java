package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductBomMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductMapper;
import com.ruoyi.system.mapper.mes.pro.ProRouteProcessMapper;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
import com.ruoyi.system.domain.mes.pro.ProRouteProduct;
import com.ruoyi.system.domain.mes.pro.ProRouteProductBom;
import com.ruoyi.system.domain.mes.md.MdItem;
import com.ruoyi.system.service.mes.pro.IProRouteProductBomService;

/**
 * 工艺路线产品BOMService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProRouteProductBomServiceImpl implements IProRouteProductBomService
{
    @Autowired
    private ProRouteProductBomMapper qxxProRouteProductBomMapper;

    @Autowired
    private MdItemMapper qxxMdItemMapper;

    @Autowired
    private ProRouteProductMapper proRouteProductMapper;

    @Autowired
    private ProRouteProcessMapper proRouteProcessMapper;

    @Override
    public ProRouteProductBom selectProRouteProductBomByRecordId(Long recordId)
    {
        return qxxProRouteProductBomMapper.selectProRouteProductBomByRecordId(recordId);
    }

    @Override
    public List<ProRouteProductBom> selectProRouteProductBomList(ProRouteProductBom proRouteProductBom)
    {
        return qxxProRouteProductBomMapper.selectProRouteProductBomList(proRouteProductBom);
    }

    @Override
    public List<ProRouteProductBom> selectProRouteProductBomByRouteId(Long routeId)
    {
        return qxxProRouteProductBomMapper.selectProRouteProductBomByRouteId(routeId);
    }

    @Override
    @Transactional
    public int insertProRouteProductBom(ProRouteProductBom proRouteProductBom)
    {
        // 校验物料是否存在
        Long itemId = proRouteProductBom.getItemId();
        if (itemId != null)
        {
            MdItem mdItem = qxxMdItemMapper.selectMdItemById(itemId);
            if (mdItem == null)
            {
                throw new ServiceException("物料ID[" + itemId + "]不存在");
            }
        }
        validateRouteOwnership(proRouteProductBom, null);
        proRouteProductBom.setCreateTime(DateUtils.getNowDate());
        proRouteProductBom.setCreateBy(SecurityUtils.getUsername());
        return qxxProRouteProductBomMapper.insertProRouteProductBom(proRouteProductBom);
    }

    @Override
    public int updateProRouteProductBom(ProRouteProductBom proRouteProductBom)
    {
        ProRouteProductBom existing = proRouteProductBom.getRecordId() == null ? null
                : qxxProRouteProductBomMapper.selectProRouteProductBomByRecordId(proRouteProductBom.getRecordId());
        validateRouteOwnership(proRouteProductBom, existing);
        proRouteProductBom.setUpdateTime(DateUtils.getNowDate());
        proRouteProductBom.setUpdateBy(SecurityUtils.getUsername());
        return qxxProRouteProductBomMapper.updateProRouteProductBom(proRouteProductBom);
    }

    /**
     * 校验 BOM 行的产品/工序必须归属于所填工艺路线，防止产品归属串位写入脏数据
     *
     * @param bom      待写入 BOM 行；routeId/productId/processId 为 null 的字段不校验
     * @param existing 更新场景的存量行，用于补全请求中未传的 routeId；新增场景传 null
     */
    private void validateRouteOwnership(ProRouteProductBom bom, ProRouteProductBom existing)
    {
        Long routeId = bom.getRouteId() != null ? bom.getRouteId()
                : (existing != null ? existing.getRouteId() : null);
        if (routeId == null)
        {
            throw new ServiceException("工艺路线ID不能为空");
        }

        Long productId = bom.getProductId() != null ? bom.getProductId()
                : (existing != null ? existing.getProductId() : null);
        if (productId != null)
        {
            List<ProRouteProduct> products = proRouteProductMapper.selectProRouteProductByRouteId(routeId);
            boolean productBound = products != null && products.stream()
                    .anyMatch(p -> productId.equals(p.getItemId()));
            if (!productBound)
            {
                throw new ServiceException("产品未关联到该工艺路线，不能维护BOM，请先在关联产品中添加");
            }
        }

        Long processId = bom.getProcessId() != null ? bom.getProcessId()
                : (existing != null ? existing.getProcessId() : null);
        if (processId != null && proRouteProcessMapper.selectByRouteAndProcess(routeId, processId) == null)
        {
            throw new ServiceException("所选工序不属于该工艺路线，请重新选择");
        }
    }

    @Override
    public int deleteProRouteProductBomByRecordIds(Long[] recordIds)
    {
        return qxxProRouteProductBomMapper.deleteProRouteProductBomByRecordIds(recordIds);
    }

    @Override
    public int deleteProRouteProductBomByRecordId(Long recordId)
    {
        return qxxProRouteProductBomMapper.deleteProRouteProductBomByRecordId(recordId);
    }

    @Override
    public int deleteProRouteProductBomByRouteId(Long routeId)
    {
        return qxxProRouteProductBomMapper.deleteProRouteProductBomByRouteId(routeId);
    }

    @Override
    public int deleteByRouteIdAndProductId(Long routeId, Long productId)
    {
        ProRouteProductBom query = new ProRouteProductBom();
        query.setRouteId(routeId);
        query.setProductId(productId);
        List<ProRouteProductBom> list = qxxProRouteProductBomMapper.selectProRouteProductBomList(query);
        if (list == null || list.isEmpty()) return 0;
        Long[] ids = list.stream().map(ProRouteProductBom::getRecordId).toArray(Long[]::new);
        return qxxProRouteProductBomMapper.deleteProRouteProductBomByRecordIds(ids);
    }
}
