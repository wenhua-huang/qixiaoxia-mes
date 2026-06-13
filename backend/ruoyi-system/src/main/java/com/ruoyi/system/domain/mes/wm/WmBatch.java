package com.ruoyi.system.domain.mes.wm;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 批次记录表对象 qxx_wm_batch
 * 
 * @author qixiaoxia
 * @date 2026-06-12
 */
public class WmBatch extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "ID")
    private Long batchId;

    @Excel(name = "工厂ID")
    private Long factoryId;

    @Excel(name = "批次编码")
    private String batchCode;

    @Excel(name = "批次名称")
    private String batchName;

    @Excel(name = "物料ID")
    private Long itemId;

    @Excel(name = "物料编码")
    private String itemCode;

    @Excel(name = "物料名称")
    private String itemName;

    @Excel(name = "规格型号")
    private String specification;

    @Excel(name = "生产日期")
    private Date produceDate;

    @Excel(name = "有效期至")
    private Date expireDate;

    @Excel(name = "入库日期")
    private Date recptDate;

    @Excel(name = "供应商ID")
    private Long vendorId;

    @Excel(name = "供应商编码")
    private String vendorCode;

    @Excel(name = "供应商名称")
    private String vendorName;

    @Excel(name = "生产工单ID")
    private Long workorderId;

    @Excel(name = "生产工单编码")
    private String workorderCode;

    @Excel(name = "生产批号")
    private String lotNumber;

    @Excel(name = "质量状态:NORMAL-正常")
    private String qualityStatus;

    @Excel(name = "批次状态:ACTIVE-活跃")
    private String status;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long v) { this.batchId = v; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long v) { this.factoryId = v; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String v) { this.batchCode = v; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String v) { this.batchName = v; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long v) { this.itemId = v; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String v) { this.itemCode = v; }

    public String getItemName() { return itemName; }
    public void setItemName(String v) { this.itemName = v; }

    public String getSpecification() { return specification; }
    public void setSpecification(String v) { this.specification = v; }

    public Date getProduceDate() { return produceDate; }
    public void setProduceDate(Date v) { this.produceDate = v; }

    public Date getExpireDate() { return expireDate; }
    public void setExpireDate(Date v) { this.expireDate = v; }

    public Date getRecptDate() { return recptDate; }
    public void setRecptDate(Date v) { this.recptDate = v; }

    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long v) { this.vendorId = v; }

    public String getVendorCode() { return vendorCode; }
    public void setVendorCode(String v) { this.vendorCode = v; }

    public String getVendorName() { return vendorName; }
    public void setVendorName(String v) { this.vendorName = v; }

    public Long getWorkorderId() { return workorderId; }
    public void setWorkorderId(Long v) { this.workorderId = v; }

    public String getWorkorderCode() { return workorderCode; }
    public void setWorkorderCode(String v) { this.workorderCode = v; }

    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String v) { this.lotNumber = v; }

    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String v) { this.qualityStatus = v; }

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("batchId", getBatchId())
            .append("batchCode", getBatchCode())
            .append("batchName", getBatchName())
            .append("itemCode", getItemCode())
            .toString();
    }
}