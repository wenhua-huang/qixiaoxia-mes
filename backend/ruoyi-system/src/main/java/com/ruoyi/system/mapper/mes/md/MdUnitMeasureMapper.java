package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdUnitMeasure;

/**
 * 单位管理Mapper接口（factory_id 由 FactoryIdInterceptor 自动注入，SQL 无需手写）
 *
 * @author qixiaoxia
 * @date 2025-06-10
 */
public interface MdUnitMeasureMapper
{
    public MdUnitMeasure selectMdUnitMeasureByUnitId(Long unitId);
    public MdUnitMeasure checkUnitCodeUnique(MdUnitMeasure mdUnitMeasure);
    public List<MdUnitMeasure> selectMdUnitMeasureList(MdUnitMeasure mdUnitMeasure);
    public MdUnitMeasure selectMdUnitMeasureByCode(String unitCode);
    public int insertMdUnitMeasure(MdUnitMeasure mdUnitMeasure);
    public int updateMdUnitMeasure(MdUnitMeasure mdUnitMeasure);
    public int deleteMdUnitMeasureByUnitId(Long unitId);
    public int deleteMdUnitMeasureByUnitIds(Long[] unitIds);
}
