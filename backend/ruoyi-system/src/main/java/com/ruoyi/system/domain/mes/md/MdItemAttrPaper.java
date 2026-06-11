package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物料纸张属性对象 qxx_md_item_attr_paper
 *
 * @author qixiaoxia
 * @date 2026-06-10
 */
public class MdItemAttrPaper extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long attrId;
    private Long factoryId;
    private Long itemId;
    private String paperWidth;
    private String paperWeight;
    private String paperSource;
    private String paperType;

    public Long getAttrId() { return attrId; }
    public void setAttrId(Long attrId) { this.attrId = attrId; }
    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getPaperWidth() { return paperWidth; }
    public void setPaperWidth(String paperWidth) { this.paperWidth = paperWidth; }
    public String getPaperWeight() { return paperWeight; }
    public void setPaperWeight(String paperWeight) { this.paperWeight = paperWeight; }
    public String getPaperSource() { return paperSource; }
    public void setPaperSource(String paperSource) { this.paperSource = paperSource; }
    public String getPaperType() { return paperType; }
    public void setPaperType(String paperType) { this.paperType = paperType; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("attrId", getAttrId())
            .append("itemId", getItemId())
            .append("paperWidth", getPaperWidth())
            .append("paperWeight", getPaperWeight())
            .toString();
    }
}
