package com.ruoyi.system.mapper.mes.tm;

import java.util.List;
import com.ruoyi.system.domain.mes.tm.TmToolType;

/**
 * 工装夹具类型Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface TmToolTypeMapper 
{
    /**
     * 查询工装夹具类型
     * 
     * @param toolTypeId 工装夹具类型主键
     * @return 工装夹具类型
     */
    public TmToolType selectTmToolTypeByToolTypeId(Long toolTypeId);

    /**
     * 查询工装夹具类型列表
     * 
     * @param tmToolType 工装夹具类型
     * @return 工装夹具类型集合
     */
    public List<TmToolType> selectTmToolTypeList(TmToolType tmToolType);

    /**
     * 查询所有启用的工装夹具类型
     *
     * @return 工装夹具类型集合
     */
    public List<TmToolType> selectTmToolTypeAll();

    /**
     * 新增工装夹具类型
     * 
     * @param tmToolType 工装夹具类型
     * @return 结果
     */
    public int insertTmToolType(TmToolType tmToolType);

    /**
     * 修改工装夹具类型
     * 
     * @param tmToolType 工装夹具类型
     * @return 结果
     */
    public int updateTmToolType(TmToolType tmToolType);

    /**
     * 删除工装夹具类型
     * 
     * @param toolTypeId 工装夹具类型主键
     * @return 结果
     */
    public int deleteTmToolTypeByToolTypeId(Long toolTypeId);

    /**
     * 批量删除工装夹具类型
     * 
     * @param toolTypeIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTmToolTypeByToolTypeIds(Long[] toolTypeIds);
}
