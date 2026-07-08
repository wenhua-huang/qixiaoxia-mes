package com.ruoyi.system.service.mes.wm;

import java.util.List;
import com.ruoyi.system.domain.mes.wm.WmIssueDetail;
import com.ruoyi.system.domain.mes.wm.WmIssueHeader;

/**
 * WmIssueHeaderService接口
 *
 * <p>领料单完整生命周期状态机（标准制造 8 态）：
 * <pre>
 *   DRAFT ──提交──▶ PENDING ──审核──▶ APPROVED ──预占──▶ ALLOCATED
 *    │                │ 退回            │                   │ 发料(可分批)
 *    │                ◀────────────────                    ▼
 *    │                                            PARTIAL_ISSUED / ISSUED
 *    └──────────作废──────────────▶ CANCELED          │ 收料确认/关闭
 *                                                      ▼
 *                                                   CLOSED
 * </pre>
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

    // ══════════════════════════════════════════════
    // 生命周期状态流转
    // ══════════════════════════════════════════════

    /** 提交审核：DRAFT → PENDING */
    public int submitForApprove(Long issueId);

    /** 审核通过：PENDING → APPROVED */
    public int approve(Long issueId);

    /** 审核退回：PENDING → DRAFT */
    public int reject(Long issueId);

    /**
     * 预占领料单：APPROVED → ALLOCATED，扣减可用库存 quantityAvailable。
     * 兼容旧接口名 confirmIssue（DRAFT → ALLOCATED 直达，供历史调用方使用）。
     */
    public int confirmIssue(Long issueId);

    /** 释放预占库存：ALLOCATED → APPROVED，恢复 quantityAvailable */
    public int releaseAllocation(Long issueId);

    /**
     * 发料出库（支持分批）：ALLOCATED/PARTIAL_ISSUED → PARTIAL_ISSUED/ISSUED。
     * 按本次发料明细（WmIssueDetail 列表）扣减 onhand，累加 quantity_issued_total，
     * 全部发完转 ISSUED，否则 PARTIAL_ISSUED。
     */
    public int issueOut(Long issueId, List<WmIssueDetail> details);

    /** 关闭：ISSUED → CLOSED（收料确认/手工关闭，终态） */
    public int close(Long issueId);

    /** 作废：非终态 → CANCELED，ALLOCATED 态先恢复 available（终态） */
    public int cancel(Long issueId, String reason);

    /**
     * 执行出库（全量，旧接口兼容）：ALLOCATED → ISSUED。
     * 等价于按 line 全量发料，不写 detail。保留供历史调用方使用。
     * @deprecated 请使用 {@link #issueOut(Long, List)} 支持分批发料
     */
    @Deprecated
    public int executeIssue(Long issueId);
}
