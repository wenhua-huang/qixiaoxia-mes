package com.ruoyi.system.mapper.mes.pro;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.pro.ProException;

/**
 * 生产异常单 Mapper（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 */
public interface ProExceptionMapper
{
    /** 台账条件查询 */
    List<ProException> selectProExceptionList(ProException query);

    /** 主键查询 */
    ProException selectProExceptionByExceptionId(Long exceptionId);

    /** 新增（factory_id 不写，由拦截器注入） */
    int insertProException(ProException exception);

    /** 动态更新（状态流转/补全共用） */
    int updateProException(ProException exception);

    /** 补全编辑专用：显式 SET 可空列，允许清空影响/可使用数量、预计到货日、备注 */
    int updateExceptionEditOptional(ProException exception);

    /** 工单下未关闭异常（完工硬拦/工单警示区），按发生时间升序 */
    List<ProException> selectOpenByWorkorderId(@Param("workorderId") Long workorderId);

    /** 批量查多个工单的未关闭异常（列表角标，消 N+1；非空由调用方保证） */
    List<ProException> selectOpenByWorkorderIds(@Param("workorderIds") Collection<Long> workorderIds);

    /** 统计某原任务下已开的异常(返工/补做)任务数，-E 序号计数用 */
    int countExceptionTasksByOrigin(@Param("originTaskId") Long originTaskId);
}
