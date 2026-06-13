package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 杂项入库单表对象 qxx_wm_misc_recpt
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmMiscRecpt extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long recptId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "杂项入库单编码")
    private String recptCode;

    @Excel(name = "杂项入库单名称")
    private String recptName;

    @Excel(name = "入库原因:OPENING-期初导入")
    private String recptType;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "入库日期")
    private Date recptDate;

    @Excel(name = "入库总数量")
    private BigDecimal totalQuantity;

    @Excel(name = "状态:DRAFT-草稿")
    private String status;

    public Long getRecptId() { return recptId; }
    public void setRecptId(Long v) { this.recptId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getRecptCode() { return recptCode; }
    public void setRecptCode(String v) { this.recptCode = v; }

    public String getRecptName() { return recptName; }
    public void setRecptName(String v) { this.recptName = v; }

    public String getRecptType() { return recptType; }
    public void setRecptType(String v) { this.recptType = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Date getRecptDate() { return recptDate; }
    public void setRecptDate(Date v) { this.recptDate = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recptId", getRecptId())
            .append("recptCode", getRecptCode())
            .append("recptName", getRecptName())
            .append("warehouseName", getWarehouseName())
            .toString();
    }
}