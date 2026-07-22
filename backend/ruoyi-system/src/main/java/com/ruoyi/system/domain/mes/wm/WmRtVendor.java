package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 采购退货单表对象 qxx_wm_rt_vendor
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmRtVendor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long rtId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "退货单编码")
    private String rtCode;

    @Excel(name = "退货单名称")
    private String rtName;

    @Excel(name = "原入库单ID")
    private Long recptId;

    @Excel(name = "原入库单编码")
    private String recptCode;

    @Excel(name = "供应商ID")
    private Long vendorId;

    @Excel(name = "供应商编码")
    private String vendorCode;

    @Excel(name = "供应商名称")
    private String vendorName;

    @Excel(name = "退货仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "退货日期")
    private Date rtDate;

    @Excel(name = "退货总数量")
    private BigDecimal totalQuantity;

    @Excel(name = "退料质检单ID")
    private Long rqcId;

    @Excel(name = "退料质检单编码")
    private String rqcCode;

    @Excel(name = "状态:DRAFT-草稿,CONFIRMED-已确认,POSTED-已过账")
    private String status;

    @Excel(name = "采购订单ID")
    private Long purOrderId;

    @Excel(name = "采购订单编码")
    private String purOrderCode;

    /** 确认时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;

    /** 确认人 */
    private String confirmBy;

    /** 过账时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date postTime;

    /** 过账人 */
    private String postBy;

    public Long getRtId() { return rtId; }
    public void setRtId(Long v) { this.rtId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getRtCode() { return rtCode; }
    public void setRtCode(String v) { this.rtCode = v; }

    public String getRtName() { return rtName; }
    public void setRtName(String v) { this.rtName = v; }

    public Long getRecptId() { return recptId; }
    public void setRecptId(Long v) { this.recptId = v; }

    public String getRecptCode() { return recptCode; }
    public void setRecptCode(String v) { this.recptCode = v; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Date getRtDate() { return rtDate; }
    public void setRtDate(Date v) { this.rtDate = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public Long getRqcId() { return rqcId; }
    public void setRqcId(Long v) { this.rqcId = v; }

    public String getRqcCode() { return rqcCode; }
    public void setRqcCode(String v) { this.rqcCode = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public Long getPurOrderId() { return purOrderId; }
    public void setPurOrderId(Long v) { this.purOrderId = v; }

    public String getPurOrderCode() { return purOrderCode; }
    public void setPurOrderCode(String v) { this.purOrderCode = v; }

    public Date getConfirmTime() { return confirmTime; }
    public void setConfirmTime(Date v) { this.confirmTime = v; }

    public String getConfirmBy() { return confirmBy; }
    public void setConfirmBy(String v) { this.confirmBy = v; }

    public Date getPostTime() { return postTime; }
    public void setPostTime(Date v) { this.postTime = v; }

    public String getPostBy() { return postBy; }
    public void setPostBy(String v) { this.postBy = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("rtId", getRtId())
            .append("rtCode", getRtCode())
            .append("rtName", getRtName())
            .append("recptCode", getRecptCode())
            .toString();
    }
}