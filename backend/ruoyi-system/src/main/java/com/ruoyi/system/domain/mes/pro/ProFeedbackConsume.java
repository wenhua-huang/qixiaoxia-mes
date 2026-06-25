package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 报工物料消耗对象 qxx_pro_feedback_consume
 *
 * @author qixiaoxia
 * @date 2026-06-23
 */
public class ProFeedbackConsume
{
    private static final long serialVersionUID = 1L;

    private Long consumeId;
    private Long factoryId;
    private Long feedbackId;
    private Long workorderId;
    private Long itemId;
    private String itemCode;
    private String itemName;
    private BigDecimal quantity;
    private String batchCode;

    public Long getConsumeId() { return consumeId; }
    public void setConsumeId(Long v) { this.consumeId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public Long getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Long v) { this.feedbackId = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("consumeId", getConsumeId())
            .append("feedbackId", getFeedbackId())
            .append("itemCode", getItemCode())
            .append("quantity", getQuantity())
            .toString();
    }
}
