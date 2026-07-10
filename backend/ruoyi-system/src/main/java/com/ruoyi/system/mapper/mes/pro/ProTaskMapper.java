package com.ruoyi.system.mapper.mes.pro;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.pro.ProTask;

public interface ProTaskMapper {
    ProTask selectProTaskByTaskId(Long taskId);
    List<ProTask> selectProTaskList(ProTask task);
    int insertProTask(ProTask task);
    int updateProTask(ProTask task);
    int deleteProTaskByTaskId(Long taskId);
    int deleteProTaskByTaskIds(Long[] taskIds);
    /** 审核报工时原子增量更新任务已生产数量 */
    int addQuantityProduced(@org.apache.ibatis.annotations.Param("taskId") Long taskId,
            @org.apache.ibatis.annotations.Param("deltaProduced") BigDecimal deltaProduced,
            @org.apache.ibatis.annotations.Param("deltaQualified") BigDecimal deltaQualified,
            @org.apache.ibatis.annotations.Param("deltaUnqualified") BigDecimal deltaUnqualified);

    /**
     * 工作站在 [startTime, endTime] 时段内、未结束（非 COMPLETED/CANCEL）的任务数。
     * 返回 >0 表示该时段已被占用（存在冲突）。factoryId 显式传入，因 @Param 多参时拦截器无法自动注入。
     */
    int countConflict(@Param("workstationId") Long workstationId,
                      @Param("startTime") Date startTime,
                      @Param("endTime") Date endTime,
                      @Param("factoryId") Long factoryId,
                      @Param("excludeTaskId") Long excludeTaskId);
}
