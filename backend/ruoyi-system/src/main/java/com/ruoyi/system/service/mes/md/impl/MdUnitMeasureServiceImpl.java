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
 * 单位管理Service业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2025-06-10
 */
@Service
public class MdUnitMeasureServiceImpl implements IMdUnitMeasureService
{
    @Autowired
    private MdUnitMeasureMapper mdUnitMeasureMapper;

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

    @Override
    public int insertMdUnitMeasure(MdUnitMeasure mdUnitMeasure)
    {
        mdUnitMeasure.setCreateTime(DateUtils.getNowDate());
        return mdUnitMeasureMapper.insertMdUnitMeasure(mdUnitMeasure);
    }    @Override
    public int updateMdUnitMeasure(MdUnitMeasure mdUnitMeasure)
    {
        mdUnitMeasure.setUpdateTime(DateUtils.getNowDate());
        return mdUnitMeasureMapper.updateMdUnitMeasure(mdUnitMeasure);
    }

    @Override
    public int deleteMdUnitMeasureByUnitIds(Long[] unitIds)
    {
        return mdUnitMeasureMapper.deleteMdUnitMeasureByUnitIds(unitIds);
    }

    @Override
    public int deleteMdUnitMeasureByUnitId(Long unitId)
    {
        return mdUnitMeasureMapper.deleteMdUnitMeasureByUnitId(unitId);
    }
}
