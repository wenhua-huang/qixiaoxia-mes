package com.ruoyi.system.mapper.mes.dv;

import java.util.List;
import com.ruoyi.system.domain.mes.dv.DvMachineryType;

/**
 * 设备类型Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
public interface DvMachineryTypeMapper
{
    public List<DvMachineryType> selectDvMachineryTypeList(DvMachineryType dvMachineryType);

    public DvMachineryType selectDvMachineryTypeById(Long machineryTypeId);

    public DvMachineryType checkMachineryTypeCodeUnique(DvMachineryType dvMachineryType);

    /** 查询指定父ID下的子节点列表 */
    public List<DvMachineryType> selectDvMachineryTypeByParentId(Long parentTypeId);

    /** 递归查询指定节点及所有子孙节点（MySQL 8.0 递归CTE） */
    public List<Long> selectDescendantIds(Long machineryTypeId);

    public int insertDvMachineryType(DvMachineryType dvMachineryType);

    public int updateDvMachineryType(DvMachineryType dvMachineryType);

    public int deleteDvMachineryTypeById(Long machineryTypeId);

    public int deleteDvMachineryTypeByIds(Long[] machineryTypeIds);
}
