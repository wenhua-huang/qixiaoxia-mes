package com.ruoyi.system.service.mes.ai;

/**
 * AI 流式回答的输出通道（由 Web 层用 SseEmitter 适配实现）。
 *
 * <p>Service 层不依赖 spring-web，事件类型约定：delta 增量 / done 完成 / error 错误。
 *
 * @author qixiaoxia
 */
public interface AiAnswerSink {

    /** 推送一个事件（type: delta/done/error） */
    void send(String type, String content);

    /** 结束输出（无论成功失败都必须调用） */
    void complete();
}
