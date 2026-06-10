package com.ruoyi.system.service.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdUnitMeasure;

/**
 * 单位管理Service接口（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2025-06-10
 */
public interface IMdUnitMeasureService
{
    public MdUnitMeasure selectMdUnitMeasureByUnitId(Long unitId);
    public boolean checkUnitCodeUnique(MdUnitMeasure mdUnitMeasure);
    public List<MdUnitMeasure> selectMdUnitMeasureList(MdUnitMeasure mdUnitMeasure);
    public MdUnitMeasure selectMdUnitMeasureByCode(String unitCode);
    public int insertMdUnitMeasure(MdUnitMeasure mdUnitMeasure);
    public int updateMdUnitMeasure(MdUnitMeasure mdUnitMeasure);
    public int deleteMdUnitMeasureByUnitIds(Long[] unitIds);
    public int deleteMdUnitMeasureByUnitId(Long unitId);
}
