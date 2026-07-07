package com.ruoyi.web.controller.mes.pur;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * 采购订单全生命周期集成测试 — API 端到端（HTTP → Controller → Service → DB）
 *
 * 覆盖流程：创建PO → 审批 → 下单 → 关闭
 *
 * 前置条件：Docker + Redis 必须在运行
 * 运行命令：mvn verify -pl ruoyi-admin -Dit.test=PurOrderLifecycleIntegrationTest
 *
 * @author qixiaoxia
 * @date 2026-07-07
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PurOrderLifecycleIntegrationTest extends BaseIntegrationTest {

    private static Long testOrderId;
    private static final String BASE_URL = "http://localhost:";

    private String url(String path) {
        return BASE_URL + port + path;
    }

    @BeforeEach
    void cleanUp() {
        // 按外键安全顺序清理（子表在前）
        truncateTables("qxx_pur_order_line", "qxx_pur_order");
    }

    @Test
    @Order(1)
    @DisplayName("1. 创建采购订单(DRAFT) → 返回orderId")
    void testCreatePurOrder() {
        Map<String, Object> body = new HashMap<>();
        body.put("orderName", "集成测试采购单");
        body.put("vendorId", 1);
        body.put("vendorCode", "V001");
        body.put("vendorName", "测试供应商");
        body.put("purchaseType", "PAPER");
        body.put("status", "DRAFT");
        body.put("currency", "CNY");

        ResponseEntity<Map> resp = restTemplate.postForEntity(
            url("/mes/pur/order"), authRequest(body), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = resp.getBody();
        assertThat(result).isNotNull();
        assertThat(result.get("code")).isEqualTo(200);

        // 提取 orderId
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        assertThat(data).isNotNull();
        testOrderId = ((Number) data.get("orderId")).longValue();
        assertThat(testOrderId).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("2. 审批(DRAFT → APPROVED) → 状态变为APPROVED")
    void testApprovePurOrder() {
        assertThat(testOrderId).isNotNull();

        ResponseEntity<Map> resp = restTemplate.exchange(
            url("/mes/pur/order/" + testOrderId + "/approve"),
            HttpMethod.POST, authRequest(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = resp.getBody();
        assertThat(result.get("code")).isEqualTo(200);

        // 验证状态已更新
        ResponseEntity<Map> getResp = restTemplate.exchange(
            url("/mes/pur/order/" + testOrderId),
            HttpMethod.GET, authRequest(), Map.class);

        Map<String, Object> getResult = getResp.getBody();
        Map<String, Object> data = (Map<String, Object>) getResult.get("data");
        assertThat(data.get("status")).isEqualTo("APPROVED");
    }

    @Test
    @Order(3)
    @DisplayName("3. 下单(APPROVED → ORDERED) → 状态变为ORDERED，日期自动设置")
    void testOrderPurOrder() {
        assertThat(testOrderId).isNotNull();

        ResponseEntity<Map> resp = restTemplate.exchange(
            url("/mes/pur/order/" + testOrderId + "/order"),
            HttpMethod.POST, authRequest(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 验证
        ResponseEntity<Map> getResp = restTemplate.exchange(
            url("/mes/pur/order/" + testOrderId),
            HttpMethod.GET, authRequest(), Map.class);
        Map<String, Object> data = (Map<String, Object>) getResp.getBody().get("data");
        assertThat(data.get("status")).isEqualTo("ORDERED");
        assertThat(data.get("orderDate")).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("4. 非法状态流转拒绝 — 已ORDERED不能再审批")
    void testApproveRejectedOnOrdered() {
        assertThat(testOrderId).isNotNull();

        ResponseEntity<Map> resp = restTemplate.exchange(
            url("/mes/pur/order/" + testOrderId + "/approve"),
            HttpMethod.POST, authRequest(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = resp.getBody();
        // 应返回错误（不能重复审批）
        assertThat(result.get("code").toString()).isNotEqualTo("200");
    }

    @Test
    @Order(5)
    @DisplayName("5. 关闭未收货的PO被拒绝 — RECEIVED才能关闭")
    void testCloseRejectedWhenNotReceived() {
        assertThat(testOrderId).isNotNull();

        ResponseEntity<Map> resp = restTemplate.exchange(
            url("/mes/pur/order/" + testOrderId + "/close"),
            HttpMethod.POST, authRequest(), Map.class);

        Map<String, Object> result = resp.getBody();
        assertThat(result.get("code").toString()).isNotEqualTo("200");
        // 期望：提示"已收货"相关错误
        String msg = (String) result.get("msg");
        assertThat(msg).contains("已收货");
    }
}
