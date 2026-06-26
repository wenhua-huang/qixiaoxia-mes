package com.ruoyi.system.service.mes.tm;

import java.util.List;
import com.ruoyi.system.domain.mes.tm.TmTool;

/**
 * 工装夹具清单Service接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface ITmToolService 
{
    /**
     * 查询工装夹具清单
     * 
     * @param toolId 工装夹具清单主键
     * @return 工装夹具清单
     */
    public TmTool selectTmToolByToolId(Long toolId);

    /**
     * 查询工装夹具清单列表
     * 
     * @param tmTool 工装夹具清单
     * @return 工装夹具清单集合
     */
    public List<TmTool> selectTmToolList(TmTool tmTool);

    /**
     * 新增工装夹具清单
     * 
     * @param tmTool 工装夹具清单
     * @return 结果
     */
        public int insertTmTool(TmTool tmTool);
    /**
     * 修改工装夹具清单
     * 
     * @param tmTool 工装夹具清单
     * @return 结果
     */
    public int updateTmTool(TmTool tmTool);

    /**
     * 批量删除工装夹具清单
     * 
     * @param toolIds 需要删除的工装夹具清单主键集合
     * @return 结果
     */
    public int deleteTmToolByToolIds(Long[] toolIds);

    /**
     * 删除工装夹具清单信息
     * 
     * @param toolId 工装夹具清单主键
     * @return 结果
     */
    public int deleteTmToolByToolId(Long toolId);
}
