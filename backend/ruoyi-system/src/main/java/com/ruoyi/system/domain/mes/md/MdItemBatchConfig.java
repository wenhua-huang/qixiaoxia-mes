package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料批次属性配置对象 qxx_md_item_batch_config
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdItemBatchConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long configId;
    private Long factoryId;
    private Long itemId;

    private String produceDateFlag;
    private String expireDateFlag;
    private String recptDateFlag;
    private String vendorFlag;
    private String clientFlag;
    private String coCodeFlag;
    private String poCodeFlag;
    private String workorderFlag;
    private String taskFlag;
    private String workstationFlag;
    private String toolFlag;
    private String moldFlag;
    private String lotNumberFlag;
    private String qualityStatusFlag;
    private String paperRollFlag;
    private String paperWidthFlag;
    private String enableFlag;

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getProduceDateFlag() { return produceDateFlag; }
    public void setProduceDateFlag(String s) { this.produceDateFlag = s; }
    public String getExpireDateFlag() { return expireDateFlag; }
    public void setExpireDateFlag(String s) { this.expireDateFlag = s; }
    public String getRecptDateFlag() { return recptDateFlag; }
    public void setRecptDateFlag(String s) { this.recptDateFlag = s; }
    public String getVendorFlag() { return vendorFlag; }
    public void setVendorFlag(String s) { this.vendorFlag = s; }
    public String getClientFlag() { return clientFlag; }
    public void setClientFlag(String s) { this.clientFlag = s; }
    public String getCoCodeFlag() { return coCodeFlag; }
    public void setCoCodeFlag(String s) { this.coCodeFlag = s; }
    public String getPoCodeFlag() { return poCodeFlag; }
    public void setPoCodeFlag(String s) { this.poCodeFlag = s; }
    public String getWorkorderFlag() { return workorderFlag; }
    public void setWorkorderFlag(String s) { this.workorderFlag = s; }
    public String getTaskFlag() { return taskFlag; }
    public void setTaskFlag(String s) { this.taskFlag = s; }
    public String getWorkstationFlag() { return workstationFlag; }
    public void setWorkstationFlag(String s) { this.workstationFlag = s; }
    public String getToolFlag() { return toolFlag; }
    public void setToolFlag(String s) { this.toolFlag = s; }
    public String getMoldFlag() { return moldFlag; }
    public void setMoldFlag(String s) { this.moldFlag = s; }
    public String getLotNumberFlag() { return lotNumberFlag; }
    public void setLotNumberFlag(String s) { this.lotNumberFlag = s; }
    public String getQualityStatusFlag() { return qualityStatusFlag; }
    public void setQualityStatusFlag(String s) { this.qualityStatusFlag = s; }
    public String getPaperRollFlag() { return paperRollFlag; }
    public void setPaperRollFlag(String s) { this.paperRollFlag = s; }
    public String getPaperWidthFlag() { return paperWidthFlag; }
    public void setPaperWidthFlag(String s) { this.paperWidthFlag = s; }
    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String s) { this.enableFlag = s; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("itemId", getItemId())
            .toString();
    }
}
