package com.ruoyi.system.service.mes.md.impl;

import java.util.ArrayList;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MapCycleUtils;
import com.ruoyi.system.domain.mes.md.MdProductBom;
import com.ruoyi.system.mapper.mes.md.MdProductBomMapper;
import com.ruoyi.system.service.mes.md.IMdProductBomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 产品BOMService业务层处理（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
@Service
public class MdProductBomServiceImpl implements IMdProductBomService
{
    @Autowired
    private MdProductBomMapper mdProductBomMapper;

    @Override
    public MdProductBom selectMdProductBomByBomId(Long bomId)
    {
        return mdProductBomMapper.selectMdProductBomByBomId(bomId);
    }

    @Override
    public List<MdProductBom> selectMdProductBomList(MdProductBom mdProductBom)
    {
        return mdProductBomMapper.selectMdProductBomList(mdProductBom);
    }

    @Override
    public List<MdProductBom> selectAll()
    {
        return mdProductBomMapper.selectAll();
    }

    @Override
    public boolean checkBomCycle(MdProductBom mdProductBom)
    {
        List<MdProductBom> all = mdProductBomMapper.selectAll();
        List<Long[]> edges = new ArrayList<>();
        for (MdProductBom bom : all)
        {
            edges.add(new Long[] { bom.getItemId(), bom.getBomItemId() });
        }
        return MapCycleUtils.hasCycle(mdProductBom.getItemId(), mdProductBom.getBomItemId(), edges);
    }

    @Override
    public int insertMdProductBom(MdProductBom mdProductBom)
    {
        mdProductBom.setCreateTime(DateUtils.getNowDate());
        return mdProductBomMapper.insertMdProductBom(mdProductBom);
    }

    @Override
    public int updateMdProductBom(MdProductBom mdProductBom)
    {
        mdProductBom.setUpdateTime(DateUtils.getNowDate());
        return mdProductBomMapper.updateMdProductBom(mdProductBom);
    }

    @Override
    public int deleteMdProductBomByBomIds(Long[] bomIds)
    {
        return mdProductBomMapper.deleteMdProductBomByBomIds(bomIds);
    }

    @Override
    public int deleteMdProductBomByBomId(Long bomId)
    {
        return mdProductBomMapper.deleteMdProductBomByBomId(bomId);
    }
}
