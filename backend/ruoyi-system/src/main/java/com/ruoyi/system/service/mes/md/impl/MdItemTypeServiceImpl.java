package com.ruoyi.system.service.mes.md.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.TreeSelect;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdItemType;
import com.ruoyi.system.mapper.mes.md.MdItemTypeMapper;
import com.ruoyi.system.service.mes.md.IMdItemTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 物料产品分类Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdItemTypeServiceImpl implements IMdItemTypeService
{
    @Autowired
    private MdItemTypeMapper mdItemTypeMapper;

    @Override
    public List<MdItemType> selectMdItemTypeList(MdItemType mdItemType)
    {
        return mdItemTypeMapper.selectMdItemTypeList(mdItemType);
    }

    @Override
    public MdItemType selectMdItemTypeById(Long itemTypeId)
    {
        return mdItemTypeMapper.selectMdItemTypeById(itemTypeId);
    }

    @Override
    public boolean checkItemTypeCodeUnique(MdItemType mdItemType)
    {
        MdItemType itemType = mdItemTypeMapper.checkItemTypeCodeUnique(mdItemType);
        Long itemTypeId = mdItemType.getItemTypeId() == null ? -1L : mdItemType.getItemTypeId();
        if (StringUtils.isNotNull(itemType) && itemType.getItemTypeId().longValue() != itemTypeId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<TreeSelect> buildItemTypeTreeSelect(List<MdItemType> itemTypes)
    {
        List<MdItemType> trees = buildTree(itemTypes);
        return trees.stream().map(this::convertToTreeSelect).collect(Collectors.toList());
    }

    private TreeSelect convertToTreeSelect(MdItemType itemType)
    {
        TreeSelect ts = new TreeSelect();
        ts.setId(itemType.getItemTypeId());
        ts.setLabel(itemType.getItemTypeName());
        ts.setDisabled("0".equals(itemType.getEnableFlag()));
        if (itemType.getChildren() != null && !itemType.getChildren().isEmpty())
        {
            ts.setChildren(itemType.getChildren().stream().map(this::convertToTreeSelect).collect(Collectors.toList()));
        }
        return ts;
    }

    /** 构建树形结构 */
    private List<MdItemType> buildTree(List<MdItemType> list)
    {
        List<MdItemType> returnList = new ArrayList<>();
        List<Long> idList = list.stream().map(MdItemType::getItemTypeId).collect(Collectors.toList());
        for (MdItemType node : list)
        {
            // 根节点：parentTypeId 为 null 或 0
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

    /** 递归构建子节点 */
    private void recursionFn(List<MdItemType> list, MdItemType t)
    {
        List<MdItemType> children = getChildList(list, t);
        t.setChildren(children);
        for (MdItemType tChild : children)
        {
            recursionFn(list, tChild);
        }
    }

    private List<MdItemType> getChildList(List<MdItemType> list, MdItemType t)
    {
        List<MdItemType> childList = new ArrayList<>();
        for (MdItemType n : list)
        {
            if (n.getParentTypeId() != null && n.getParentTypeId().longValue() == t.getItemTypeId().longValue())
            {
                childList.add(n);
            }
        }
        return childList;
    }

    @Override
    public boolean canSetParentType(Long itemTypeId, Long parentTypeId)
    {
        if (itemTypeId == null || parentTypeId == null || parentTypeId == 0L)
        {
            return true; // 允许置为根节点
        }
        if (itemTypeId.equals(parentTypeId))
        {
            return false; // 不能选自己
        }
        List<Long> descendants = mdItemTypeMapper.selectDescendantIds(itemTypeId);
        return !descendants.contains(parentTypeId);
    }

    @Override
    public List<MdItemType> selectMdItemTypeListExcludeChild(Long itemTypeId)
    {
        List<MdItemType> allList = mdItemTypeMapper.selectMdItemTypeList(new MdItemType());
        // 获取当前节点及所有子孙节点ID
        List<Long> excludeIds = mdItemTypeMapper.selectDescendantIds(itemTypeId);
        // 移除排除的ID
        Iterator<MdItemType> it = allList.iterator();
        while (it.hasNext())
        {
            MdItemType t = it.next();
            if (excludeIds.contains(t.getItemTypeId()))
            {
                it.remove();
            }
        }
        return allList;
    }

    @Override
    public int insertMdItemType(MdItemType mdItemType)
    {
        mdItemType.setCreateTime(DateUtils.getNowDate());
        return mdItemTypeMapper.insertMdItemType(mdItemType);
    }    @Override
    public int updateMdItemType(MdItemType mdItemType)
    {
        mdItemType.setUpdateTime(DateUtils.getNowDate());
        return mdItemTypeMapper.updateMdItemType(mdItemType);
    }

    @Override
    public int deleteMdItemTypeById(Long itemTypeId)
    {
        return mdItemTypeMapper.deleteMdItemTypeById(itemTypeId);
    }

    @Override
    public int deleteMdItemTypeByIds(Long[] itemTypeIds)
    {
        return mdItemTypeMapper.deleteMdItemTypeByIds(itemTypeIds);
    }
}
