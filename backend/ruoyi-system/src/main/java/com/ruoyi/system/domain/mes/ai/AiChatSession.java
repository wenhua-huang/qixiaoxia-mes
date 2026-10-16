package com.ruoyi.system.domain.mes.ai;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * AI 问答会话（qxx_sys_ai_chat_session）
 *
 * <p>user_id 为会话隔离键：所有查询/操作必须带当前登录用户 ID，
 * 用户只能看到自己的会话。
 *
 * @author qixiaoxia
 */
public class AiChatSession extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long sessionId;

    /** 工厂ID（拦截器自动注入，业务代码不赋值） */
    private Long factoryId;

    /** 会话归属用户ID */
    private Long userId;

    /** 会话标题（首问截取） */
    private String title;

    /** 删除标志 0存在 2删除 */
    private String delFlag;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String delFlag) { this.delFlag = delFlag; }
}
