package com.ruoyi.system.service.mes.cal;

import java.util.List;
import com.ruoyi.system.domain.mes.cal.CalPlanTeam;

/**
 * 计划班组关联Service接口
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public interface ICalPlanTeamService 
{
    /**
     * 查询计划班组关联
     * 
     * @param recordId 计划班组关联主键
     * @return 计划班组关联
     */
    public CalPlanTeam selectCalPlanTeamByRecordId(Long recordId);

    /**
     * 查询计划班组关联列表
     * 
     * @param calPlanTeam 计划班组关联
     * @return 计划班组关联集合
     */
    public List<CalPlanTeam> selectCalPlanTeamList(CalPlanTeam calPlanTeam);

    /**
     * 新增计划班组关联
     * 
     * @param calPlanTeam 计划班组关联
     * @return 结果
     */
    public int insertCalPlanTeam(CalPlanTeam calPlanTeam);

    /**
     * 修改计划班组关联
     * 
     * @param calPlanTeam 计划班组关联
     * @return 结果
     */
    public int updateCalPlanTeam(CalPlanTeam calPlanTeam);

    /**
     * 批量删除计划班组关联
     * 
     * @param recordIds 需要删除的计划班组关联主键集合
     * @return 结果
     */
    public int deleteCalPlanTeamByRecordIds(Long[] recordIds);

    /**
     * 删除计划班组关联信息
     * 
     * @param recordId 计划班组关联主键
     * @return 结果
     */
    public int deleteCalPlanTeamByRecordId(Long recordId);
}
