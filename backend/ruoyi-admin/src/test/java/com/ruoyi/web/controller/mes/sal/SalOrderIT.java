package com.ruoyi.web.controller.mes.sal;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 销售订单集成测试 - 转工单全链路。
 *
 * 覆盖:建单(头+行) -> 确认 -> 详情(可转量) -> 转工单 -> DB 校验工单来源
 *      (order_source=SALES_ORDER / source_code / sales_order_line_id) + 已转量回算
 *
 * @author qixiaoxia
 */
@DisplayName("销售订单转工单集成测试")
class SalOrderIT extends BaseIntegrationTest
{
    private String baseUrl() { return "http://localhost:" + port + "/mes/sal/order"; }

    @BeforeEach
    void clean()
    {
        truncateTables("qxx_pro_workorder", "qxx_sal_order_line", "qxx_sal_order");
    }

    @Test
    @DisplayName("建单->确认->转工单:工单带销售订单来源,已转量回算")
    void create_confirm_toWorkorder() throws Exception
    {
        // 1. 建单(头+1行, qty=100)
        Map<String, Object> line = new HashMap<>();
        line.put("productId", 1);
        line.put("productCode", "P001");
        line.put("productName", "心心纸袋小号");
        line.put("unitOfMeasure", "PCS");
        line.put("unitName", "个");
        line.put("quantity", new BigDecimal("100"));
        line.put("productSize", "200*100*150mm");

        Map<String, Object> order = new HashMap<>();
        order.put("orderCode", "SO-IT-001");
        order.put("orderName", "心心纸袋-小号");
        order.put("clientCode", "C001");
        order.put("clientName", "测试客户");
        order.put("businessLine", "DOMESTIC");
        order.put("status", "PREPARE");

        Map<String, Object> createReq = new HashMap<>();
        createReq.put("order", order);
        createReq.put("lines", List.of(line));

        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl() + "/createWithLines", authRequest(createReq), Map.class);
        assertThat(createResp.getBody().get("code")).isEqualTo(200);
        Long orderId = ((Number) ((Map<?, ?>) createResp.getBody().get("data")).get("orderId")).longValue();

        // 2. 确认
        ResponseEntity<Map> confirmResp = restTemplate.exchange(
                baseUrl() + "/confirm/" + orderId, HttpMethod.PUT, authRequest(), Map.class);
        assertThat(confirmResp.getBody().get("code")).isEqualTo(200);

        // 3. 详情 -> 取 lineId + 校验可转量=100
        ResponseEntity<Map> detailResp = restTemplate.exchange(
                baseUrl() + "/detail/" + orderId, HttpMethod.GET, authRequest(), Map.class);
        Map<?, ?> detailData = (Map<?, ?>) detailResp.getBody().get("data");
        List<?> lines = (List<?>) detailData.get("lines");
        assertThat(lines).hasSize(1);
        Long lineId = ((Number) ((Map<?, ?>) lines.get(0)).get("lineId")).longValue();
        assertThat(new BigDecimal(((Map<?, ?>) lines.get(0)).get("quantityConvertible").toString()))
                .isEqualByComparingTo("100");

        // 4. 转工单(qty=60)
        Map<String, Object> twReq = new HashMap<>();
        twReq.put("lineId", lineId);
        twReq.put("quantity", new BigDecimal("60"));
        twReq.put("workorderCode", "WO-IT-001");
        twReq.put("requestDate", "2026-07-30 00:00:00");

        ResponseEntity<Map> twResp = restTemplate.postForEntity(
                baseUrl() + "/toWorkorder", authRequest(twReq), Map.class);
        assertThat(twResp.getBody().get("code")).isEqualTo(200);

