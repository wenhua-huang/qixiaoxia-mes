package com.ruoyi.system.mapper.mes.pro;

import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 生产统计报表 Mapper：总览 KPI / 产能工时 / 趋势 / 状态分布 / 明细
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public interface ProReportMapper
{
    /** 总览：工单计数（总数/完工/延期/生产中） */
    Map<String, Object> selectOverviewCounts(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds, @Param("teamIds") List<Long> teamIds);

    /** 总览：标准工时（分钟） */
    Long selectStandardMinutes(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds, @Param("teamIds") List<Long> teamIds);

    /** 总览：实际工时（分钟，仅 CLOSED 会话） */
    Long selectActualMinutes(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds, @Param("teamIds") List<Long> teamIds);

    /** 产能：按车间聚合产出侧 */
    List<Map<String, Object>> selectWorkshopOutput(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds);

    /** 产能：按工作站聚合产出侧 */
    List<Map<String, Object>> selectWorkstationOutput(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds);

    /** 产能：按工序聚合产出侧 */
    List<Map<String, Object>> selectProcessOutput(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds);

    /** 产能：按班组聚合产出侧（来源已审核报工） */
    List<Map<String, Object>> selectTeamOutput(@Param("begin") Date begin, @Param("end") Date end,
            @Param("teamIds") List<Long> teamIds);

    /** 产能：按车间聚合实际工时侧 */
    List<Map<String, Object>> selectWorkshopHours(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds);

    /** 产能：按工作站聚合实际工时侧 */
    List<Map<String, Object>> selectWorkstationHours(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds);

    /** 产能：按工序聚合实际工时侧 */
    List<Map<String, Object>> selectProcessHours(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds);

    /** 产能：按班组聚合实际工时侧 */
    List<Map<String, Object>> selectTeamHours(@Param("begin") Date begin, @Param("end") Date end,
            @Param("teamIds") List<Long> teamIds);

    /** 趋势：按日完工任务数/延期数/合格数 */
    List<Map<String, Object>> selectTrendCompleted(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopId") Long workshopId);

    /** 趋势：按日新建工单数 */
    List<Map<String, Object>> selectTrendCreated(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopIds") List<Long> workshopIds);

    /** 趋势：按日实际工时 */
    List<Map<String, Object>> selectTrendMinutes(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopId") Long workshopId);

    /** 任务状态分布 */
    List<Map<String, Object>> selectTaskStatus(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopId") Long workshopId);

    /** 报表明细（工单粒度，Controller 侧分页） */
    List<Map<String, Object>> selectDetail(@Param("begin") Date begin, @Param("end") Date end,
            @Param("workshopId") Long workshopId, @Param("teamId") Long teamId);
}
