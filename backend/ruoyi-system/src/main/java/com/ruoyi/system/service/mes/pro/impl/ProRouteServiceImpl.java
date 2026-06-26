package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProRouteMapper;
import com.ruoyi.system.domain.mes.pro.ProRoute;
import com.ruoyi.system.service.mes.pro.IProRouteService;

/**
 * 工艺路线Service业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-18
 */
@Service
public class ProRouteServiceImpl implements IProRouteService
{
    @Autowired
    private ProRouteMapper qxxProRouteMapper;

    @Override
    public ProRoute selectProRouteByRouteId(Long routeId)
    {
        return qxxProRouteMapper.selectProRouteByRouteId(routeId);
    }

    @Override
    public List<ProRoute> selectProRouteList(ProRoute proRoute)
    {
        return qxxProRouteMapper.selectProRouteList(proRoute);
    }

    @Override
    public List<ProRoute> selectProRouteAll()
    {
        ProRoute cond = new ProRoute();
        cond.setEnableFlag("1");
        return qxxProRouteMapper.selectProRouteList(cond);
    }

    @Override
    @Transactional
    public int insertProRoute(ProRoute proRoute)
    {
        proRoute.setCreateTime(DateUtils.getNowDate());
        proRoute.setCreateBy(SecurityUtils.getUsername());
        return qxxProRouteMapper.insertProRoute(proRoute);
    }    @Override
    public int updateProRoute(ProRoute proRoute)
    {
        proRoute.setUpdateTime(DateUtils.getNowDate());
        proRoute.setUpdateBy(SecurityUtils.getUsername());
        return qxxProRouteMapper.updateProRoute(proRoute);
    }

    @Override
    public int deleteProRouteByRouteIds(Long[] routeIds)
    {
        return qxxProRouteMapper.deleteProRouteByRouteIds(routeIds);
    }

    @Override
    public int deleteProRouteByRouteId(Long routeId)
    {
        return qxxProRouteMapper.deleteProRouteByRouteId(routeId);
    }

    @Override
    public boolean checkRouteCodeUnique(ProRoute proRoute)
    {
        ProRoute existing = qxxProRouteMapper.selectProRouteByRouteCode(proRoute.getRouteCode());
        if (existing == null) return true;
        if (existing.getRouteId().equals(proRoute.getRouteId())) return true;
        throw new ServiceException("工艺路线编码[" + proRoute.getRouteCode() + "]已存在");
    }
}
