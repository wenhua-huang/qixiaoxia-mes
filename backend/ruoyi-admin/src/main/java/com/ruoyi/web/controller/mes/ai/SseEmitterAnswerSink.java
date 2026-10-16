package com.ruoyi.web.controller.mes.ai;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.service.mes.ai.AiAnswerSink;

/**
 * AiAnswerSink 的 SSE 实现：事件统一 JSON 包装 {"type":"...","content":"..."} 写出。
 *
 * @author qixiaoxia
 */
public class SseEmitterAnswerSink implements AiAnswerSink {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterAnswerSink.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final SseEmitter emitter;

    public SseEmitterAnswerSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void send(String type, String content) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("type", type);
            payload.put("content", content);
            emitter.send(SseEmitter.event().data(JSON.writeValueAsString(payload)));
        } catch (IOException | IllegalStateException e) {
            log.warn("SSE 推送失败（客户端可能已断开）, type={}: {}", type, e.getMessage());
        }
    }

    @Override
    public void complete() {
        emitter.complete();
    }
}
