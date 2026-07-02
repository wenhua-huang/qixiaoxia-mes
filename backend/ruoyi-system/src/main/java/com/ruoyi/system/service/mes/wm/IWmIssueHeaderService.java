package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;

/**
 * WmIssueHeaderService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IWmIssueHeaderService
{
    public WmIssueHeader selectWmIssueHeaderByIssueId(Long issueId);
    public List<WmIssueHeader> selectWmIssueHeaderList(WmIssueHeader e);
    public List<WmIssueHeader> selectAll();
    public int insertWmIssueHeader(WmIssueHeader e);
    public int updateWmIssueHeader(WmIssueHeader e);
    public int deleteWmIssueHeaderByIssueIds(Long[] issueIds);
    public int deleteWmIssueHeaderByIssueId(Long issueId);

    /** 根据工单BOM自动生成领料行 */
    public int loadBomLines(Long issueId, Long workorderId);

    /** 确认领料单：DRAFT → CONFIRMED，预占库存（扣quantityAvailable） */
    public int confirmIssue(Long issueId);

    /** 释放预占库存：CONFIRMED → DRAFT，恢复quantityAvailable */
    public int releaseAllocation(Long issueId);

    /** 执行出库：扣库存 + 写追溯 + 状态改为POSTED */
    public int executeIssue(Long issueId);
}
