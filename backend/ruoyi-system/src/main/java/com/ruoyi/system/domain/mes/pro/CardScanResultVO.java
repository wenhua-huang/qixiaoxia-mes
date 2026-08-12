package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.List;

/**
 * 扫流转卡码反查报工上下文（设计文档 §6.2）。
 */
public class CardScanResultVO {

    /** 流转卡（携带 cardCode/status/currentProcess/workorder/item 等冗余字段） */
    private ProCard card;

    /** 当前可报任务（工单下、当前工序、PRODUCING、非外协工位） */
    private List<ProTask> reportableTasks;

    /** BOM 消耗默认值 */
    private List<ProFeedbackConsume> consumeDefaults;

    /** 该卡该工序已审核合格数（防超报） */
    private BigDecimal reportedQualifiedSum;

    /** 是否可报工 */
    private boolean canReport;

    /** 不可报原因码：CARD_NOT_FOUND / CARD_COMPLETED / CARD_OUTSOURCING / CARD_SCRAPPED / NO_REPORTABLE_TASK */
    private String reason;

    public ProCard getCard() { return card; }
    public void setCard(ProCard card) { this.card = card; }

    public List<ProTask> getReportableTasks() { return reportableTasks; }
    public void setReportableTasks(List<ProTask> reportableTasks) { this.reportableTasks = reportableTasks; }

    public List<ProFeedbackConsume> getConsumeDefaults() { return consumeDefaults; }
    public void setConsumeDefaults(List<ProFeedbackConsume> consumeDefaults) { this.consumeDefaults = consumeDefaults; }

    public BigDecimal getReportedQualifiedSum() { return reportedQualifiedSum; }
    public void setReportedQualifiedSum(BigDecimal reportedQualifiedSum) { this.reportedQualifiedSum = reportedQualifiedSum; }

    public boolean isCanReport() { return canReport; }
    public void setCanReport(boolean canReport) { this.canReport = canReport; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
