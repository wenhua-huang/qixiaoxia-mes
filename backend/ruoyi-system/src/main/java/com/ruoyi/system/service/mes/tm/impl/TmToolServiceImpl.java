package com.ruoyi.system.service.mes.tm.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.mes.tm.TmToolMapper;
import com.ruoyi.system.domain.mes.tm.TmTool;
import com.ruoyi.system.service.mes.tm.ITmToolService;

/**
 * 工装夹具清单Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
@Service
public class TmToolServiceImpl implements ITmToolService 
{
    @Autowired
    private TmToolMapper qxxTmToolMapper;

    /**
     * 查询工装夹具清单
     * 
     * @param toolId 工装夹具清单主键
     * @return 工装夹具清单
     */
    @Override
    public TmTool selectTmToolByToolId(Long toolId)
    {
        return qxxTmToolMapper.selectTmToolByToolId(toolId);
    }

    /**
     * 查询工装夹具清单列表
     * 
     * @param tmTool 工装夹具清单
     * @return 工装夹具清单
     */
    @Override
    public List<TmTool> selectTmToolList(TmTool tmTool)
    {
        return qxxTmToolMapper.selectTmToolList(tmTool);
    }

    /**
     * 新增工装夹具清单
     * 
     * @param tmTool 工装夹具清单
     * @return 结果
     */
    @Override
    @Transactional
    public int insertTmTool(TmTool tmTool)
    {
        tmTool.setCreateTime(DateUtils.getNowDate());
        tmTool.setCreateBy(SecurityUtils.getUsername());
        return qxxTmToolMapper.insertTmTool(tmTool);
    }

    /**
     * 修改工装夹具清单
     * 
     * @param tmTool 工装夹具清单
     * @return 结果
     */    @Override
    public int updateTmTool(TmTool tmTool)
    {
        tmTool.setUpdateTime(DateUtils.getNowDate());
        tmTool.setUpdateBy(SecurityUtils.getUsername());
        return qxxTmToolMapper.updateTmTool(tmTool);
    }

    /**
     * 批量删除工装夹具清单
     * 
     * @param toolIds 需要删除的工装夹具清单主键
     * @return 结果
     */
    @Override
    public int deleteTmToolByToolIds(Long[] toolIds)
    {
        return qxxTmToolMapper.deleteTmToolByToolIds(toolIds);
    }

    /**
     * 删除工装夹具清单信息
     * 
     * @param toolId 工装夹具清单主键
     * @return 结果
     */
    @Override
    public int deleteTmToolByToolId(Long toolId)
    {
        return qxxTmToolMapper.deleteTmToolByToolId(toolId);
    }
}
