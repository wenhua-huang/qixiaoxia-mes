package com.ruoyi.system.service.mes.pro;

import java.util.List;
import java.util.Map;

import com.ruoyi.system.domain.mes.pro.ProException;

/**
 * 生产异常完工硬拦服务（E5，一期硬拦）。
 *
 * <p>工单存在未关闭异常时，两条自动完工路径（末道报工审核达标、末工序任务完成）
 * 均抛 {@link com.ruoyi.common.exception.ServiceException} 整体回滚；
 * 关闭全部异常后才可完工。
 *
 * @author qixiaoxia
 */
public interface IProExceptionBlockService
{
    /**
     * 完工断言：有未关闭异常则抛出带数量与单号示例的 ServiceException。
     */
    void assertCompletable(Long workorderId);

    /**
     * 工单未关闭异常明细（工单详情红色警示区用）。
     */
    List<ProException> listOpenByWorkorder(Long workorderId);

    /**
     * 批量查询工单未关闭异常态（工单列表角标，消除 N+1），单次最多 100 个工单。
     *
     * @return key=工单ID字符串，value={openCount:int, sampleCodes:List&lt;String&gt;}
     */
    Map<String, Map<String, Object>> openState(List<Long> workorderIds);
}