        // 5. DB 校验:工单来源
        Map<String, Object> wo = jdbcTemplate.queryForMap(
                "select order_source, source_code, sales_order_line_id, quantity, status "
                + "from qxx_pro_workorder where workorder_code='WO-IT-001'");
        assertThat(wo.get("order_source")).isEqualTo("SALES_ORDER");
        assertThat(wo.get("source_code")).isEqualTo("SO-IT-001");
        assertThat(((Number) wo.get("sales_order_line_id")).longValue()).isEqualTo(lineId);
        assertThat(new BigDecimal(wo.get("quantity").toString())).isEqualByComparingTo("60");
        assertThat(wo.get("status")).isEqualTo("PREPARE");

        // 6. 再查详情 -> 已转量=60, 可转量=40
        ResponseEntity<Map> detail2 = restTemplate.exchange(
                baseUrl() + "/detail/" + orderId, HttpMethod.GET, authRequest(), Map.class);
        Map<?, ?> lineAfter = (Map<?, ?>) ((List<?>) ((Map<?, ?>) detail2.getBody().get("data")).get("lines")).get(0);
        assertThat(new BigDecimal(lineAfter.get("quantityProduced").toString())).isEqualByComparingTo("60");
        assertThat(new BigDecimal(lineAfter.get("quantityConvertible").toString())).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("超转拦截:转 70 后再转 50(可转 30) -> 500")
    void toWorkorder_overConvertible_rejected() throws Exception
    {
        Long orderId = createConfirmedOrder("SO-IT-002", "WO-IT-002", new BigDecimal("70"));
        Long lineId = getFirstLineId(orderId);

        // 再转 50(可转仅 30)
        Map<String, Object> twReq = new HashMap<>();
        twReq.put("lineId", lineId);
        twReq.put("quantity", new BigDecimal("50"));
        twReq.put("workorderCode", "WO-IT-003");
        twReq.put("requestDate", "2026-07-30 00:00:00");

        ResponseEntity<Map> twResp = restTemplate.postForEntity(
                baseUrl() + "/toWorkorder", authRequest(twReq), Map.class);
        assertThat(twResp.getBody().get("code")).isEqualTo(500);
        assertThat(twResp.getBody().get("msg").toString()).contains("可转数量");
    }

    // ============ 辅助 ============
    @SuppressWarnings("unchecked")
    private Long createConfirmedOrder(String orderCode, String workorderCode, BigDecimal firstConvertQty)
    {
        Map<String, Object> line = new HashMap<>();
        line.put("productId", 1);
        line.put("productCode", "P001");
        line.put("productName", "产品");
        line.put("unitOfMeasure", "PCS");
        line.put("quantity", new BigDecimal("100"));
        Map<String, Object> order = new HashMap<>();
        order.put("orderCode", orderCode);
        order.put("orderName", "订单");
        order.put("status", "PREPARE");
        Map<String, Object> createReq = new HashMap<>();
        createReq.put("order", order);
        createReq.put("lines", List.of(line));
        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl() + "/createWithLines", authRequest(createReq), Map.class);
        Long orderId = ((Number) ((Map<String, Object>) createResp.getBody().get("data")).get("orderId")).longValue();
        restTemplate.exchange(baseUrl() + "/confirm/" + orderId, HttpMethod.PUT, authRequest(), Map.class);

        Map<String, Object> twReq = new HashMap<>();
        twReq.put("lineId", getFirstLineId(orderId));
        twReq.put("quantity", firstConvertQty);
        twReq.put("workorderCode", workorderCode);
        twReq.put("requestDate", "2026-07-30 00:00:00");
        restTemplate.postForEntity(baseUrl() + "/toWorkorder", authRequest(twReq), Map.class);
        return orderId;
    }

    @SuppressWarnings("unchecked")
    private Long getFirstLineId(Long orderId)
    {
        ResponseEntity<Map> detail = restTemplate.exchange(
                baseUrl() + "/detail/" + orderId, HttpMethod.GET, authRequest(), Map.class);
        List<?> lines = (List<?>) ((Map<String, Object>) detail.getBody().get("data")).get("lines");
        return ((Number) ((Map<String, Object>) lines.get(0)).get("lineId")).longValue();
    }
}
