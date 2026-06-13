package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 装箱单表对象 qxx_wm_package
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmPackage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long packageId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "装箱单编码")
    private String packageCode;

    @Excel(name = "装箱单名称")
    private String packageName;

    @Excel(name = "父装箱ID")
    private Long parentId;

    @Excel(name = "所有父节点ID")
    private String ancestors;

    @Excel(name = "装箱类型:CARTON-纸箱")
    private String packageType;

    @Excel(name = "生产工单ID")
    private Long workorderId;

    @Excel(name = "生产工单编码")
    private String workorderCode;

    @Excel(name = "每箱标准数量")
    private Long quantityPerBox;

    @Excel(name = "箱数")
    private Long boxCount;

    @Excel(name = "总数量")
    private BigDecimal totalQuantity;

    @Excel(name = "重量")
    private BigDecimal weight;

    @Excel(name = "长")
    private BigDecimal length;

    @Excel(name = "宽")
    private BigDecimal width;

    @Excel(name = "高")
    private BigDecimal height;

    @Excel(name = "体积")
    private BigDecimal volume;

    @Excel(name = "箱唛/唛头")
    private String boxLabel;

    @Excel(name = "状态:OPEN-开放")
    private String status;

    public Long getPackageId() { return packageId; }
    public void setPackageId(Long v) { this.packageId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getPackageCode() { return packageCode; }
    public void setPackageCode(String v) { this.packageCode = v; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String v) { this.packageName = v; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { this.parentId = v; }

    public String getAncestors() { return ancestors; }
    public void setAncestors(String v) { this.ancestors = v; }

    public String getPackageType() { return packageType; }
    public void setPackageType(String v) { this.packageType = v; }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }

    public Long getQuantityPerBox() { return quantityPerBox; }
    public void setQuantityPerBox(Long v) { this.quantityPerBox = v; }

    public Long getBoxCount() { return boxCount; }
    public void setBoxCount(Long v) { this.boxCount = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal v) { this.weight = v; }

    public BigDecimal getLength() { return length; }
    public void setLength(BigDecimal v) { this.length = v; }

    public BigDecimal getWidth() { return width; }
    public void setWidth(BigDecimal v) { this.width = v; }

    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal v) { this.height = v; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal v) { this.volume = v; }

    public String getBoxLabel() { return boxLabel; }
    public void setBoxLabel(String v) { this.boxLabel = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("packageId", getPackageId())
            .append("packageCode", getPackageCode())
            .append("packageName", getPackageName())
            .append("workorderCode", getWorkorderCode())
            .toString();
    }
}