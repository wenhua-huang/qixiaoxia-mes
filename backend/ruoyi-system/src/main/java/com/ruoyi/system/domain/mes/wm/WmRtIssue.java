package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * qxx_wm_rt_issue 对象
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class WmRtIssue extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long rtId;
    private Long factoryId;
    @Excel(name = "退料单编码")
    private String rtCode;
    @Excel(name = "退料单名称")
    private String rtName;
    private Long issueId;
    private String issueCode;
    @Excel(name = "工单ID")
    private Long workorderId;
    private String workorderCode;
    private String workorderName;
    private Long workstationId;
    private String workstationCode;
    private String workstationName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "退料日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date rtDate;
    @Excel(name = "总数")
    private BigDecimal quantityTotal;
    private String unitOfMeasure;
    private String unitName;
    private Long rqcId;
    private String rqcCode;
    @Excel(name = "状态")
    private String status;

    public Long getRtId() { return rtId; }
    public void setRtId(Long v) { this.rtId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getRtCode() { return rtCode; }
    public void setRtCode(String v) { this.rtCode = v; }
    public String getRtName() { return rtName; }
    public void setRtName(String v) { this.rtName = v; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }
    public String getIssueCode() { return issueCode; }
    public void setIssueCode(String v) { this.issueCode = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }
    public Long getWorkstationId() { return workstationId; }
    public void setWorkstationId(Long v) { this.workstationId = v; }
    public String getWorkstationCode() { return workstationCode; }
    public void setWorkstationCode(String v) { this.workstationCode = v; }
    public String getWorkstationName() { return workstationName; }
    public void setWorkstationName(String v) { this.workstationName = v; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }
    public Date getRtDate() { return rtDate; }
    public void setRtDate(Date v) { this.rtDate = v; }
    public BigDecimal getQuantityTotal() { return quantityTotal; }
    public void setQuantityTotal(BigDecimal v) { this.quantityTotal = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public Long getRqcId() { return rqcId; }
    public void setRqcId(Long v) { this.rqcId = v; }
    public String getRqcCode() { return rqcCode; }
    public void setRqcCode(String v) { this.rqcCode = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("rtId", getRtId())
            .append("factoryId", getFactoryId())
            .append("rtCode", getRtCode())
            .append("rtName", getRtName())
            .append("issueId", getIssueId())
            .append("issueCode", getIssueCode())
            .append("workorderId", getWorkorderId())
            .append("workorderCode", getWorkorderCode()).toString();
    }
}
