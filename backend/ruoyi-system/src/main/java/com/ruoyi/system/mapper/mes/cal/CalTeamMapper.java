package com.ruoyi.system.mapper.mes.cal;

import java.util.List;
import com.ruoyi.system.domain.mes.cal.CalTeam;

/**
 * 班组Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface CalTeamMapper 
{
    /**
     * 查询班组
     * 
     * @param teamId 班组主键
     * @return 班组
     */
    public CalTeam selectCalTeamByTeamId(Long teamId);

    /**
     * 查询班组列表
     * 
     * @param calTeam 班组
     * @return 班组集合
     */
    public List<CalTeam> selectCalTeamList(CalTeam calTeam);

    /**
     * 新增班组
     * 
     * @param calTeam 班组
     * @return 结果
     */
    public int insertCalTeam(CalTeam calTeam);

    /**
     * 修改班组
     * 
     * @param calTeam 班组
     * @return 结果
     */
    public int updateCalTeam(CalTeam calTeam);

    /**
     * 删除班组
     * 
     * @param teamId 班组主键
     * @return 结果
     */
    public int deleteCalTeamByTeamId(Long teamId);

    /**
     * 批量删除班组
     * 
     * @param teamIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCalTeamByTeamIds(Long[] teamIds);
}
