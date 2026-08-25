package com.ruoyi.system.service.mes.pro;

import java.util.List;
import com.ruoyi.system.domain.mes.pro.vo.DelayItemVO;
import com.ruoyi.system.domain.mes.pro.vo.WorkorderProgressVO;

/**
 * 工单进度 Service
 *
 * @author qixiaoxia
 * @date 2026-08-22
 */
public interface IProProgressService
{
    /**
     * 查询单个工单的进度详情（含工序进度、流转卡进度）。
     */
    WorkorderProgressVO getWorkorderProgress(Long workorderId);

    /**
     * 延期预警分页列表（已由 Controller 启动 PageHelper 分页）。
     *
     * @param objectType WORKORDER / TASK
     * @param riskLevel  DELAY / WARNING / null(全部)
     */
    List<DelayItemVO> selectDelayList(String objectType, String riskLevel,
            Long workshopId, Long teamId, String keyword);
}
