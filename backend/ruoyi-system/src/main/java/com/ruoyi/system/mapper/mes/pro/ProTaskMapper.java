package com.ruoyi.system.mapper.mes.pro;

import java.util.List;
import java.math.BigDecimal;
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
}
