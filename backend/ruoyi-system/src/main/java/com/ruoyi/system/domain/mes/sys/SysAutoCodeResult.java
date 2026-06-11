package com.ruoyi.system.domain.mes.sys;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 编码生成记录对象 sys_auto_code_result
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public class SysAutoCodeResult extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long codeId;

    /** 工厂ID */
    private Long factoryId;

    /** 规则ID(关联sys_auto_code_rule) */
    private Long ruleId;

    /** 生成日期时间(用于循环判断) */
    private String genDate;

    /** 最后产生的序号 */
    private Integer genIndex;

    /** 最后产生的完整编码值 */
    private String lastResult;

    /** 最后产生的流水号 */
    private Integer lastSerialNo;

    /** 最后传入的参数(循环方式为OTHER时使用) */
    private String lastInputChar;

    public Long getCodeId() { return codeId; }
    public void setCodeId(Long codeId) { this.codeId = codeId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public String getGenDate() { return genDate; }
    public void setGenDate(String genDate) { this.genDate = genDate; }

    public Integer getGenIndex() { return genIndex; }
    public void setGenIndex(Integer genIndex) { this.genIndex = genIndex; }

    public String getLastResult() { return lastResult; }
    public void setLastResult(String lastResult) { this.lastResult = lastResult; }

    public Integer getLastSerialNo() { return lastSerialNo; }
    public void setLastSerialNo(Integer lastSerialNo) { this.lastSerialNo = lastSerialNo; }

    public String getLastInputChar() { return lastInputChar; }
    public void setLastInputChar(String lastInputChar) { this.lastInputChar = lastInputChar; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("codeId", getCodeId())
            .append("factoryId", getFactoryId())
            .append("ruleId", getRuleId())
            .append("genDate", getGenDate())
            .append("genIndex", getGenIndex())
            .append("lastResult", getLastResult())
            .append("lastSerialNo", getLastSerialNo())
            .append("lastInputChar", getLastInputChar())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
