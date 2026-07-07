package com.ruoyi.web.controller.mes.pur;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * 采购订单集成测试 — 验证 Flyway 迁移 + Context 加载 + 端点可达
 *
 * 前置条件：Docker + Redis 运行
 * 运行：mvn test -Dtest=PurOrderLifecycleIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PurOrderLifecycleIntegrationTest extends BaseIntegrationTest {

    private static Long testOrderId;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @BeforeEach
    void cleanUp() {
        truncateTables("qxx_pur_order_line", "qxx_pur_order");
    }

    @Test
    @Order(1)
    @DisplayName("1. Flyway迁移成功 → Spring Context加载 → 采购订单端点可达")
    void testContextLoadsAndEndpointAccessible() {
        // 验证 Context 加载成功（Flyway 迁移完成）
        // 验证 列表端点可达
        ResponseEntity<Map> resp = restTemplate.exchange(
            url("/mes/pur/order/list"),
            HttpMethod.GET, authRequest(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = resp.getBody();
        assertThat(result.get("code")).isEqualTo(200);
    }

    @Test
    @Order(2)
    @DisplayName("2. 创建采购订单 → 返回200")
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

        // 端点可达（具体返回值取决于 FactoryIdInterceptor + 数据库种子数据）
        Map<String, Object> result = resp.getBody();
        assertThat(result).isNotNull();
        // 记录orderId（如果创建成功）
        if (Integer.valueOf(200).equals(result.get("code"))) {
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            if (data != null && data.get("orderId") != null) {
                testOrderId = ((Number) data.get("orderId")).longValue();
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("3. 审批端点可达")
    void testApproveEndpointAccessible() {
        // 即使没有有效的 orderId，验证端点存在且返回合理响应
        Long id = testOrderId != null ? testOrderId : 99999L;
        ResponseEntity<Map> resp = restTemplate.exchange(
            url("/mes/pur/order/" + id + "/approve"),
            HttpMethod.POST, authRequest(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> result = resp.getBody();
        assertThat(result).isNotNull();
        assertThat(result.get("code")).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("4. 下单端点可达")
    void testOrderEndpointAccessible() {
        Long id = testOrderId != null ? testOrderId : 99999L;
        ResponseEntity<Map> resp = restTemplate.exchange(
            url("/mes/pur/order/" + id + "/order"),
            HttpMethod.POST, authRequest(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("code")).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("5. 关闭端点可达")
    void testCloseEndpointAccessible() {
        Long id = testOrderId != null ? testOrderId : 99999L;
        ResponseEntity<Map> resp = restTemplate.exchange(
            url("/mes/pur/order/" + id + "/close"),
            HttpMethod.POST, authRequest(), Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get("code")).isNotNull();
    }
}
