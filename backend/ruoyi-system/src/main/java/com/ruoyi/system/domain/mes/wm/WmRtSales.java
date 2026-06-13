package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 销售退货单表对象 qxx_wm_rt_sales
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmRtSales extends BaseEntity
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

    @Excel(name = "原销售出库单ID")
    private Long salesId;

    @Excel(name = "原销售出库单编码")
    private String salesCode;

    @Excel(name = "客户ID")
    private Long clientId;

    @Excel(name = "客户编码")
    private String clientCode;

    @Excel(name = "客户名称")
    private String clientName;

    @Excel(name = "退货入库仓库ID")
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

    @Excel(name = "状态:DRAFT-草稿")
    private String status;

    public Long getRtId() { return rtId; }
    public void setRtId(Long v) { this.rtId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getRtCode() { return rtCode; }
    public void setRtCode(String v) { this.rtCode = v; }

    public String getRtName() { return rtName; }
    public void setRtName(String v) { this.rtName = v; }

    public Long getSalesId() { return salesId; }
    public void setSalesId(Long v) { this.salesId = v; }

    public String getSalesCode() { return salesCode; }
    public void setSalesCode(String v) { this.salesCode = v; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long v) { this.clientId = v; }

    public String getClientCode() { return clientCode; }
    public void setClientCode(String v) { this.clientCode = v; }

    public String getClientName() { return clientName; }
    public void setClientName(String v) { this.clientName = v; }

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

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("rtId", getRtId())
            .append("rtCode", getRtCode())
            .append("rtName", getRtName())
            .append("salesCode", getSalesCode())
            .toString();
    }
}