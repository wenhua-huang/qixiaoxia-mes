package com.ruoyi.system.domain.mes.sys;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统消息对象 sys_message
 *
 * @author qixiaoxia
 * @date 2025-06-11
 */
public class SysMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long messageId;

    /** 工厂ID */
    private Long factoryId;

    /** 接收用户ID */
    private Long userId;

    /** 消息标题 */
    @Excel(name = "消息标题")
    private String title;

    /** 消息内容 */
    private String content;

    /** 消息类型:SYSTEM-系统消息,APPROVAL-审批通知,WARNING-预警通知,NOTICE-公告 */
    @Excel(name = "消息类型", readConverterExp = "SYSTEM=系统消息,APPROVAL=审批通知,WARNING=预警通知,NOTICE=公告")
    private String messageType;

    /** 关联业务单据ID */
    private Long sourceDocId;

    /** 关联业务单据类型 */
    private String sourceDocType;

    /** 是否已读(1-已读,0-未读) */
    @Excel(name = "是否已读", readConverterExp = "1=已读,0=未读")
    private String readFlag;

    /** 阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public Long getSourceDocId() { return sourceDocId; }
    public void setSourceDocId(Long sourceDocId) { this.sourceDocId = sourceDocId; }

    public String getSourceDocType() { return sourceDocType; }
    public void setSourceDocType(String sourceDocType) { this.sourceDocType = sourceDocType; }

    public String getReadFlag() { return readFlag; }
    public void setReadFlag(String readFlag) { this.readFlag = readFlag; }

    public Date getReadTime() { return readTime; }
    public void setReadTime(Date readTime) { this.readTime = readTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("messageId", getMessageId())
            .append("factoryId", getFactoryId())
            .append("userId", getUserId())
            .append("title", getTitle())
            .append("content", getContent())
            .append("messageType", getMessageType())
            .append("sourceDocId", getSourceDocId())
            .append("sourceDocType", getSourceDocType())
            .append("readFlag", getReadFlag())
            .append("readTime", getReadTime())
            .append("remark", getRemark())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
