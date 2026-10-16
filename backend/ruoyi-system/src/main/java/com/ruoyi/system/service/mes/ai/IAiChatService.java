package com.ruoyi.system.service.mes.ai;

import java.util.List;
import com.ruoyi.system.domain.mes.ai.AiChatMessage;
import com.ruoyi.system.domain.mes.ai.AiChatSession;

/**
 * AI 问答 Service
 *
 * @author qixiaoxia
 */
public interface IAiChatService {

    /** 当前用户的会话列表 */
    List<AiChatSession> listMySessions();

    /** 新建会话 */
    AiChatSession createSession();

    /** 删除自己的会话（逻辑删除，越权返回 false） */
    boolean deleteMySession(Long sessionId);

    /** 查看自己会话的历史消息 */
    List<AiChatMessage> listMessages(Long sessionId);

    /**
     * 提问（流式）。同步段做校验/限流/属主检查（失败抛 ServiceException），
     * 异步段调模型并把增量/完成/错误事件写入 sink。
     */
    void ask(Long sessionId, String question, AiAnswerSink sink);
}
