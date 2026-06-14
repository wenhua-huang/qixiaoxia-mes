package com.ruoyi.system.domain.mes.cal;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 班组成员对象 qxx_cal_team_member
 * 
 * @author ruoyi
 * @date 2026-06-14
 */
public class CalTeamMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 成员关联ID */
    private Long memberId;

    /** 工厂ID(关联qxx_md_factory) */
    @Excel(name = "工厂ID(关联qxx_md_factory)")
    private Long factoryId;

    /** 班组ID(关联qxx_cal_team) */
    @Excel(name = "班组ID(关联qxx_cal_team)")
    private Long teamId;

    /** 班组编码 */
    @Excel(name = "班组编码")
    private String teamCode;

    /** 班组名称 */
    @Excel(name = "班组名称")
    private String teamName;

    /** 用户ID(关联sys_user) */
    @Excel(name = "用户ID(关联sys_user)")
    private Long userId;

    /** 用户姓名 */
    @Excel(name = "用户姓名")
    private String userName;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    public void setMemberId(Long memberId) 
    {
        this.memberId = memberId;
    }

    public Long getMemberId() 
    {
        return memberId;
    }

    public void setFactoryId(Long factoryId) 
    {
        this.factoryId = factoryId;
    }

    public Long getFactoryId() 
    {
        return factoryId;
    }

    public void setTeamId(Long teamId) 
    {
        this.teamId = teamId;
    }

    public Long getTeamId() 
    {
        return teamId;
    }

    public void setTeamCode(String teamCode) 
    {
        this.teamCode = teamCode;
    }

    public String getTeamCode() 
    {
        return teamCode;
    }

    public void setTeamName(String teamName) 
    {
        this.teamName = teamName;
    }

    public String getTeamName() 
    {
        return teamName;
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

    public void setPhone(String phone) 
    {
        this.phone = phone;
    }

    public String getPhone() 
    {
        return phone;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("memberId", getMemberId())
            .append("factoryId", getFactoryId())
            .append("teamId", getTeamId())
            .append("teamCode", getTeamCode())
            .append("teamName", getTeamName())
            .append("userId", getUserId())
            .append("userName", getUserName())
            .append("phone", getPhone())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
