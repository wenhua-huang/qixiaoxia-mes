package com.ruoyi.system.domain.mes.pro;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 生产任务/排产对象 qxx_pro_task
 *
 * @author qixiaoxia
 * @date 2026-06-20
 */
public class ProTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long taskId;
    @Excel(name = "工厂ID") private Long factoryId;
    @Excel(name = "任务编码") private String taskCode;
    @Excel(name = "任务名称") private String taskName;
    @Excel(name = "工单ID") private Long workorderId;
    @Excel(name = "工单编码") private String workorderCode;
    @Excel(name = "工单名称") private String workorderName;
    @Excel(name = "工作站ID") private Long workstationId;
    @Excel(name = "工作站编码") private String workstationCode;
    @Excel(name = "工作站名称") private String workstationName;
    @Excel(name = "工艺路线ID") private Long routeId;
    @Excel(name = "工艺路线编码") private String routeCode;
    @Excel(name = "工序ID") private Long processId;
    @Excel(name = "工序编码") private String processCode;
    @Excel(name = "工序名称") private String processName;
    @Excel(name = "产品物料ID") private Long itemId;
    @Excel(name = "产品物料编码") private String itemCode;
    @Excel(name = "产品物料名称") private String itemName;
    @Excel(name = "规格型号") private String specification;
    @Excel(name = "主单位编码") private String unitOfMeasure;
    @Excel(name = "主单位名称") private String unitName;
    @Excel(name = "排产数量") private BigDecimal quantity;
    @Excel(name = "已生产数量") private BigDecimal quantityProduced;
    @Excel(name = "合格品数量") private BigDecimal quantityQualified;
    @Excel(name = "不合格品数量") private BigDecimal quantityUnqualified;
    @Excel(name = "调整数量") private BigDecimal quantityChanged;
    private Long clientId;
    private String clientCode;
    private String clientName;
    private String clientNick;
    @Excel(name = "机台号") private String machineCode;
    @Excel(name = "调机时长") private Integer setupDuration;
    @Excel(name = "单位耗时") private BigDecimal unitDuration;
    @Excel(name = "下机个数") private Integer offlineQty;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @Excel(name = "计划时长") private Integer duration;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    private String colorCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date requestDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelDate;
    private Long vendorId;
    private String vendorCode;
    private Long outsourceFactoryId;
    @Excel(name = "状态") private String status;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long v) { this.taskId = v; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }
    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String v) { this.taskCode = v; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String v) { this.taskName = v; }
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
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long v) { this.routeId = v; }
    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String v) { this.routeCode = v; }
    public Long getProcessId() { return processId; }
    public void setProcessId(Long v) { this.processId = v; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String v) { this.processCode = v; }
    public String getProcessName() { return processName; }
    public void setProcessName(String v) { this.processName = v; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }
    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }
    public String getSpecification() { return specification; }
    public void setSpecification(String v) { this.specification = v; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String v) { this.unitOfMeasure = v; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String v) { this.unitName = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { this.quantity = v; }
    public BigDecimal getQuantityProduced() { return quantityProduced; }
    public void setQuantityProduced(BigDecimal v) { this.quantityProduced = v; }
    public BigDecimal getQuantityQualified() { return quantityQualified; }
    public void setQuantityQualified(BigDecimal v) { this.quantityQualified = v; }
    public BigDecimal getQuantityUnqualified() { return quantityUnqualified; }
    public void setQuantityUnqualified(BigDecimal v) { this.quantityUnqualified = v; }
    public BigDecimal getQuantityChanged() { return quantityChanged; }
    public void setQuantityChanged(BigDecimal v) { this.quantityChanged = v; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long v) { this.clientId = v; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { this.clientCode = v; }
    public String getClientName() { return clientName; }
    public void setClientName(String v) { this.clientName = v; }
    public String getClientNick() { return clientNick; }
    public void setClientNick(String v) { this.clientNick = v; }
    public String getMachineCode() { return machineCode; }
    public void setMachineCode(String v) { this.machineCode = v; }
    public Integer getSetupDuration() { return setupDuration; }
    public void setSetupDuration(Integer v) { this.setupDuration = v; }
    public BigDecimal getUnitDuration() { return unitDuration; }
    public void setUnitDuration(BigDecimal v) { this.unitDuration = v; }
    public Integer getOfflineQty() { return offlineQty; }
    public void setOfflineQty(Integer v) { this.offlineQty = v; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date v) { this.startTime = v; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer v) { this.duration = v; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date v) { this.endTime = v; }
    public String getColorCode() { return colorCode; }
    public void setColorCode(String v) { this.colorCode = v; }
    public Date getRequestDate() { return requestDate; }
    public void setRequestDate(Date v) { this.requestDate = v; }
    public Date getFinishDate() { return finishDate; }
    public void setFinishDate(Date v) { this.finishDate = v; }
    public Date getCancelDate() { return cancelDate; }
    public void setCancelDate(Date v) { this.cancelDate = v; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }
    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }
    public Long getOutsourceFactoryId() { return outsourceFactoryId; }
    public void setOutsourceFactoryId(Long v) { this.outsourceFactoryId = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId()).append("taskCode", getTaskCode())
            .append("taskName", getTaskName()).append("workorderId", getWorkorderId())
            .append("quantity", getQuantity()).append("status", getStatus())
            .append("createTime", getCreateTime()).toString();
    }
}
