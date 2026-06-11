package com.ruoyi.system.domain.mes.sys;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 编码生成规则组成对象 sys_auto_code_part
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public class SysAutoCodePart extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 分段ID */
    private Long partId;

    /** 工厂ID */
    private Long factoryId;

    /** 规则ID(关联sys_auto_code_rule) */
    private Long ruleId;

    /** 分段序号(从1开始) */
    private Integer partIndex;

    /** 分段类型:INPUTCHAR-输入字符,NOWDATE-当前日期,FIXCHAR-固定字符,SERIALNO-流水号 */
    @Excel(name = "分段类型", readConverterExp = "INPUTCHAR=输入字符,NOWDATE=当前日期,FIXCHAR=固定字符,SERIALNO=流水号")
    private String partType;

    /** 分段编码 */
    @Excel(name = "分段编码")
    private String partCode;

    /** 分段名称 */
    @Excel(name = "分段名称")
    private String partName;

    /** 分段长度 */
    private Integer partLength;

    /** 日期时间格式(如yyyyMMdd) */
    private String dateFormat;

    /** 输入字符(INPUTCHAR类型时使用) */
    private String inputCharacter;

    /** 固定字符(FIXCHAR类型时使用) */
    private String fixCharacter;

    /** 流水号起始值 */
    private Integer seriaStartNo;

    /** 流水号步长 */
    private Integer seriaStep;

    /** 流水号当前值 */
    private Integer seriaNowNo;

    /** 流水号是否循环(1-是,0-否) */
    private String cycleFlag;

    /** 循环方式:YEAR-按年,MONTH-按月,DAY-按天,HOUR-按小时,MINUTE-按分钟,OTHER-按传入字符变 */
    private String cycleMethod;

    public Long getPartId() { return partId; }
    public void setPartId(Long partId) { this.partId = partId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public Integer getPartIndex() { return partIndex; }
    public void setPartIndex(Integer partIndex) { this.partIndex = partIndex; }

    public String getPartType() { return partType; }
    public void setPartType(String partType) { this.partType = partType; }

    public String getPartCode() { return partCode; }
    public void setPartCode(String partCode) { this.partCode = partCode; }

    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }

    public Integer getPartLength() { return partLength; }
    public void setPartLength(Integer partLength) { this.partLength = partLength; }

    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }

    public String getInputCharacter() { return inputCharacter; }
    public void setInputCharacter(String inputCharacter) { this.inputCharacter = inputCharacter; }

    public String getFixCharacter() { return fixCharacter; }
    public void setFixCharacter(String fixCharacter) { this.fixCharacter = fixCharacter; }

    public Integer getSeriaStartNo() { return seriaStartNo; }
    public void setSeriaStartNo(Integer seriaStartNo) { this.seriaStartNo = seriaStartNo; }

    public Integer getSeriaStep() { return seriaStep; }
    public void setSeriaStep(Integer seriaStep) { this.seriaStep = seriaStep; }

    public Integer getSeriaNowNo() { return seriaNowNo; }
    public void setSeriaNowNo(Integer seriaNowNo) { this.seriaNowNo = seriaNowNo; }

    public String getCycleFlag() { return cycleFlag; }
    public void setCycleFlag(String cycleFlag) { this.cycleFlag = cycleFlag; }

    public String getCycleMethod() { return cycleMethod; }
    public void setCycleMethod(String cycleMethod) { this.cycleMethod = cycleMethod; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("partId", getPartId())
            .append("factoryId", getFactoryId())
            .append("ruleId", getRuleId())
            .append("partIndex", getPartIndex())
            .append("partType", getPartType())
            .append("partCode", getPartCode())
            .append("partName", getPartName())
            .append("partLength", getPartLength())
            .append("dateFormat", getDateFormat())
            .append("inputCharacter", getInputCharacter())
            .append("fixCharacter", getFixCharacter())
            .append("seriaStartNo", getSeriaStartNo())
            .append("seriaStep", getSeriaStep())
            .append("seriaNowNo", getSeriaNowNo())
            .append("cycleFlag", getCycleFlag())
            .append("cycleMethod", getCycleMethod())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
