package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 销售出库-发运单对象 qxx_wm_product_sales_shipment
 * 一张出库单可多次发运（部分发货），每次发运 = 一条记录
 *
 * @author qixiaoxia
 * @date 2026-07-27
 */
public class WmProductSalesShipment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long shipmentId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "出库单ID")
    private Long salesId;

    @Excel(name = "发运单号")
    private String shipmentCode;

    @Excel(name = "发货方式")
    private String shipMethod;

    @Excel(name = "物流公司")
    private String logisticsCompany;

    @Excel(name = "运单号")
    private String trackingNo;

    @Excel(name = "物流费用")
    private BigDecimal logisticsFee;

    @Excel(name = "车牌号")
    private String vehicleNo;

    @Excel(name = "司机姓名")
    private String driverName;

    @Excel(name = "司机电话")
    private String driverTel;

    @Excel(name = "收货人")
    private String receiverName;

    @Excel(name = "收货电话")
    private String receiverTel;

    @Excel(name = "收货地址")
    private String shippingAddress;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Excel(name = "计划发货日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date planShipDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "实际发货时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date actualShipDate;

    @Excel(name = "本次发运量")
    private BigDecimal shippedQuantity;

    @Excel(name = "箱数")
    private Long boxCount;

    @Excel(name = "发运状态")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Excel(name = "签收时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date receivedTime;

    @Excel(name = "签收人")
    private String receivedBy;

    @Excel(name = "签收备注")
    private String receivedRemark;

    @Excel(name = "回单附件URL")
    private String attachmentUrl;

    /** 关联装箱列表（详情接口聚合，非DB字段） */
    private List<WmProductSalesBox> boxes;

    public Long getShipmentId() { return shipmentId; }
    public void setShipmentId(Long v) { this.shipmentId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public Long getSalesId() { return salesId; }
    public void setSalesId(Long v) { this.salesId = v; }

    public String getShipmentCode() { return shipmentCode; }
    public void setShipmentCode(String v) { this.shipmentCode = v; }

    public String getShipMethod() { return shipMethod; }
    public void setShipMethod(String v) { this.shipMethod = v; }

    public String getLogisticsCompany() { return logisticsCompany; }
    public void setLogisticsCompany(String v) { this.logisticsCompany = v; }

    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String v) { this.trackingNo = v; }

    public BigDecimal getLogisticsFee() { return logisticsFee; }
    public void setLogisticsFee(BigDecimal v) { this.logisticsFee = v; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String v) { this.vehicleNo = v; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String v) { this.driverName = v; }

    public String getDriverTel() { return driverTel; }
    public void setDriverTel(String v) { this.driverTel = v; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String v) { this.receiverName = v; }

    public String getReceiverTel() { return receiverTel; }
    public void setReceiverTel(String v) { this.receiverTel = v; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String v) { this.shippingAddress = v; }

    public Date getPlanShipDate() { return planShipDate; }
    public void setPlanShipDate(Date v) { this.planShipDate = v; }

    public Date getActualShipDate() { return actualShipDate; }
    public void setActualShipDate(Date v) { this.actualShipDate = v; }

    public BigDecimal getShippedQuantity() { return shippedQuantity; }
    public void setShippedQuantity(BigDecimal v) { this.shippedQuantity = v; }

    public Long getBoxCount() { return boxCount; }
    public void setBoxCount(Long v) { this.boxCount = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Date getReceivedTime() { return receivedTime; }
    public void setReceivedTime(Date v) { this.receivedTime = v; }

    public String getReceivedBy() { return receivedBy; }
    public void setReceivedBy(String v) { this.receivedBy = v; }

    public String getReceivedRemark() { return receivedRemark; }
    public void setReceivedRemark(String v) { this.receivedRemark = v; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String v) { this.attachmentUrl = v; }

    public List<WmProductSalesBox> getBoxes() { return boxes; }
    public void setBoxes(List<WmProductSalesBox> v) { this.boxes = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("shipmentId", getShipmentId())
            .append("shipmentCode", getShipmentCode())
            .append("salesId", getSalesId())
            .append("status", getStatus())
            .toString();
    }
}
