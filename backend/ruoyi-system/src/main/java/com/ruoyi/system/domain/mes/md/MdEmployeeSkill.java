package com.ruoyi.system.domain.mes.md;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 员工技能对象 qxx_md_employee_skill
 * 
 * @author qixiaoxia
 * @date 2026-07-15
 */
public class MdEmployeeSkill extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 技能ID */
    private Long skillId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID")
    private Long factoryId;

    /** 用户ID(关联sys_user) */
    @Excel(name = "用户ID")
    private Long userId;

    /** 用户姓名 */
    @Excel(name = "用户姓名")
    private String userName;

    /** 技能名称 */
    @Excel(name = "技能名称")
    private String skillName;

    /** 技能等级:JUNIOR-初级,MIDDLE-中级,SENIOR-高级 */
    @Excel(name = "技能等级", readConverterExp = "JUNIOR=初级,MIDDLE=中级,SENIOR=高级")
    private String skillLevel;

    public void setSkillId(Long skillId) 
    {
        this.skillId = skillId;
    }

    public Long getSkillId() 
    {
        return skillId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setUserName(String userName) 
    {
        this.userName = userName;
    }

    public String getUserName() 
    {
        return userName;
    }

    public void setSkillName(String skillName) 
    {
        this.skillName = skillName;
    }

    public String getSkillName() 
    {
        return skillName;
    }

    public void setSkillLevel(String skillLevel) 
    {
        this.skillLevel = skillLevel;
    }

    public String getSkillLevel() 
    {
        return skillLevel;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("skillId", getSkillId())
            .append("factoryId", getFactoryId())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("skillName", getSkillName())
            .append("skillLevel", getSkillLevel())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
