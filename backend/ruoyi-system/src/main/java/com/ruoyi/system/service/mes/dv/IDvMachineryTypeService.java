package com.ruoyi.system.service.mes.dv;

import java.util.List;
import com.ruoyi.common.core.domain.TreeSelect;
import com.ruoyi.system.domain.mes.dv.DvMachineryType;

/**
 * 设备类型Service接口
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
public interface IDvMachineryTypeService
{
    public List<DvMachineryType> selectDvMachineryTypeList(DvMachineryType dvMachineryType);

    public DvMachineryType selectDvMachineryTypeById(Long machineryTypeId);

    public boolean checkMachineryTypeCodeUnique(DvMachineryType dvMachineryType);

    /** 构建前端树形下拉 */
    public List<TreeSelect> buildMachineryTypeTreeSelect(List<DvMachineryType> list);

    /** 校验父节点是否合法（排除自身及子孙） */
    public boolean canSetParentType(Long machineryTypeId, Long parentTypeId);

    /** 查询排除自身及子孙的列表（编辑时选父节点用） */
    public List<DvMachineryType> selectListExcludeChild(Long machineryTypeId);

    /** 查询指定父ID下的子节点列表 */
    public List<DvMachineryType> selectDvMachineryTypeByParentId(Long parentTypeId);

    /** 查询指定节点及所有子孙节点ID（MySQL 8.0 递归CTE） */
    public List<Long> selectDescendantIds(Long machineryTypeId);

        public int insertDvMachineryType(DvMachineryType dvMachineryType);
    public int updateDvMachineryType(DvMachineryType dvMachineryType);

    public int deleteDvMachineryTypeById(Long machineryTypeId);

    public int deleteDvMachineryTypeByIds(Long[] machineryTypeIds);
}
