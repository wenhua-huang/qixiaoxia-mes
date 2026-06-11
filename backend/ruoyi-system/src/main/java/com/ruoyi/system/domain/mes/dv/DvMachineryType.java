package com.ruoyi.system.domain.mes.dv;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备类型对象 qxx_dv_machinery_type
 *
 * @author qixiaoxia
 * @date 2026-06-11
 */
public class DvMachineryType extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备类型ID */
    private Long machineryTypeId;

    /** 工厂ID */
    private Long factoryId;

    /** 设备类型编码 */
    @Excel(name = "设备类型编码")
    private String machineryTypeCode;

    /** 设备类型名称 */
    @Excel(name = "设备类型名称")
    private String machineryTypeName;

    /** 父类型ID(0=根节点) */
    private Long parentTypeId;

    /** 同级排序号 */
    @Excel(name = "排序号")
    private Integer orderNum;

    /** 是否启用(1-是,0-否) */
    @Excel(name = "是否启用", readConverterExp = "1=是,0=否")
    private String enableFlag;

    /** 子类型列表（内存树结构） */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<DvMachineryType> children = new ArrayList<>();

    public Long getMachineryTypeId() { return machineryTypeId; }
    public void setMachineryTypeId(Long machineryTypeId) { this.machineryTypeId = machineryTypeId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public String getMachineryTypeCode() { return machineryTypeCode; }
    public void setMachineryTypeCode(String machineryTypeCode) { this.machineryTypeCode = machineryTypeCode; }

    public String getMachineryTypeName() { return machineryTypeName; }
    public void setMachineryTypeName(String machineryTypeName) { this.machineryTypeName = machineryTypeName; }

    public Long getParentTypeId() { return parentTypeId; }
    public void setParentTypeId(Long parentTypeId) { this.parentTypeId = parentTypeId; }

    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }

    public List<DvMachineryType> getChildren() { return children; }
    public void setChildren(List<DvMachineryType> children) { this.children = children; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("machineryTypeId", getMachineryTypeId())
            .append("factoryId", getFactoryId())
            .append("machineryTypeCode", getMachineryTypeCode())
            .append("machineryTypeName", getMachineryTypeName())
            .append("parentTypeId", getParentTypeId())
            .append("orderNum", getOrderNum())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
