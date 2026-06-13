package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 杂项出库单表对象 qxx_wm_misc_issue
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmMiscIssue extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long issueId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "杂项出库单编码")
    private String issueCode;

    @Excel(name = "杂项出库单名称")
    private String issueName;

    @Excel(name = "出库原因:ADJUST-盘亏调整")
    private String issueType;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "出库日期")
    private Date issueDate;

    @Excel(name = "出库总数量")
    private BigDecimal totalQuantity;

    @Excel(name = "状态:DRAFT-草稿")
    private String status;

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

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Date getIssueDate() { return issueDate; }
    public void setIssueDate(Date v) { this.issueDate = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("issueId", getIssueId())
            .append("issueCode", getIssueCode())
            .append("issueName", getIssueName())
            .append("warehouseName", getWarehouseName())
            .toString();
    }
}