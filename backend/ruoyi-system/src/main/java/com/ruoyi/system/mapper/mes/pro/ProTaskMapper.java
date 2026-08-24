package com.ruoyi.system.mapper.mes.pro;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.pro.ProTask;

public interface ProTaskMapper {
    ProTask selectProTaskByTaskId(Long taskId);
    List<ProTask> selectProTaskList(ProTask task);

    /**
     * 查可报工任务：厂内 PRODUCING（workstation_code != 'VENDOR'）且所属工单处于
     * PREPARE/PRODUCING。JOIN qxx_pro_workorder 过滤工单状态，避免查出工单已完工/
     * 取消后仍残留在 PRODUCING 的僵尸任务（点进去会被 feedbackEntry 工单状态门控拦截）。
     * factory_id 由拦截器自动注入（主表别名 t）。
     */
    List<ProTask> selectReportableTaskList(ProTask task);
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

    /**
     * 按工单 ID + 源状态批量更新任务状态（用于工单开工级联下发/取消级联取消）。
     * factory_id 由 MyBatis 拦截器自动注入。
     */
    int updateStatusByWorkorder(@Param("workorderId") Long workorderId,
                                @Param("fromStatuses") List<String> fromStatuses,
                                @Param("toStatus") String toStatus);

    /**
     * 条件完成任务：仅当任务当前为 PRODUCING 时置 COMPLETED，防止收货与工单取消并发时回退状态。
     * @return 影响行数；0 表示任务已被其他流程推进/取消
     */
    int completeTaskIfProducing(@Param("taskId") Long taskId, @Param("operator") String operator);
}
