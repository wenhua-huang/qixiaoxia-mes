package com.ruoyi.system.service.mes.pro;

import com.ruoyi.system.domain.mes.pro.vo.ProductivityRowVO;
import com.ruoyi.system.domain.mes.pro.vo.ReportOverviewVO;
import com.ruoyi.system.domain.mes.pro.vo.TaskStatusDistVO;
import com.ruoyi.system.domain.mes.pro.vo.TrendPointVO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 生产统计报表 Service
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public interface IProReportService
{
    /**
     * 总览 KPI：工单计数 + 标准/实际工时 + 效率。
     */
    ReportOverviewVO overview(Date begin, Date end, List<Long> workshopIds, List<Long> teamIds);

    /**
     * 产能/工时/效率统计，按维度分组。
     *
     * @param groupBy WORKSHOP / WORKSTATION / PROCESS / TEAM
     */
    List<ProductivityRowVO> productivity(String groupBy, Date begin, Date end,
            List<Long> workshopIds, List<Long> teamIds);

    /**
     * 按日趋势：新建/完工/延期/合格数/实际工时，补齐无数据日期。
     */
    List<TrendPointVO> trend(Date begin, Date end, Long workshopId);

    /**
     * 任务状态分布。
     */
    List<TaskStatusDistVO> taskStatus(Date begin, Date end, Long workshopId);

    /**
     * 报表明细（工单粒度，Controller 侧分页），附加 delayLevel。
     */
    List<Map<String, Object>> detail(Date begin, Date end, Long workshopId, Long teamId);
}
