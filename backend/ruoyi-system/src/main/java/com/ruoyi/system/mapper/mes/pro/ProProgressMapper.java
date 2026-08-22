package com.ruoyi.system.mapper.mes.pro;

import com.ruoyi.system.domain.mes.pro.vo.DelayItemVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 工单进度 Mapper：聚合实际时间 + 延期预警查询
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public interface ProProgressMapper
{
    /**
     * 按任务 ID 批量聚合实际开始/结束时间。
     * 返回每行：taskId、actualStart、actualEnd。
     */
    List<Map<String, Object>> aggregateActualByTaskIds(@Param("taskIds") List<Long> taskIds);

    /**
     * 按工单聚合：planStart=MIN(task.start_time), planEnd=MAX(task.end_time),
     * actualStart=MIN(card_process.input_time), actualEnd=MAX(card_process.output_time)。
     */
    Map<String, Object> aggregateWorkorderTime(@Param("workorderId") Long workorderId);

    /**
     * 工单延期预警列表。
     */
    List<DelayItemVO> selectWorkorderDelays(@Param("riskLevel") String riskLevel,
            @Param("workshopId") Long workshopId, @Param("keyword") String keyword,
            @Param("warnDays") Integer warnDays);

    /**
     * 任务延期预警列表。
     */
    List<DelayItemVO> selectTaskDelays(@Param("riskLevel") String riskLevel,
            @Param("workshopId") Long workshopId, @Param("teamId") Long teamId,
            @Param("keyword") String keyword, @Param("warnHours") Integer warnHours);
}
