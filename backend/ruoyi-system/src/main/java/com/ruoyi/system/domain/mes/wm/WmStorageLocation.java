package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库区表对象 qxx_wm_storage_location
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmStorageLocation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long locationId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "库区编码")
    private String locationCode;

    @Excel(name = "库区名称")
    private String locationName;

    @Excel(name = "仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "库区面积")
    private BigDecimal area;

    @Excel(name = "是否启用")
    private String enableFlag;

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long v) { this.locationId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getLocationCode() { return locationCode; }
    public void setLocationCode(String v) { this.locationCode = v; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String v) { this.locationName = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal v) { this.area = v; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String v) { this.enableFlag = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("locationId", getLocationId())
            .append("locationCode", getLocationCode())
            .append("locationName", getLocationName())
            .append("warehouseCode", getWarehouseCode())
            .toString();
    }
}