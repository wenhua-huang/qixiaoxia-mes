package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmRtIssue;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;

/**
 * WmRtIssueService接口
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public interface IWmRtIssueService
{
    public WmRtIssue selectWmRtIssueByRtId(Long rtId);
    public List<WmRtIssue> selectWmRtIssueList(WmRtIssue e);
    public List<WmRtIssue> selectAll();
    public int insertWmRtIssue(WmRtIssue e);
    public int updateWmRtIssue(WmRtIssue e);
    public int deleteWmRtIssueByRtIds(Long[] rtIds);
    public int deleteWmRtIssueByRtId(Long rtId);

    /** 从领料单创建退料单（落库式，全量退，需已存在领料单） */
    public Long createFromIssue(Long issueId);

    /**
     * 从领料单生成退料单草稿（不落库，差额退料）。
     * 退料量 = 已发料量 − 工单级报工消耗量（与 generateReturnDocuments 算法一致）。
     * 仅允许 ISSUED 状态领料单。真正落库走 {@link #insertWmRtIssue}。
     */
    public WmRtIssue buildFromIssue(Long issueId);

    /**
     * 退料领料单选择弹窗：分页查 ISSUED 领料单并预算每张的可退量（已发料−工单级消耗）。
     * 同工单的消耗只算一次，消除 N+1。
     */
    public List<WmIssueHeader> returnablePreview(WmIssueHeader query);

    /** 执行退库：加库存 + 写追溯 + 状态改为POSTED */
    public int executeReturn(Long rtId);
}
