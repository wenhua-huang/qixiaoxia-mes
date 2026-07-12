package com.ruoyi.system.domain.mes.wm;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 产品入库单表对象 qxx_wm_product_recpt
 *
 * @author qixiaoxia
 * @date 2026-07-11
 */
public class WmProductRecpt extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long recptId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "入库单编码")
    private String recptCode;

    @Excel(name = "入库单名称")
    private String recptName;

    @Excel(name = "生产产品ID")
    private Long produceId;

    @Excel(name = "生产产品编码")
    private String produceCode;

    @Excel(name = "关联工单ID")
    private Long workorderId;

    @Excel(name = "关联工单编码")
    private String workorderCode;

    @Excel(name = "来源仓库ID")
    private Long sourceWarehouseId;

    @Excel(name = "入库仓库ID")
    private Long warehouseId;

    @Excel(name = "仓库编码")
    private String warehouseCode;

    @Excel(name = "仓库名称")
    private String warehouseName;

    @Excel(name = "入库日期")
    private Date recptDate;

    @Excel(name = "入库总数量")
    private BigDecimal totalQuantity;

    @Excel(name = "总箱数")
    private Integer totalBox;

    @Excel(name = "IPQC质检单ID")
    private Long ipqcId;

    @Excel(name = "IPQC质检单编码")
    private String ipqcCode;

    @Excel(name = "状态:DRAFT-草稿")
    private String status;

    /** 入库单行列表（详情接口返回，非持久化字段） */
    private List<WmProductRecptLine> lines;

    public Long getRecptId() { return recptId; }
    public void setRecptId(Long v) { this.recptId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getRecptCode() { return recptCode; }
    public void setRecptCode(String v) { this.recptCode = v; }

    public String getRecptName() { return recptName; }
    public void setRecptName(String v) { this.recptName = v; }

    public Long getProduceId() { return produceId; }
    public void setProduceId(Long v) { this.produceId = v; }

    public String getProduceCode() { return produceCode; }
    public void setProduceCode(String v) { this.produceCode = v; }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }

    public Long getSourceWarehouseId() { return sourceWarehouseId; }
    public void setSourceWarehouseId(Long v) { this.sourceWarehouseId = v; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long v) { this.warehouseId = v; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String v) { this.warehouseCode = v; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String v) { this.warehouseName = v; }

    public Date getRecptDate() { return recptDate; }
    public void setRecptDate(Date v) { this.recptDate = v; }

    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(BigDecimal v) { this.totalQuantity = v; }

    public Integer getTotalBox() { return totalBox; }
    public void setTotalBox(Integer v) { this.totalBox = v; }

    public Long getIpqcId() { return ipqcId; }
    public void setIpqcId(Long v) { this.ipqcId = v; }

    public String getIpqcCode() { return ipqcCode; }
    public void setIpqcCode(String v) { this.ipqcCode = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    public List<WmProductRecptLine> getLines() { return lines; }
    public void setLines(List<WmProductRecptLine> lines) { this.lines = lines; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("recptId", getRecptId())
            .append("recptCode", getRecptCode())
            .append("recptName", getRecptName())
            .append("workorderCode", getWorkorderCode())
            .toString();
    }
}
