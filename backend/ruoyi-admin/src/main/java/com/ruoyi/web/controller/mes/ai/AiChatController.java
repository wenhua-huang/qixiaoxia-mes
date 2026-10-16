package com.ruoyi.web.controller.mes.ai;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.mes.ai.AiAskRequest;
import com.ruoyi.system.domain.mes.ai.AiChatMessage;
import com.ruoyi.system.domain.mes.ai.AiChatSession;
import com.ruoyi.system.service.mes.ai.IAiChatService;

/**
 * 帮助中心 AI 问答助手。
 *
 * <p>登录即可使用（与 /help 页面同级，不设业务权限点）；
 * 会话隔离与频率上限在 Service 层保证。
 *
 * @author qixiaoxia
 */
@RestController
@RequestMapping("/mes/ai/chat")
public class AiChatController extends BaseController {

    /** SSE 单连接最长 2 分钟（DeepSeek 回答一般 30s 内完成） */
    private static final long SSE_TIMEOUT_MS = 120_000L;

    @Autowired
    private IAiChatService aiChatService;

    /** 我的会话列表 */
    @GetMapping("/sessions")
    public AjaxResult sessions() {
        List<AiChatSession> list = aiChatService.listMySessions();
        return AjaxResult.success(list);
    }

    /** 新建会话 */
    @PostMapping("/sessions")
    public AjaxResult createSession() {
        return AjaxResult.success(aiChatService.createSession());
    }

    /** 删除我的会话 */
    @DeleteMapping("/sessions/{sessionId}")
    public AjaxResult deleteSession(@PathVariable("sessionId") Long sessionId) {
        boolean ok = aiChatService.deleteMySession(sessionId);
        return ok ? AjaxResult.success() : AjaxResult.error("会话不存在或已被删除");
    }

    /** 会话历史消息 */
    @GetMapping("/messages/{sessionId}")
    public AjaxResult messages(@PathVariable("sessionId") Long sessionId) {
        List<AiChatMessage> list = aiChatService.listMessages(sessionId);
        return AjaxResult.success(list);
    }

    /** 提问（SSE 流式回答） */
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@RequestBody AiAskRequest req) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        aiChatService.ask(req.getSessionId(), req.getQuestion(), new SseEmitterAnswerSink(emitter));
        return emitter;
    }
}
