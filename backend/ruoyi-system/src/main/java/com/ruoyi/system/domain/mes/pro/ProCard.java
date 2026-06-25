package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

public class ProCard extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long cardId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "流转卡编码") private String cardCode;
    @Excel(name = "工单ID") private Long workorderId;
    @Excel(name = "工单编码") private String workorderCode;
    @Excel(name = "工单名称") private String workorderName;
    private Long taskId; private String taskCode;
    @Excel(name = "批次号") private String batchCode;
    @Excel(name = "产品物料ID") private Long itemId;
    @Excel(name = "产品物料编码") private String itemCode;
    @Excel(name = "产品物料名称") private String itemName;
    private String specification;
    @Excel(name = "主单位编码") private String unitOfMeasure;
    @Excel(name = "主单位名称") private String unitName;
    private String barcodeUrl;
    @Excel(name = "流转数量") private BigDecimal quantityTransfered;
    private Long currentProcessId; private String currentProcessName;
    @Excel(name = "状态") private String status;

    public Long getCardId() { return cardId; } public void setCardId(Long v) { this.cardId = v; }
    public Long getFactoryId() { return factoryId; } public void setFactoryId(Long v) { this.factoryId = v; }
    public String getCardCode() { return cardCode; } public void setCardCode(String v) { this.cardCode = v; }
    public Long getWorkorderId() { return workorderId; } public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; } public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; } public void setWorkorderName(String v) { this.workorderName = v; }
    public Long getTaskId() { return taskId; } public void setTaskId(Long v) { this.taskId = v; }
    public String getTaskCode() { return taskCode; } public void setTaskCode(String v) { this.taskCode = v; }
    public String getBatchCode() { return batchCode; } public void setBatchCode(String v) { this.batchCode = v; }
    public Long getItemId() { return itemId; } public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; } public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; } public void setItemName(String v) { this.itemName = v; }
    public String getSpecification() { return specification; } public void setSpecification(String v) { this.specification = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; } public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; } public void setUnitName(String v) { this.unitName = v; }
    public String getBarcodeUrl() { return barcodeUrl; } public void setBarcodeUrl(String v) { this.barcodeUrl = v; }
    public BigDecimal getQuantityTransfered() { return quantityTransfered; } public void setQuantityTransfered(BigDecimal v) { this.quantityTransfered = v; }
    public Long getCurrentProcessId() { return currentProcessId; } public void setCurrentProcessId(Long v) { this.currentProcessId = v; }
    public String getCurrentProcessName() { return currentProcessName; } public void setCurrentProcessName(String v) { this.currentProcessName = v; }
    public String getStatus() { return status; } public void setStatus(String v) { this.status = v; }
    @Override public String toString() { return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE).append("cardId",getCardId()).append("cardCode",getCardCode()).append("workorderId",getWorkorderId()).append("status",getStatus()).toString(); }
}
