package com.ruoyi.system.domain.mes.ai;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.enums.AiChatRole;

/**
 * AI 问答消息（qxx_sys_ai_chat_message）
 *
 * @author qixiaoxia
 */
public class AiChatMessage {

    /** 主键 */
    private Long msgId;

    /** 工厂ID（拦截器自动注入，业务代码不赋值） */
    private Long factoryId;

    /** 会话ID */
    private Long sessionId;

    /** 提问用户ID */
    private Long userId;

    /** 角色 USER/ASSISTANT，见 {@link AiChatRole} */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getMsgId() { return msgId; }
    public void setMsgId(Long msgId) { this.msgId = msgId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
