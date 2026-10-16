package com.ruoyi.common.enums;

/**
 * AI 问答消息角色（qxx_sys_ai_chat_message.role）
 *
 * @author qixiaoxia
 */
public enum AiChatRole {

    USER("USER", "用户"),
    ASSISTANT("ASSISTANT", "AI助手");

    private final String code;
    private final String info;

    AiChatRole(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() { return code; }
    public String getInfo() { return info; }

    /** 转 OpenAI 协议的小写角色名 */
    public String toApiRole() {
        return this.code.toLowerCase();
    }
}
