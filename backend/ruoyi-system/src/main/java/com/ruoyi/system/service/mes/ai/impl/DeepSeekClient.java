package com.ruoyi.system.service.mes.ai.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysConfigService;

/**
 * DeepSeek（OpenAI 兼容协议）流式对话客户端。
 *
 * <p>接入参数全部来自 sys_config（mes.ai.baseUrl/apiKey/model），
 * 用 JDK 自带 HttpClient 读 SSE 行，不引入第三方 HTTP 依赖。
 *
 * @author qixiaoxia
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final String KEY_BASE_URL = "mes.ai.baseUrl";
    private static final String KEY_API_KEY = "mes.ai.apiKey";
    private static final String KEY_MODEL = "mes.ai.model";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    @Autowired
    private ISysConfigService configService;

    /**
     * 流式对话：逐段回调内容，返回时回答已完整。
     *
     * @param messages OpenAI 消息数组 [{role, content}, ...]
     * @param onDelta  每收到一段文本回调一次
     */
    public void streamChat(List<Map<String, String>> messages, Consumer<String> onDelta) {
        String apiKey = StringUtils.nvl(configService.selectConfigByKey(KEY_API_KEY), "");
        if (StringUtils.isBlank(apiKey)) {
            throw new ServiceException("AI 助手未配置 API 密钥，请联系管理员在「系统管理 → 参数设置」填写 mes.ai.apiKey");
        }
        HttpRequest request = buildRequest(messages, apiKey);
        try {
            HttpResponse<InputStream> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) {
                String errBody = new String(resp.body().readAllBytes(), StandardCharsets.UTF_8);
                log.error("DeepSeek 调用失败 HTTP {}: {}", resp.statusCode(), StringUtils.substring(errBody, 0, 500));
                throw new ServiceException("AI 服务暂时不可用（HTTP " + resp.statusCode() + "），请稍后重试");
            }
            readSseStream(resp.body(), onDelta);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek 流式调用异常", e);
            throw new ServiceException("AI 服务连接异常，请稍后重试");
        }
    }

    private HttpRequest buildRequest(List<Map<String, String>> messages, String apiKey) {
        String baseUrl = StringUtils.nvl(configService.selectConfigByKey(KEY_BASE_URL), "https://api.deepseek.com");
        String model = StringUtils.nvl(configService.selectConfigByKey(KEY_MODEL), "deepseek-chat");
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", true,
                "messages", messages,
                "temperature", 0.3,
                "max_tokens", 2048);
        try {
            return HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            throw new ServiceException("AI 请求构造失败：" + e.getMessage());
        }
    }

    /** 逐行解析 SSE：data: {...} → choices[0].delta.content；data: [DONE] 结束 */
    private void readSseStream(InputStream body, Consumer<String> onDelta) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data) || data.isEmpty()) {
                    break;
                }
                JsonNode delta = objectMapper.readTree(data).path("choices").path(0).path("delta").path("content");
                if (delta.isTextual()) {
                    onDelta.accept(delta.asText());
                }
            }
        }
    }
}
