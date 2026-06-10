package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdUnitMeasure;
import com.ruoyi.system.mapper.mes.md.MdUnitMeasureMapper;
import com.ruoyi.system.service.mes.md.IMdUnitMeasureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 单位管理Service业务层处理
 *
 * @author qixiaoxia
 * @date 2025-06-10
 */
@Service
public class MdUnitMeasureServiceImpl implements IMdUnitMeasureService
{
    @Autowired
    private MdUnitMeasureMapper mdUnitMeasureMapper;

    /**
     * 查询单位
     *
     * @param unitId 单位主键
     * @return 单位
     */
    @Override
    public MdUnitMeasure selectMdUnitMeasureByUnitId(Long unitId)
    {
        return mdUnitMeasureMapper.selectMdUnitMeasureByUnitId(unitId);
    }

    @Override
    public boolean checkUnitCodeUnique(MdUnitMeasure mdUnitMeasure)
    {
        MdUnitMeasure unitMeasure = mdUnitMeasureMapper.checkUnitCodeUnique(mdUnitMeasure);
        Long unitId = mdUnitMeasure.getUnitId() == null ? -1L : mdUnitMeasure.getUnitId();
        if (StringUtils.isNotNull(unitMeasure) && unitMeasure.getUnitId().longValue() != unitId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 查询单位列表
     *
     * @param mdUnitMeasure 单位
     * @return 单位
     */
    @Override
    public List<MdUnitMeasure> selectMdUnitMeasureList(MdUnitMeasure mdUnitMeasure)
    {
        return mdUnitMeasureMapper.selectMdUnitMeasureList(mdUnitMeasure);
    }

    @Override
    public MdUnitMeasure selectMdUnitMeasureByCode(String unitCode)
    {
        return mdUnitMeasureMapper.selectMdUnitMeasureByCode(unitCode);
    }

    /**
     * 新增单位
     *
     * @param mdUnitMeasure 单位
     * @return 结果
     */
    @Override
    public int insertMdUnitMeasure(MdUnitMeasure mdUnitMeasure)
    {
        mdUnitMeasure.setCreateTime(DateUtils.getNowDate());
        return mdUnitMeasureMapper.insertMdUnitMeasure(mdUnitMeasure);
    }

    /**
     * 修改单位
     *
     * @param mdUnitMeasure 单位
     * @return 结果
     */
    @Override
    public int updateMdUnitMeasure(MdUnitMeasure mdUnitMeasure)
    {
        mdUnitMeasure.setUpdateTime(DateUtils.getNowDate());
        return mdUnitMeasureMapper.updateMdUnitMeasure(mdUnitMeasure);
    }

    /**
     * 批量删除单位
     *
     * @param unitIds 需要删除的单位主键
     * @return 结果
     */
    @Override
    public int deleteMdUnitMeasureByUnitIds(Long[] unitIds)
    {
        return mdUnitMeasureMapper.deleteMdUnitMeasureByUnitIds(unitIds);
    }

    /**
     * 删除单位信息
     *
     * @param unitId 单位主键
     * @return 结果
     */
    @Override
    public int deleteMdUnitMeasureByUnitId(Long unitId)
    {
        return mdUnitMeasureMapper.deleteMdUnitMeasureByUnitId(unitId);
    }
}
