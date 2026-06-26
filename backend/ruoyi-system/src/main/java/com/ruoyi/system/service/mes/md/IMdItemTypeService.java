package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.common.core.domain.TreeSelect;
import com.ruoyi.system.domain.mes.md.MdItemType;

/**
 * 物料产品分类Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface IMdItemTypeService
{
    public List<MdItemType> selectMdItemTypeList(MdItemType mdItemType);

    public MdItemType selectMdItemTypeById(Long itemTypeId);

    public boolean checkItemTypeCodeUnique(MdItemType mdItemType);

    /** 构建树形选择（前端 treeselect 用） */
    public List<TreeSelect> buildItemTypeTreeSelect(List<MdItemType> itemTypes);

    /** 检查父类型是否合法（不能选自身或子孙节点） */
    public boolean canSetParentType(Long itemTypeId, Long parentTypeId);

    /** 排除自身及子孙节点（编辑时防止循环引用） */
    public List<MdItemType> selectMdItemTypeListExcludeChild(Long itemTypeId);

        public int insertMdItemType(MdItemType mdItemType);
    public int updateMdItemType(MdItemType mdItemType);

    public int deleteMdItemTypeById(Long itemTypeId);

    public int deleteMdItemTypeByIds(Long[] itemTypeIds);
}
