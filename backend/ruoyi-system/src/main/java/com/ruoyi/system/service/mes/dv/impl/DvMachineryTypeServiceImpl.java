package com.ruoyi.system.service.mes.dv.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.TreeSelect;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.dv.DvMachineryType;
import com.ruoyi.system.mapper.mes.dv.DvMachineryTypeMapper;
import com.ruoyi.system.service.mes.dv.IDvMachineryTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 设备类型Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
@Service
public class DvMachineryTypeServiceImpl implements IDvMachineryTypeService
{
    @Autowired
    private DvMachineryTypeMapper dvMachineryTypeMapper;

    @Override
    public List<DvMachineryType> selectDvMachineryTypeList(DvMachineryType dvMachineryType)
    {
        return dvMachineryTypeMapper.selectDvMachineryTypeList(dvMachineryType);
    }

    @Override
    public DvMachineryType selectDvMachineryTypeById(Long machineryTypeId)
    {
        return dvMachineryTypeMapper.selectDvMachineryTypeById(machineryTypeId);
    }

    @Override
    public boolean checkMachineryTypeCodeUnique(DvMachineryType dvMachineryType)
    {
        DvMachineryType type = dvMachineryTypeMapper.checkMachineryTypeCodeUnique(dvMachineryType);
        Long typeId = dvMachineryType.getMachineryTypeId() == null ? -1L : dvMachineryType.getMachineryTypeId();
        if (StringUtils.isNotNull(type) && type.getMachineryTypeId().longValue() != typeId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<TreeSelect> buildMachineryTypeTreeSelect(List<DvMachineryType> list)
    {
        List<DvMachineryType> trees = buildTree(list);
        return trees.stream().map(this::convertToTreeSelect).collect(Collectors.toList());
    }

    private TreeSelect convertToTreeSelect(DvMachineryType type)
    {
        TreeSelect ts = new TreeSelect();
        ts.setId(type.getMachineryTypeId());
        ts.setLabel(type.getMachineryTypeName());
        ts.setDisabled("0".equals(type.getEnableFlag()));
        if (type.getChildren() != null && !type.getChildren().isEmpty())
        {
            ts.setChildren(type.getChildren().stream().map(this::convertToTreeSelect).collect(Collectors.toList()));
        }
        return ts;
    }

    /** 构建树形结构 */
    private List<DvMachineryType> buildTree(List<DvMachineryType> list)
    {
        List<DvMachineryType> returnList = new ArrayList<>();
        for (DvMachineryType node : list)
        {
            if (node.getParentTypeId() == null || node.getParentTypeId() == 0L)
            {
                recursionFn(list, node);
                returnList.add(node);
            }
        }
        if (returnList.isEmpty())
        {
            returnList = list;
        }
        return returnList;
    }

    private void recursionFn(List<DvMachineryType> list, DvMachineryType t)
    {
        List<DvMachineryType> children = getChildList(list, t);
        t.setChildren(children);
        for (DvMachineryType tChild : children)
        {
            recursionFn(list, tChild);
        }
    }

    private List<DvMachineryType> getChildList(List<DvMachineryType> list, DvMachineryType t)
    {
        List<DvMachineryType> childList = new ArrayList<>();
        for (DvMachineryType n : list)
        {
            if (n.getParentTypeId() != null && n.getParentTypeId().longValue() == t.getMachineryTypeId().longValue())
            {
                childList.add(n);
            }
        }
        return childList;
    }

    @Override
    public boolean canSetParentType(Long machineryTypeId, Long parentTypeId)
    {
        if (machineryTypeId == null || parentTypeId == null || parentTypeId == 0L)
        {
            return true;
        }
        if (machineryTypeId.equals(parentTypeId))
        {
            return false;
        }
        List<Long> descendants = dvMachineryTypeMapper.selectDescendantIds(machineryTypeId);
        return !descendants.contains(parentTypeId);
    }

    @Override
    public List<DvMachineryType> selectDvMachineryTypeByParentId(Long parentTypeId)
    {
        return dvMachineryTypeMapper.selectDvMachineryTypeByParentId(parentTypeId);
    }

    @Override
    public List<Long> selectDescendantIds(Long machineryTypeId)
    {
        return dvMachineryTypeMapper.selectDescendantIds(machineryTypeId);
    }

    @Override
    public List<DvMachineryType> selectListExcludeChild(Long machineryTypeId)
    {
        List<DvMachineryType> allList = dvMachineryTypeMapper.selectDvMachineryTypeList(new DvMachineryType());
        List<Long> excludeIds = dvMachineryTypeMapper.selectDescendantIds(machineryTypeId);
        Iterator<DvMachineryType> it = allList.iterator();
        while (it.hasNext())
        {
            DvMachineryType t = it.next();
            if (excludeIds.contains(t.getMachineryTypeId()))
            {
                it.remove();
            }
        }
        return allList;
    }

    @Override
    public int insertDvMachineryType(DvMachineryType dvMachineryType)
    {
        dvMachineryType.setCreateTime(DateUtils.getNowDate());
        return dvMachineryTypeMapper.insertDvMachineryType(dvMachineryType);
    }    @Override
    public int updateDvMachineryType(DvMachineryType dvMachineryType)
    {
        dvMachineryType.setUpdateTime(DateUtils.getNowDate());
        return dvMachineryTypeMapper.updateDvMachineryType(dvMachineryType);
    }

    @Override
    public int deleteDvMachineryTypeById(Long machineryTypeId)
    {
        return dvMachineryTypeMapper.deleteDvMachineryTypeById(machineryTypeId);
    }

    @Override
    public int deleteDvMachineryTypeByIds(Long[] machineryTypeIds)
    {
        return dvMachineryTypeMapper.deleteDvMachineryTypeByIds(machineryTypeIds);
    }
}
