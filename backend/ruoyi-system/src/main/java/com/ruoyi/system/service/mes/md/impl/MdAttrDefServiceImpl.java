package com.ruoyi.system.service.mes.md.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.md.MdAttrDef;
import com.ruoyi.system.mapper.mes.md.MdAttrDefMapper;
import com.ruoyi.system.service.mes.md.IMdAttrDefService;

/**
 * 物料扩展属性字典Service业务层
 *
 * @author qixiaoxia
 * @date 2026-08-01
 */
@Service
public class MdAttrDefServiceImpl implements IMdAttrDefService
{
    @Autowired
    private MdAttrDefMapper mdAttrDefMapper;

    @Override
    public MdAttrDef selectMdAttrDefByAttrId(Long attrId)
    {
        return mdAttrDefMapper.selectMdAttrDefByAttrId(attrId);
    }

    @Override
    public List<MdAttrDef> selectMdAttrDefList(MdAttrDef mdAttrDef)
    {
        return mdAttrDefMapper.selectMdAttrDefList(mdAttrDef);
    }

    @Override
    public boolean checkAttrCodeUnique(MdAttrDef mdAttrDef)
    {
        MdAttrDef exist = mdAttrDefMapper.checkAttrCodeUnique(mdAttrDef);
        Long attrId = mdAttrDef.getAttrId() == null ? -1L : mdAttrDef.getAttrId();
        if (StringUtils.isNotNull(exist) && exist.getAttrId().longValue() != attrId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int insertMdAttrDef(MdAttrDef mdAttrDef)
    {
        // 全局属性字典，factory_id 恒为 0（不随工厂隔离）
        mdAttrDef.setFactoryId(0L);
        mdAttrDef.setCreateTime(DateUtils.getNowDate());
        return mdAttrDefMapper.insertMdAttrDef(mdAttrDef);
    }

    @Override
    public int updateMdAttrDef(MdAttrDef mdAttrDef)
    {
        mdAttrDef.setUpdateTime(DateUtils.getNowDate());
        return mdAttrDefMapper.updateMdAttrDef(mdAttrDef);
    }

    @Override
    public int deleteMdAttrDefByAttrIds(Long[] attrIds)
    {
        return mdAttrDefMapper.deleteMdAttrDefByAttrIds(attrIds);
    }
}
