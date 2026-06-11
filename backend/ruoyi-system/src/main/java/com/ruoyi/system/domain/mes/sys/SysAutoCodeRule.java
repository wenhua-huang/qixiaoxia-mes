package com.ruoyi.system.domain.mes.sys;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 编码生成规则对象 sys_auto_code_rule
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public class SysAutoCodeRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 规则ID */
    private Long ruleId;

    /** 工厂ID */
    private Long factoryId;

    /** 规则编码 */
    @Excel(name = "规则编码")
    private String ruleCode;

    /** 规则名称 */
    @Excel(name = "规则名称")
    private String ruleName;

    /** 规则描述 */
    private String ruleDesc;

    /** 最大长度 */
    private Integer maxLength;

    /** 是否补齐(1-是,0-否) */
    @Excel(name = "是否补齐", readConverterExp = "1=是,0=否")
    private String isPadded;

    /** 补齐字符(如0) */
    private String paddedChar;

    /** 补齐方式(L-左补齐,R-右补齐) */
    private String paddedMethod;

    /** 是否启用(1-启用,0-停用) */
    @Excel(name = "是否启用", readConverterExp = "1=启用,0=停用")
    private String enableFlag;

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleDesc() { return ruleDesc; }
    public void setRuleDesc(String ruleDesc) { this.ruleDesc = ruleDesc; }

    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }

    public String getIsPadded() { return isPadded; }
    public void setIsPadded(String isPadded) { this.isPadded = isPadded; }

    public String getPaddedChar() { return paddedChar; }
    public void setPaddedChar(String paddedChar) { this.paddedChar = paddedChar; }

    public String getPaddedMethod() { return paddedMethod; }
    public void setPaddedMethod(String paddedMethod) { this.paddedMethod = paddedMethod; }

    public String getEnableFlag() { return enableFlag; }
    public void setEnableFlag(String enableFlag) { this.enableFlag = enableFlag; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("ruleId", getRuleId())
            .append("factoryId", getFactoryId())
            .append("ruleCode", getRuleCode())
            .append("ruleName", getRuleName())
            .append("ruleDesc", getRuleDesc())
            .append("maxLength", getMaxLength())
            .append("isPadded", getIsPadded())
            .append("paddedChar", getPaddedChar())
            .append("paddedMethod", getPaddedMethod())
            .append("enableFlag", getEnableFlag())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
