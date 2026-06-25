package com.ruoyi.system.service.mes.pro.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.pro.ProMaterialTraceMapper;
import com.ruoyi.system.domain.mes.pro.ProMaterialTrace;
import com.ruoyi.system.service.mes.pro.IProMaterialTraceService;

/**
 * ProMaterialTraceService业务层处理
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
@Service
public class ProMaterialTraceServiceImpl implements IProMaterialTraceService
{
    @Autowired
    private ProMaterialTraceMapper proMaterialTraceMapper;

    @Override
    public ProMaterialTrace selectProMaterialTraceByTraceId(Long traceId) { return proMaterialTraceMapper.selectProMaterialTraceByTraceId(traceId); }

    @Override
    public List<ProMaterialTrace> selectProMaterialTraceList(ProMaterialTrace e) { return proMaterialTraceMapper.selectProMaterialTraceList(e); }

    @Override
    public List<ProMaterialTrace> selectAll() { return proMaterialTraceMapper.selectProMaterialTraceList(new ProMaterialTrace()); }

    @Override
    @Transactional
    public int insertProMaterialTrace(ProMaterialTrace e) {
        e.setCreateTime(DateUtils.getNowDate());
        e.setCreateBy(SecurityUtils.getUsername());
        return proMaterialTraceMapper.insertProMaterialTrace(e);
    }

    @Override
    public int updateProMaterialTrace(ProMaterialTrace e) {
        e.setUpdateTime(DateUtils.getNowDate());
        e.setUpdateBy(SecurityUtils.getUsername());
        return proMaterialTraceMapper.updateProMaterialTrace(e);
    }

    @Override
    public int deleteProMaterialTraceByTraceIds(Long[] traceIds) { return proMaterialTraceMapper.deleteProMaterialTraceByTraceIds(traceIds); }

    @Override
    public int deleteProMaterialTraceByTraceId(Long traceId) { return proMaterialTraceMapper.deleteProMaterialTraceByTraceId(traceId); }

    @Override
    public List<ProMaterialTrace> traceForward(String parentType, Long parentId) {
        return proMaterialTraceMapper.selectByParent(parentType, parentId);
    }

    @Override
    public List<ProMaterialTrace> traceBackward(String childType, Long childId) {
        return proMaterialTraceMapper.selectByChild(childType, childId);
    }
}
