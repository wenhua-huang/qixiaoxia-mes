package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 调拨转移单表对象 qxx_wm_transfer
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmTransfer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long transferId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "调拨单编码")
    private String transferCode;

    @Excel(name = "调拨单名称")
    private String transferName;

    @Excel(name = "来源仓库ID")
    private Long sourceWarehouseId;

    @Excel(name = "来源仓库编码")
    private String sourceWarehouseCode;

    @Excel(name = "来源仓库名称")
    private String sourceWarehouseName;

    @Excel(name = "目标仓库ID")
    private Long targetWarehouseId;

    @Excel(name = "目标仓库编码")
    private String targetWarehouseCode;

    @Excel(name = "目标仓库名称")
    private String targetWarehouseName;

    @Excel(name = "调拨日期")
    private Date transferDate;

    @Excel(name = "调拨总数量")
    private BigDecimal totalQuantity;

    @Excel(name = "状态:DRAFT-草稿")
    private String status;

    public Long getTransferId() { return transferId; }
    public void setTransferId(Long v) { this.transferId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getTransferCode() { return transferCode; }
    public void setTransferCode(String v) { this.transferCode = v; }

    public String getTransferName() { return transferName; }
    public void setTransferName(String v) { this.transferName = v; }

    public Long getSourceWarehouseId() { return sourceWarehouseId; }
    public void setSourceWarehouseId(Long v) { this.sourceWarehouseId = v; }

    public String getSourceWarehouseCode() { return sourceWarehouseCode; }
    public void setSourceWarehouseCode(String v) { this.sourceWarehouseCode = v; }

    public String getSourceWarehouseName() { return sourceWarehouseName; }
    public void setSourceWarehouseName(String v) { this.sourceWarehouseName = v; }

    public Long getTargetWarehouseId() { return targetWarehouseId; }
    public void setTargetWarehouseId(Long v) { this.targetWarehouseId = v; }

    public String getTargetWarehouseCode() { return targetWarehouseCode; }
    public void setTargetWarehouseCode(String v) { this.targetWarehouseCode = v; }

    public String getTargetWarehouseName() { return targetWarehouseName; }
    public void setTargetWarehouseName(String v) { this.targetWarehouseName = v; }

    public Date getTransferDate() { return transferDate; }
    public void setTransferDate(Date v) { this.transferDate = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("transferId", getTransferId())
            .append("transferCode", getTransferCode())
            .append("transferName", getTransferName())
            .append("sourceWarehouseCode", getSourceWarehouseCode())
            .toString();
    }
}