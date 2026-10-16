package com.ruoyi.system.domain.mes.ai;

/**
 * AI 问答提问请求体（POST /mes/ai/chat/ask）
 *
 * @author qixiaoxia
 */
public class AiAskRequest {

    /** 会话ID */
    private Long sessionId;

    /** 用户问题 */
    private String question;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
