package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProRouteProductBomMapper;
import com.ruoyi.system.mapper.mes.md.MdItemMapper;
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
        proRouteProductBom.setCreateTime(DateUtils.getNowDate());
        proRouteProductBom.setCreateBy(SecurityUtils.getUsername());
        return qxxProRouteProductBomMapper.insertProRouteProductBom(proRouteProductBom);
    }

    @Override
    public int updateProRouteProductBom(ProRouteProductBom proRouteProductBom)
    {
        proRouteProductBom.setUpdateTime(DateUtils.getNowDate());
        proRouteProductBom.setUpdateBy(SecurityUtils.getUsername());
        return qxxProRouteProductBomMapper.updateProRouteProductBom(proRouteProductBom);
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
