package com.ruoyi.system.domain.mes.qc;

/**
 * （工单ID, 工序ID）组合键：跟单质检门控批量查询的入参载体。
 *
 * <p>MyBatis foreach 中 long[] 元素无法用 {@code #{pair[0]}} 取值，
 * 故用具名 getter 的 POJO 承载。
 *
 * @author qixiaoxia
 */
public class WorkorderProcessPair
{
    private final Long workorderId;
    private final Long processId;

    public WorkorderProcessPair(Long workorderId, Long processId)
    {
        this.workorderId = workorderId;
        this.processId = processId;
    }

    public Long getWorkorderId()
    {
        return workorderId;
    }

    public Long getProcessId()
    {
        return processId;
    }
}
