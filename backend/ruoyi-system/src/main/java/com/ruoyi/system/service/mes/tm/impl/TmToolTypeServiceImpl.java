package com.ruoyi.system.service.mes.tm.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.tm.TmToolTypeMapper;
import com.ruoyi.system.domain.mes.tm.TmToolType;
import com.ruoyi.system.service.mes.tm.ITmToolTypeService;

/**
 * 工装夹具类型Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class TmToolTypeServiceImpl implements ITmToolTypeService 
{
    @Autowired
    private TmToolTypeMapper qxxTmToolTypeMapper;

    /**
     * 查询工装夹具类型
     * 
     * @param toolTypeId 工装夹具类型主键
     * @return 工装夹具类型
     */
    @Override
    public TmToolType selectTmToolTypeByToolTypeId(Long toolTypeId)
    {
        return qxxTmToolTypeMapper.selectTmToolTypeByToolTypeId(toolTypeId);
    }

    /**
     * 查询工装夹具类型列表
     * 
     * @param tmToolType 工装夹具类型
     * @return 工装夹具类型
     */
    @Override
    public List<TmToolType> selectTmToolTypeList(TmToolType tmToolType)
    {
        return qxxTmToolTypeMapper.selectTmToolTypeList(tmToolType);
    }

    /**
     * 查询所有启用的工装夹具类型
     *
     * @return 工装夹具类型集合
     */
    @Override
    public List<TmToolType> selectTmToolTypeAll()
    {
        return qxxTmToolTypeMapper.selectTmToolTypeAll();
    }

    /**
     * 新增工装夹具类型
     * 
     * @param tmToolType 工装夹具类型
     * @return 结果
     */
    @Override
    @Transactional
    public int insertTmToolType(TmToolType tmToolType)
    {
        tmToolType.setCreateTime(DateUtils.getNowDate());
        tmToolType.setCreateBy(SecurityUtils.getUsername());
        return qxxTmToolTypeMapper.insertTmToolType(tmToolType);
    }

    /**
     * 修改工装夹具类型
     * 
     * @param tmToolType 工装夹具类型
     * @return 结果
     */
    @Override
    public int updateTmToolType(TmToolType tmToolType)
    {
        tmToolType.setUpdateTime(DateUtils.getNowDate());
        tmToolType.setUpdateBy(SecurityUtils.getUsername());
        return qxxTmToolTypeMapper.updateTmToolType(tmToolType);
    }

    /**
     * 批量删除工装夹具类型
     * 
     * @param toolTypeIds 需要删除的工装夹具类型主键
     * @return 结果
     */
    @Override
    public int deleteTmToolTypeByToolTypeIds(Long[] toolTypeIds)
    {
        return qxxTmToolTypeMapper.deleteTmToolTypeByToolTypeIds(toolTypeIds);
    }

    /**
     * 删除工装夹具类型信息
     * 
     * @param toolTypeId 工装夹具类型主键
     * @return 结果
     */
    @Override
    public int deleteTmToolTypeByToolTypeId(Long toolTypeId)
    {
        return qxxTmToolTypeMapper.deleteTmToolTypeByToolTypeId(toolTypeId);
    }
}
