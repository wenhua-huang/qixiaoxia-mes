package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdItemType;

/**
 * 物料产品分类Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public interface MdItemTypeMapper
{
    public List<MdItemType> selectMdItemTypeList(MdItemType mdItemType);

    public MdItemType selectMdItemTypeById(Long itemTypeId);

    public MdItemType checkItemTypeCodeUnique(MdItemType mdItemType);

    /** 查询指定父ID下的子节点列表 */
    public List<MdItemType> selectMdItemTypeByParentId(Long parentTypeId);

    /** 递归查询指定节点及所有子孙节点（MySQL 8.0 递归CTE） */
    public List<Long> selectDescendantIds(Long itemTypeId);

    public int insertMdItemType(MdItemType mdItemType);

    public int updateMdItemType(MdItemType mdItemType);

    public int deleteMdItemTypeById(Long itemTypeId);

    public int deleteMdItemTypeByIds(Long[] itemTypeIds);
}
