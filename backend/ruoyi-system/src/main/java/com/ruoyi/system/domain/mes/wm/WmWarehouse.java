package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 仓库表对象 qxx_wm_warehouse
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmWarehouse extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long warehouseId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "仓库类型:RAW-原料仓")
    private String warehouseType;

    @Excel(name = "仓库位置/地址")
    private String address;

    @Excel(name = "仓库面积")
    private BigDecimal area;

    @Excel(name = "仓库负责人")
    private String charge;

    @Excel(name = "是否启用")
    private String enableFlag;

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public String getWarehouseType() { return warehouseType; }
    public void setWarehouseType(String v) { this.warehouseType = v; }

    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal v) { this.area = v; }

    public String getCharge() { return charge; }
    public void setCharge(String v) { this.charge = v; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String v) { this.enableFlag = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("warehouseId", getWarehouseId())
            .append("warehouseCode", getWarehouseCode())
            .append("warehouseName", getWarehouseName())
            .toString();
    }
}