package com.ruoyi.system.service.mes.ai.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.AiChatRole;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.mes.ai.AiChatMessage;
import com.ruoyi.system.domain.mes.ai.AiChatSession;
import com.ruoyi.system.mapper.mes.ai.AiChatMessageMapper;
import com.ruoyi.system.mapper.mes.ai.AiChatSessionMapper;
import com.ruoyi.system.service.mes.ai.AiAnswerSink;
import com.ruoyi.system.service.mes.ai.IAiChatService;

/**
 * AI 问答 Service 实现：会话/消息落库 + 知识检索 + DeepSeek 流式回答。
 *
 * <p>会话隔离：所有读写都带 SecurityUtils.getUserId()，用户只能触达归属自己的会话。
 * 成本控制：每分钟 10 次、每天 100 次软上限（Redis 固定窗口计数）。
 *
 * @author qixiaoxia
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);
    /** 提问频率上限（次/分钟） */
    private static final int LIMIT_PER_MINUTE = 10;
    /** 提问频率上限（次/天） */
    private static final int LIMIT_PER_DAY = 100;
    /** 带入上下文的最近消息条数（约 5 轮问答） */
    private static final int CONTEXT_MESSAGES = 10;
    /** 问题长度上限 */
    private static final int QUESTION_MAX_LEN = 500;
    /** 会话标题取首问前 N 字 */
    private static final int TITLE_MAX_LEN = 20;

    @Autowired
    private AiChatSessionMapper sessionMapper;
    @Autowired
    private AiChatMessageMapper messageMapper;
    @Autowired
    private AiKnowledgeService knowledgeService;
    @Autowired
    private DeepSeekClient deepSeekClient;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public List<AiChatSession> listMySessions() {
        return sessionMapper.selectSessionListByUserId(SecurityUtils.getUserId());
    }

    @Override
    public AiChatSession createSession() {
        AiChatSession session = new AiChatSession();
        session.setUserId(SecurityUtils.getUserId());
        session.setTitle("新会话");
        session.setCreateBy(SecurityUtils.getUsername());
        Date now = new Date();
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insertSession(session);
        return session;
    }

    @Override
    public boolean deleteMySession(Long sessionId) {
        return sessionMapper.deleteSessionByOwner(sessionId, SecurityUtils.getUserId()) > 0;
    }

    @Override
    public List<AiChatMessage> listMessages(Long sessionId) {
        checkSessionOwner(sessionId);
        return messageMapper.selectMessagesBySessionId(sessionId);
    }

    @Override
    public void ask(Long sessionId, String question, AiAnswerSink sink) {
        Long userId = SecurityUtils.getUserId();
        Long factoryId = SecurityUtils.getFactoryId();
        validateQuestion(question);
        checkAskLimit(userId);
        AiChatSession session = checkSessionOwner(sessionId);
        saveMessage(sessionId, userId, factoryId, AiChatRole.USER, question.trim());
        List<Map<String, String>> messages = buildPrompt(sessionId, question.trim());
        // 异步线程无请求上下文，SecurityUtils 不可用，userId/factoryId 由请求线程传入
        taskExecutor.execute(() -> streamAnswer(session, userId, factoryId, messages, sink));
    }

    /** 校验问题非空且不超长 */
    private void validateQuestion(String question) {
        if (StringUtils.isBlank(question)) {
            throw new ServiceException("请输入问题");
        }
        if (question.trim().length() > QUESTION_MAX_LEN) {
            throw new ServiceException("问题太长啦，请控制在 " + QUESTION_MAX_LEN + " 字以内");
        }
    }

    /** 每分钟/每天固定窗口软限流（get+set 有极小竞态，对软上限可接受） */
    private void checkAskLimit(Long userId) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String minute = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String day = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String minuteKey = CacheConstants.AI_CHAT_KEY + "min:" + userId + ":" + minute;
        String dailyKey = CacheConstants.AI_CHAT_KEY + "daily:" + userId + ":" + day;
        if (incrWithExpire(minuteKey, 120) > LIMIT_PER_MINUTE) {
            throw new ServiceException("提问太频繁啦，请稍候再问");
        }
        if (incrWithExpire(dailyKey, 86400) > LIMIT_PER_DAY) {
            throw new ServiceException("今天提问次数已达上限（" + LIMIT_PER_DAY + " 次），明天再来吧");
        }
    }

    private int incrWithExpire(String key, int ttlSeconds) {
        Integer count = redisCache.getCacheObject(key);
        count = count == null ? 1 : count + 1;
        redisCache.setCacheObject(key, count, ttlSeconds, TimeUnit.SECONDS);
        return count;
    }

    /** 会话属主校验：不存在、已删除、非本人一律"会话不存在"（不暴露他人会话存在性） */
    private AiChatSession checkSessionOwner(Long sessionId) {
        if (sessionId == null) {
            throw new ServiceException("缺少会话ID");
        }
        AiChatSession session = sessionMapper.selectSessionById(sessionId);
        if (session == null || "2".equals(session.getDelFlag())
                || !SecurityUtils.getUserId().equals(session.getUserId())) {
            throw new ServiceException("会话不存在或已被删除");
        }
        return session;
    }

    private void saveMessage(Long sessionId, Long userId, Long factoryId, AiChatRole role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setFactoryId(factoryId);
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role.getCode());
        message.setContent(content);
        message.setCreateTime(new Date());
        messageMapper.insertMessage(message);
    }

    /** 组 OpenAI 消息数组：system(拒答护栏+知识) + 最近 N 条历史（含刚存的本问） */
    private List<Map<String, String>> buildPrompt(Long sessionId, String question) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(question)));
        List<AiChatMessage> recent = messageMapper.selectRecentMessages(sessionId, CONTEXT_MESSAGES);
        Collections.reverse(recent);
        for (AiChatMessage m : recent) {
            String role = AiChatRole.ASSISTANT.getCode().equals(m.getRole())
                    ? AiChatRole.ASSISTANT.toApiRole() : AiChatRole.USER.toApiRole();
            messages.add(Map.of("role", role, "content", m.getContent()));
        }
        return messages;
    }

    private String buildSystemPrompt(String question) {
        String knowledge = knowledgeService.retrieve(question);
        StringBuilder sb = new StringBuilder();
        sb.append("你是「企小侠MES」生产管理系统的内置帮助助手。严格遵守：\n");
        sb.append("1. 只回答企小侠MES系统的使用问题（功能说明、操作步骤、流程指引）。\n");
        sb.append("2. 与系统使用无关的问题（闲聊、天气、编程、翻译、写文案、其他软件等），一律只回复：")
          .append("「抱歉，我只能回答企小侠MES系统的使用问题哦。」不要输出其他内容。\n");
        sb.append("3. 回答严格依据下方系统文档；文档没有的内容不要编造，回答「文档中暂未说明，建议联系系统管理员」。\n");
        sb.append("4. 操作步骤用有序列表，简明扼要，面向工厂操作员。\n\n");
        sb.append("【系统文档】\n");
        sb.append(knowledge != null ? knowledge
                : "（本次问题未匹配到任何系统文档。若问题与系统使用无关，按第 2 条拒绝；若确与系统相关，按第 3 条回答文档中暂未说明。）");
        return sb.toString();
    }

    /** 异步段：流式调模型，增量推 sink，完成后落库 assistant 消息 */
    private void streamAnswer(AiChatSession session, Long userId, Long factoryId, List<Map<String, String>> messages, AiAnswerSink sink) {
        StringBuilder answer = new StringBuilder();
        try {
            deepSeekClient.streamChat(messages, delta -> {
                answer.append(delta);
                sink.send("delta", delta);
            });
            saveMessage(session.getSessionId(), userId, factoryId, AiChatRole.ASSISTANT, answer.toString());
            updateSessionAfterAnswer(session);
            sink.send("done", "");
        } catch (ServiceException e) {
            sink.send("error", e.getMessage());
        } catch (Exception e) {
            log.error("AI 回答流式输出异常, sessionId={}", session.getSessionId(), e);
            sink.send("error", "回答中断了，请重新提问");
        } finally {
            sink.complete();
        }
    }

    /** 首问后回写会话标题（取本会话首条提问）；每次问答刷新活跃时间 */
    private void updateSessionAfterAnswer(AiChatSession session) {
        if (StringUtils.isBlank(session.getTitle()) || "新会话".equals(session.getTitle())) {
            List<AiChatMessage> recent = messageMapper.selectRecentMessages(session.getSessionId(), CONTEXT_MESSAGES + 2);
            Collections.reverse(recent);
            for (AiChatMessage m : recent) {
                if (AiChatRole.USER.getCode().equals(m.getRole())) {
                    AiChatSession update = new AiChatSession();
                    update.setSessionId(session.getSessionId());
                    update.setTitle(StringUtils.substring(m.getContent(), 0, TITLE_MAX_LEN));
                    sessionMapper.updateTitleAndTime(update);
                    return;
                }
            }
        }
        sessionMapper.touchSession(session.getSessionId());
    }
}
