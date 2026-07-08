package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * qxx_wm_issue_header 对象
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class WmIssueHeader extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long issueId;
    private Long factoryId;
    @Excel(name = "领料单编码")
    private String issueCode;
    @Excel(name = "领料单名称")
    private String issueName;
    @Excel(name = "领料类型")
    private String issueType;
    @Excel(name = "工单ID")
    private Long workorderId;
    private String workorderCode;
    private String workorderName;
    private Long taskId;
    private String taskCode;
    private Long workstationId;
    private String workstationCode;
    private String workstationName;
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Long locationId;
    private String locationCode;
    private String locationName;
    private Long areaId;
    private String areaCode;
    private String areaName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "领料日期", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date issueDate;
    @Excel(name = "申请总数")
    private BigDecimal quantityTotal;
    @Excel(name = "已发料累计")
    private BigDecimal quantityIssuedTotal;
    @Excel(name = "已收料累计")
    private BigDecimal quantityReceived;
    private Integer version;
    @Excel(name = "状态")
    private String status;
    private String approveBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;
    private String cancelReason;

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long v) { this.issueId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getIssueCode() { return issueCode; }
    public void setIssueCode(String v) { this.issueCode = v; }
    public String getIssueName() { return issueName; }
    public void setIssueName(String v) { this.issueName = v; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String v) { this.issueType = v; }
    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }
    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }
    public String getWorkorderName() { return workorderName; }
    public void setWorkorderName(String v) { this.workorderName = v; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String v) { this.taskCode = v; }
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
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long v) { this.locationId = v; }
    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String v) { this.locationCode = v; }
    public String getLocationName() { return locationName; }
    public void setLocationName(String v) { this.locationName = v; }
    public Long getAreaId() { return areaId; }
    public void setAreaId(Long v) { this.areaId = v; }
    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String v) { this.areaCode = v; }
    public String getAreaName() { return areaName; }
    public void setAreaName(String v) { this.areaName = v; }
    public Date getIssueDate() { return issueDate; }
    public void setIssueDate(Date v) { this.issueDate = v; }
    public BigDecimal getQuantityTotal() { return quantityTotal; }
    public void setQuantityTotal(BigDecimal v) { this.quantityTotal = v; }
    public BigDecimal getQuantityIssuedTotal() { return quantityIssuedTotal; }
    public void setQuantityIssuedTotal(BigDecimal v) { this.quantityIssuedTotal = v; }
    public BigDecimal getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(BigDecimal v) { this.quantityReceived = v; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { this.version = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getApproveBy() { return approveBy; }
    public void setApproveBy(String v) { this.approveBy = v; }
    public Date getApproveTime() { return approveTime; }
    public void setApproveTime(Date v) { this.approveTime = v; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String v) { this.cancelReason = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("issueId", getIssueId())
            .append("factoryId", getFactoryId())
            .append("issueCode", getIssueCode())
            .append("issueName", getIssueName())
            .append("issueType", getIssueType())
            .append("workorderId", getWorkorderId())
            .append("workorderCode", getWorkorderCode())
            .append("workorderName", getWorkorderName())
            .append("taskId", getTaskId()).toString();
    }
}
