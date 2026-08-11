package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.List;

/**
 * 通用外协请求 DTO（创建外协发货单时用）
 *
 * 不依赖任何具体业务领域（不碰 WmRollDetail），
 * 分切/印刷等业务在前端组装好 issueLines 传入。
 *
 * @author qixiaoxia
 */
public class OutsourceRequest
{
    /** 外协厂商ID（必填） */
    private Long vendorId;
    private String vendorCode;
    private String vendorName;

    /** 工单ID（可选，独立外协可不填） */
    private Long workorderId;
    private String workorderCode;
    /** 流转卡ID（可选，联动 OUTSOURCING 状态） */
    private Long cardId;
    /** 工艺路线ID（可选，判断末工序用） */
    private Long routeId;
    /** 外协工序ID（可选） */
    private Long processId;
    private String processCode;
    private String processName;

    /** 来源类型：GENERIC/SLITTING/PRINTING */
    private String sourceType = "GENERIC";
    /** 来源业务单ID（如 slitting_record.slit_id） */
    private Long sourceRefId;

    /** 发料行（必填，至少一行） */
    private List<WmOutsourceIssueLine> issueLines;

    private String remark;

    /** 草稿模式：true=只建单不扣料（DRAFT），false=直接发料扣料（ISSUED） */
    private boolean draft = false;

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public Long getCardId() { return cardId; }
    public void setCardId(Long v) { this.cardId = v; }
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long v) { this.routeId = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String v) { this.sourceType = v; }
    public Long getSourceRefId() { return sourceRefId; }
    public void setSourceRefId(Long v) { this.sourceRefId = v; }
    public List<WmOutsourceIssueLine> getIssueLines() { return issueLines; }
    public void setIssueLines(List<WmOutsourceIssueLine> v) { this.issueLines = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public boolean isDraft() { return draft; }
    public void setDraft(boolean v) { this.draft = v; }
}
