package com.ruoyi.system.mapper.mes.ai;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.common.annotation.SkipFactoryId;
import com.ruoyi.system.domain.mes.ai.AiChatMessage;

/**
 * AI 问答消息 Mapper
 *
 * @author qixiaoxia
 */
public interface AiChatMessageMapper {

    /** 会话全部消息（升序；属主校验在 Service 做） */
    List<AiChatMessage> selectMessagesBySessionId(@Param("sessionId") Long sessionId);

    /** 最近 N 条消息（倒序取，Service 反转后作为上下文） */
    List<AiChatMessage> selectRecentMessages(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    /** 追加消息。assistant 消息在异步线程落库（无请求上下文，拦截器无法注入），factory_id 由 Service 显式赋值 */
    @SkipFactoryId
    int insertMessage(AiChatMessage message);
}
