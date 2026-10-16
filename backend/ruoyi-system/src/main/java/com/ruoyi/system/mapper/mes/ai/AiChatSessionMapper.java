package com.ruoyi.system.mapper.mes.ai;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.mes.ai.AiChatSession;

/**
 * AI 问答会话 Mapper
 *
 * @author qixiaoxia
 */
public interface AiChatSessionMapper {

    /** 当前用户的会话列表（会话隔离：只查归属自己的未删除会话） */
    List<AiChatSession> selectSessionListByUserId(@Param("userId") Long userId);

    /** 按主键查询（属主校验在 Service 做） */
    AiChatSession selectSessionById(@Param("sessionId") Long sessionId);

    /** 新建会话 */
    int insertSession(AiChatSession session);

    /** 回写标题（首问后）并刷新最后活跃时间 */
    int updateTitleAndTime(AiChatSession session);

    /** 仅刷新最后活跃时间 */
    int touchSession(@Param("sessionId") Long sessionId);

    /** 逻辑删除（限定归属人，防删他人会话） */
    int deleteSessionByOwner(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
