package com.ruoyi.system.mapper.mes.md;

import java.util.List;
import com.ruoyi.system.domain.mes.md.MdUnitMeasure;

/**
 * 单位管理Mapper接口
 *
 * @author qixiaoxia
 * @date 2025-06-10
 */
public interface MdUnitMeasureMapper
{
    /**
     * 查询单位
     *
     * @param unitId 单位主键
     * @return 单位
     */
    public MdUnitMeasure selectMdUnitMeasureByUnitId(Long unitId);

    /**
     * 单位编码唯一校验
     *
     * @param mdUnitMeasure 单位
     * @return 单位
     */
    public MdUnitMeasure checkUnitCodeUnique(MdUnitMeasure mdUnitMeasure);

    /**
     * 查询单位列表
     *
     * @param mdUnitMeasure 单位
     * @return 单位集合
     */
    public List<MdUnitMeasure> selectMdUnitMeasureList(MdUnitMeasure mdUnitMeasure);

    /**
     * 根据单位编码查询单位
     *
     * @param unitCode 单位编码
     * @return 单位
     */
    public MdUnitMeasure selectMdUnitMeasureByCode(String unitCode);

    /**
     * 新增单位
     *
     * @param mdUnitMeasure 单位
     * @return 结果
     */
    public int insertMdUnitMeasure(MdUnitMeasure mdUnitMeasure);

    /**
     * 修改单位
     *
     * @param mdUnitMeasure 单位
     * @return 结果
     */
    public int updateMdUnitMeasure(MdUnitMeasure mdUnitMeasure);

    /**
     * 删除单位
     *
     * @param unitId 单位主键
     * @return 结果
     */
    public int deleteMdUnitMeasureByUnitId(Long unitId);

    /**
     * 批量删除单位
     *
     * @param unitIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMdUnitMeasureByUnitIds(Long[] unitIds);
}
