package com.ruoyi.web.controller.mes.sal;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.BaseIntegrationTest;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.event.mes.SalesShipmentCompletedEvent;
import com.ruoyi.system.event.mes.SalesShipmentRevokedEvent;
import com.ruoyi.system.event.mes.WorkorderStartedEvent;
import com.ruoyi.system.service.mes.pro.IProWorkorderService;
import com.ruoyi.system.service.mes.sal.impl.SalOrderLifecycleListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 销售订单四态收敛 —— 主链路集成测试（Task 6 验收）。
 *
 * <p>验收链：建单即 CONFIRMED（factory_id=1 由 FactoryIdInterceptor 注入）→ toWorkorder
 * → 开工事务（AFTER_COMMIT 事件同步投递）→ PRODUCING → 两道任务连续报工进度
 * 0→25→50→50→100 且订单状态恒 PRODUCING（D1：报工/产量绝不动订单状态）→ 部分发货 60
 * 事件复核不通过仍 PRODUCING → 第二张出库单 40 累计发齐 → SHIPPED → close → CLOSED。
 *
 * <p>门控：PRODUCING close/delete → HTTP body code 500；超转 → 500；取消后重复开工事件幂等。
 *
 * <p>两个测试线程陷阱：
 * <ol>
 *   <li>直调 {@code proWorkorderService.startProduction} 依赖线程级登录态，@BeforeAll
 *       塞入 admin/factoryId=1 的 SecurityContext（仿 MaterialTraceE2ETest）。</li>
 *   <li>AFTER_COMMIT 监听器无事务不投递，事件发布必须包在真实 TransactionTemplate 内，
 *       提交后监听器以 REQUIRES_NEW 同步执行条件 UPDATE。</li>
 * </ol>
 *
 * @author qixiaoxia
 */
@DisplayName("销售订单四态主链路集成测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SalOrderIT extends BaseIntegrationTest
{
    private static final Long FACTORY = 1L;

    /** 订单行数量（三张订单一致，便于超转门控复用） */
    private static final BigDecimal LINE_QTY = new BigDecimal("100");

    @Autowired
    private IProWorkorderService proWorkorderService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private SalOrderLifecycleListener listener;

    @Autowired
    private PlatformTransactionManager txManager;

    private String baseUrl() { return "http://localhost:" + port + "/mes/sal/order"; }

    @BeforeAll
    void setupAuth()
    {
        // startProduction 内部 SecurityUtils.getUsername() 依赖线程级登录态
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        user.setFactoryId(FACTORY);
        LoginUser loginUser = new LoginUser(user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));

        // 测试 schema（manual_tables.sql）未播种 PRO_CARD_CODE 编码规则，开工自动建卡会因规则缺失
        // 抛 RuntimeException（被 startProduction try-catch 吞掉但打印整段错误堆栈）。
        // 这里幂等补齐 V40 的规则+分段，让开工真正建卡（与 brief "开工自动建卡会写" 一致），输出无噪音。
        ensureCardCodeRule();
    }

    /** 防跨测试类 fork 线程复用泄漏 admin 身份（仿 ProFeedbackReceiptIT）。 */
    @AfterAll
    void clearAuth()
    {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void clean()
    {
        // qxx_wm_product_sales_box 是 V88 迁移建的表（Flyway baseline=136 不重放 V88，
        // manual_tables.sql 亦无），markShippedIfFullyDelivered SQL 依赖它，测试内幂等补建。
        ensureShipmentBoxTable();
        truncateTables("qxx_pro_card_process", "qxx_pro_card", "qxx_pro_task",
                "qxx_wm_product_sales_box", "qxx_wm_product_sales_line", "qxx_wm_product_sales",
                "qxx_pro_workorder", "qxx_sal_order_line", "qxx_sal_order");
    }

    // ==================== 测试 ====================

    @Test
    @DisplayName("四态主链路：建单CONFIRMED→开工PRODUCING→连续报工进度递增状态不跳→发齐SHIPPED→结单CLOSED")
    void four_status_main_flow()
    {
        Long orderId = createOrderAndWorkorder("SO-IT-M1", "WO-IT-M1", new BigDecimal("100"));
        Long lineId = getFirstLineId(orderId);
        Long woId = jdbcTemplate.queryForObject(
                "select workorder_id from qxx_pro_workorder where workorder_code='WO-IT-M1'", Long.class);

        // 建单即 CONFIRMED（不再有 submit/approve），factory_id=1 由拦截器注入
        assertThat(queryStatus(orderId)).isEqualTo("CONFIRMED");

        // 开工 + 事件投递包在同一真实事务里：提交后 AFTER_COMMIT 监听器同步执行条件 UPDATE
        startWorkorderAndPublish(woId);
        assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");

        // 两道工序任务（每道计划 qty=100）在报工开始前均已排产：分母 200 恒定。
        // 模拟连续审核报工后的任务产量累加（audit 不写订单状态由 Task4 反射防线单测固化）。
        seedTask(woId, "T1", new BigDecimal("100"));
        seedTask(woId, "T2", new BigDecimal("100"));
        assertProgressAndProducing(orderId, 0);          // 都还没报工：0/200
        jdbcTemplate.update("update qxx_pro_task set quantity_produced=50 where task_code='T1'");
        assertProgressAndProducing(orderId, 25);         // 50/200
        jdbcTemplate.update("update qxx_pro_task set quantity_produced=100 where task_code='T1'");
        assertProgressAndProducing(orderId, 50);         // 100/200
        assertProgressAndProducing(orderId, 50);         // T2 计划在、产量0：仍 100/200，状态依旧不跳
        jdbcTemplate.update("update qxx_pro_task set quantity_produced=100 where task_code='T2'");
        assertProgressAndProducing(orderId, 100);        // 全部报满 200/200：状态仍 PRODUCING，进度100

        // 部分发货 60/100：即使收到发运事件，订单维度 NOT EXISTS 复核不通过，仍 PRODUCING
        seedShippedBoxes(orderId, lineId, "WS-IT-1", new BigDecimal("60"));
        publishShipmentEventInTx(jdbcTemplate.queryForObject(
                "select sales_id from qxx_wm_product_sales where sales_code='WS-IT-1'", Long.class));
        assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");

        // 第二张出库单发剩余 40：累计 100 发齐 → SHIPPED
        seedShippedBoxes(orderId, lineId, "WS-IT-2", new BigDecimal("40"));
        publishShipmentEventInTx(jdbcTemplate.queryForObject(
                "select sales_id from qxx_wm_product_sales where sales_code='WS-IT-2'", Long.class));
        assertThat(queryStatus(orderId)).isEqualTo("SHIPPED");

        ResponseEntity<Map> close = restTemplate.exchange(baseUrl() + "/close/" + orderId,
                HttpMethod.PUT, authRequest(), Map.class);
        assertThat(close.getBody().get("code")).isEqualTo(200);
        assertThat(queryStatus(orderId)).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("PRODUCING 途中取消→CANCEL；重复开工事件状态不变（条件UPDATE幂等）")
    void cancel_during_producing_then_event_noop()
    {
        Long orderId = createOrderAndWorkorder("SO-IT-M2", "WO-IT-M2", new BigDecimal("100"));
        Long lineId = getFirstLineId(orderId);
        Long woId = jdbcTemplate.queryForObject(
                "select workorder_id from qxx_pro_workorder where workorder_code='WO-IT-M2'", Long.class);
        startWorkorderAndPublish(woId);
        assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");

        ResponseEntity<Map> cancel = restTemplate.exchange(baseUrl() + "/cancel/" + orderId,
                HttpMethod.PUT, authRequest(), Map.class);
        assertThat(cancel.getBody().get("code")).isEqualTo(200);
        assertThat(queryStatus(orderId)).isEqualTo("CANCEL");

        // 直接调监听方法模拟重复事件（REQUIRES_NEW 自行开事务）：WHERE status='CONFIRMED' 不命中
        listener.onWorkorderStarted(new WorkorderStartedEvent(woId, lineId, FACTORY));
        assertThat(queryStatus(orderId)).isEqualTo("CANCEL");
    }

    @Test
    @DisplayName("门控：PRODUCING 不可结单/不可删/不可超转（HTTP body code 500）")
    void gates_rejected()
    {
        Long orderId = createOrderAndWorkorder("SO-IT-M3", "WO-IT-M3", new BigDecimal("100"));
        Long lineId = getFirstLineId(orderId);
        Long woId = jdbcTemplate.queryForObject(
                "select workorder_id from qxx_pro_workorder where workorder_code='WO-IT-M3'", Long.class);
        startWorkorderAndPublish(woId);
        assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");

        // 超转门控（旧用例 toWorkorder_overConvertible_rejected 语义保留）：行 100 已全转，再转 50 → 500
        Map<String, Object> overReq = new HashMap<>();
        overReq.put("lineId", lineId);
        overReq.put("quantity", new BigDecimal("50"));
        overReq.put("workorderCode", "WO-IT-M3-OV");
        overReq.put("requestDate", "2026-07-30 00:00:00");
        ResponseEntity<Map> over = restTemplate.postForEntity(
                baseUrl() + "/toWorkorder", authRequest(overReq), Map.class);
        assertThat(over.getBody().get("code")).isEqualTo(500);
        assertThat(over.getBody().get("msg").toString()).contains("可转数量");

        ResponseEntity<Map> close = restTemplate.exchange(baseUrl() + "/close/" + orderId,
                HttpMethod.PUT, authRequest(), Map.class);
        assertThat(close.getBody().get("code")).isEqualTo(500);

        ResponseEntity<Map> del = restTemplate.exchange(baseUrl() + "/" + orderId,
                HttpMethod.DELETE, authRequest(), Map.class);
        assertThat(del.getBody().get("code")).isEqualTo(500);
    }

    @Test
    @DisplayName("冲销对称降级：SHIPPED 删发运单（箱回 PACKED）→PRODUCING；仍发齐时冲销事件不动 SHIPPED；重发后回到 SHIPPED")
    void shipment_revoke_demotion_roundtrip()
    {
        Long orderId = createOrderAndWorkorder("SO-IT-M4", "WO-IT-M4", new BigDecimal("100"));
        Long lineId = getFirstLineId(orderId);
        Long woId = jdbcTemplate.queryForObject(
                "select workorder_id from qxx_pro_workorder where workorder_code='WO-IT-M4'", Long.class);
        startWorkorderAndPublish(woId);
        assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");

        // 60+40 两张出库单累计发齐 → SHIPPED
        seedShippedBoxes(orderId, lineId, "WS-IT-3", new BigDecimal("60"));
        Long sales3 = jdbcTemplate.queryForObject(
                "select sales_id from qxx_wm_product_sales where sales_code='WS-IT-3'", Long.class);
        publishShipmentEventInTx(sales3);
        assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");
        seedShippedBoxes(orderId, lineId, "WS-IT-4", new BigDecimal("40"));
        Long sales4 = jdbcTemplate.queryForObject(
                "select sales_id from qxx_wm_product_sales where sales_code='WS-IT-4'", Long.class);
        publishShipmentEventInTx(sales4);
        assertThat(queryStatus(orderId)).isEqualTo("SHIPPED");

        // 删除已发齐的发运单前，箱仍 SHIPPED：冲销事件幂等不动 SHIPPED
        publishRevokeEventInTx(sales4);
        assertThat(queryStatus(orderId)).isEqualTo("SHIPPED");

        // 模拟 doDeleteShipment：WS-IT-4 的箱回滚为 PACKED → 已不发齐 → SHIPPED 降级 PRODUCING（有未取消工单）
        jdbcTemplate.update(
                "update qxx_wm_product_sales_box set status='PACKED' where sales_id=?", sales4);
        publishRevokeEventInTx(sales4);
        assertThat(queryStatus(orderId)).isEqualTo("PRODUCING");

        // 重发（箱重新 SHIPPED）→ 条件推进再次生效，回到 SHIPPED
        jdbcTemplate.update(
                "update qxx_wm_product_sales_box set status='SHIPPED' where sales_id=?", sales4);
        publishShipmentEventInTx(sales4);
        assertThat(queryStatus(orderId)).isEqualTo("SHIPPED");
    }

    @Test
    @DisplayName("冲销降级：未派生工单的订单 SHIPPED→CONFIRMED；改/删闸门对已派生工单返回 500")
    void shipment_revoke_demotes_to_confirmed_and_editdelete_gate()
    {
        // 仅建单不转工单，直接造出发齐事实
        Long orderId = createOrderOnly("SO-IT-M5");
        Long lineId = getFirstLineId(orderId);
        seedShippedBoxes(orderId, lineId, "WS-IT-5", new BigDecimal("100"));
        Long sales5 = jdbcTemplate.queryForObject(
                "select sales_id from qxx_wm_product_sales where sales_code='WS-IT-5'", Long.class);
        publishShipmentEventInTx(sales5);
        assertThat(queryStatus(orderId)).isEqualTo("SHIPPED");

        // 无未取消工单 → 降级 CONFIRMED
        jdbcTemplate.update(
                "update qxx_wm_product_sales_box set status='PACKED' where sales_id=?", sales5);
        publishRevokeEventInTx(sales5);
        assertThat(queryStatus(orderId)).isEqualTo("CONFIRMED");

        // CONFIRMED + 已派生未开工工单：改/删闸门（工单由主链路另单派生，这里给本单补一张 PREPARE 工单）
        Long orderId2 = createOrderAndWorkorder("SO-IT-M6", "WO-IT-M6", new BigDecimal("100"));
        assertThat(queryStatus(orderId2)).isEqualTo("CONFIRMED");
        ResponseEntity<Map> del = restTemplate.exchange(baseUrl() + "/" + orderId2,
                HttpMethod.DELETE, authRequest(), Map.class);
        assertThat(del.getBody().get("code")).isEqualTo(500);
        assertThat(del.getBody().get("msg").toString()).contains("已派生工单");
    }

    // ==================== 辅助：状态 / 进度 / 事务事件 ====================

    /** 冲销事件同样要在事务内发布，AFTER_COMMIT 才会投递到 REQUIRES_NEW 监听器 */
    private void publishRevokeEventInTx(Long salesId)
    {
        Long orderId = jdbcTemplate.queryForObject(
                "select sales_order_id from qxx_wm_product_sales where sales_id=?", Long.class, salesId);
        new TransactionTemplate(txManager).executeWithoutResult(status ->
                publisher.publishEvent(new SalesShipmentRevokedEvent(salesId, orderId, FACTORY)));
    }

    private String queryStatus(Long orderId)
    {
        return jdbcTemplate.queryForObject(
                "select status from qxx_sal_order where order_id=?", String.class, orderId);
    }

    /**
     * 真实事务内开工；startProduction 内部已自行发布 WorkorderStartedEvent（Task 4），
     * 这里只提供事务边界：提交后 AFTER_COMMIT 监听器同步推进 CONFIRMED→PRODUCING。
     * 不再手工 publishEvent 开工事件（重复投递虽幂等无害，但日志有噪音）。
     */
    private void startWorkorderAndPublish(Long woId)
    {
        new TransactionTemplate(txManager).executeWithoutResult(
                status -> proWorkorderService.startProduction(woId));
    }

    /** 发运事件同样要在事务内发布，AFTER_COMMIT 才会投递 */
    private void publishShipmentEventInTx(Long salesId)
    {
        Long orderId = jdbcTemplate.queryForObject(
                "select sales_order_id from qxx_wm_product_sales where sales_id=?", Long.class, salesId);
        new TransactionTemplate(txManager).executeWithoutResult(status ->
                publisher.publishEvent(new SalesShipmentCompletedEvent(salesId, orderId, FACTORY)));
    }

    /** includeProgress=true 查列表，断言该订单状态恒 PRODUCING 且进度符合预期 */
    @SuppressWarnings("unchecked")
    private void assertProgressAndProducing(Long orderId, int expectedPercent)
    {
        ResponseEntity<Map> resp = restTemplate.exchange(
                baseUrl() + "/list?pageNum=1&pageSize=100&includeProgress=true",
                HttpMethod.GET, authRequest(), Map.class);
        List<Map<String, Object>> rows = (List<Map<String, Object>>) resp.getBody().get("rows");
        Map<String, Object> mine = rows.stream()
                .filter(x -> orderId.equals(((Number) x.get("orderId")).longValue()))
                .findFirst().orElseThrow();
        assertThat(mine.get("status")).isEqualTo("PRODUCING");
        assertThat(((Number) mine.get("progressPercent")).intValue()).isEqualTo(expectedPercent);
    }

    // ==================== 辅助：建单 + 转工单 ====================

    /**
     * 建单（不传 status → 建单即 CONFIRMED）并整单转工单。
     * 保留旧用例 create_confirm_toWorkorder 的全部断言语义：来源列回填、已转量/可转量回算，
     * 仅去掉已废弃的 submit/approve 两步。
     */
    private Long createOrderAndWorkorder(String orderCode, String workorderCode, BigDecimal convertQty)
    {
        Map<String, Object> line = new HashMap<>();
        line.put("productId", 1);
        line.put("productCode", "P001");
        line.put("productName", "产品");
        line.put("unitOfMeasure", "PCS");
        line.put("unitName", "个");
        line.put("quantity", LINE_QTY);

        Map<String, Object> order = new HashMap<>();
        order.put("orderCode", orderCode);
        order.put("orderName", "订单-" + orderCode);
        order.put("clientCode", "C001");
        order.put("clientName", "测试客户");
        order.put("businessLine", "DOMESTIC");

        Map<String, Object> createReq = new HashMap<>();
        createReq.put("order", order);
        createReq.put("lines", List.of(line));

        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl() + "/createWithLines", authRequest(createReq), Map.class);
        assertThat(createResp.getBody().get("code")).isEqualTo(200);
        Long orderId = ((Number) ((Map<?, ?>) createResp.getBody().get("data")).get("orderId")).longValue();

        // 建单即 CONFIRMED，factory_id 由 FactoryIdInterceptor 注入为线程工厂 1
        Map<String, Object> created = jdbcTemplate.queryForMap(
                "select status, factory_id from qxx_sal_order where order_id=?", orderId);
        assertThat(created.get("status")).isEqualTo("CONFIRMED");
        assertThat(((Number) created.get("factory_id")).longValue()).isEqualTo(FACTORY.longValue());

        Long lineId = getFirstLineId(orderId);
        Map<String, Object> twReq = new HashMap<>();
        twReq.put("lineId", lineId);
        twReq.put("quantity", convertQty);
        twReq.put("workorderCode", workorderCode);
        twReq.put("requestDate", "2026-07-30 00:00:00");
        ResponseEntity<Map> twResp = restTemplate.postForEntity(
                baseUrl() + "/toWorkorder", authRequest(twReq), Map.class);
        assertThat(twResp.getBody().get("code")).isEqualTo(200);

        // 工单带来源列：order_source=SALES_ORDER / source_code / sales_order_line_id / 初态 PREPARE
        Map<String, Object> wo = jdbcTemplate.queryForMap(
                "select order_source, source_code, sales_order_line_id, quantity, status, factory_id "
                        + "from qxx_pro_workorder where workorder_code=?", workorderCode);
        assertThat(wo.get("order_source")).isEqualTo("SALES_ORDER");
        assertThat(wo.get("source_code")).isEqualTo(orderCode);
        assertThat(((Number) wo.get("sales_order_line_id")).longValue()).isEqualTo(lineId);
        assertThat(new BigDecimal(wo.get("quantity").toString())).isEqualByComparingTo(convertQty);
        assertThat(wo.get("status")).isEqualTo("PREPARE");
        assertThat(((Number) wo.get("factory_id")).longValue()).isEqualTo(FACTORY.longValue());

        // 已转量/可转量回算
        BigDecimal convertible = LINE_QTY.subtract(convertQty);
        Map<String, Object> lineAfter = getFirstLineDetail(orderId);
        assertThat(new BigDecimal(lineAfter.get("quantityProduced").toString())).isEqualByComparingTo(convertQty);
        assertThat(new BigDecimal(lineAfter.get("quantityConvertible").toString())).isEqualByComparingTo(convertible);

        return orderId;
    }

    /** 仅建单（建单即 CONFIRMED），不转工单——用于无工单降级 CONFIRMED 分支 */
    @SuppressWarnings("unchecked")
    private Long createOrderOnly(String orderCode)
    {
        Map<String, Object> line = new HashMap<>();
        line.put("productId", 1);
        line.put("productCode", "P001");
        line.put("productName", "产品");
        line.put("unitOfMeasure", "PCS");
        line.put("unitName", "个");
        line.put("quantity", LINE_QTY);

        Map<String, Object> order = new HashMap<>();
        order.put("orderCode", orderCode);
        order.put("orderName", "订单-" + orderCode);
        order.put("clientCode", "C001");
        order.put("clientName", "测试客户");
        order.put("businessLine", "DOMESTIC");

        Map<String, Object> createReq = new HashMap<>();
        createReq.put("order", order);
        createReq.put("lines", List.of(line));

        ResponseEntity<Map> createResp = restTemplate.postForEntity(
                baseUrl() + "/createWithLines", authRequest(createReq), Map.class);
        assertThat(createResp.getBody().get("code")).isEqualTo(200);
        return ((Number) ((Map<?, ?>) createResp.getBody().get("data")).get("orderId")).longValue();
    }

    @SuppressWarnings("unchecked")
    private Long getFirstLineId(Long orderId)
    {
        return ((Number) getFirstLineDetail(orderId).get("lineId")).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFirstLineDetail(Long orderId)
    {
        ResponseEntity<Map> detail = restTemplate.exchange(
                baseUrl() + "/detail/" + orderId, HttpMethod.GET, authRequest(), Map.class);
        List<?> lines = (List<?>) ((Map<String, Object>) detail.getBody().get("data")).get("lines");
        assertThat(lines).isNotEmpty();
        return (Map<String, Object>) lines.get(0);
    }

    // ==================== 辅助：直插任务 / 出库数据（绕拦截器，显式 factory_id=1） ====================

    /**
     * 直插任务。qxx_pro_task 这些列 NOT NULL：
     * task_name/workorder_code/workorder_name/workstation 三列/route_id/process_id/item 三列/unit_of_measure
     */
    private void seedTask(Long woId, String code, BigDecimal qty)
    {
        Map<String, Object> wo = jdbcTemplate.queryForMap(
                "select workorder_code, workorder_name, product_id, product_code, product_name, unit_of_measure "
                        + "from qxx_pro_workorder where workorder_id=?", woId);
        Object uom = wo.get("unit_of_measure");
        jdbcTemplate.update(
                "insert into qxx_pro_task (factory_id, task_code, task_name, workorder_id, workorder_code, workorder_name, "
                        + "workstation_id, workstation_code, workstation_name, route_id, process_id, process_code, process_name, "
                        + "item_id, item_code, item_name, unit_of_measure, quantity, quantity_produced, status, create_by, create_time) "
                        + "values (1, ?, ?, ?, ?, ?, 1, 'WS-IT', 'IT工位', 1, 1, 'P1', '工序', ?, ?, ?, ?, ?, 0, 'PRODUCING', 'admin', NOW())",
                code, "任务-" + code, woId, wo.get("workorder_code"), wo.get("workorder_name"),
                wo.get("product_id"), wo.get("product_code"), wo.get("product_name"),
                uom == null ? "PCS" : uom, qty);
    }

    /**
     * 建一张非作废出库单（头 + 行 + SHIPPED 箱）。
     * 头 client_id/warehouse_id、行 warehouse_id/item 三列/unit 两列均 NOT NULL（V82/V83 真实 DDL）。
     */
    private void seedShippedBoxes(Long orderId, Long lineId, String salesCode, BigDecimal boxQty)
    {
        jdbcTemplate.update(
                "insert into qxx_wm_product_sales (factory_id, sales_code, sales_name, client_id, "
                        + "warehouse_id, sales_order_id, total_quantity, shipped_quantity, ship_status, status, create_by, create_time) "
                        + "values (1, ?, 'IT出库', 1, 1, ?, ?, ?, 'SHIPPED', 'SHIPPED', 'admin', NOW())",
                salesCode, orderId, boxQty, boxQty);
        Long salesId = jdbcTemplate.queryForObject(
                "select sales_id from qxx_wm_product_sales where sales_code=?", Long.class, salesCode);
        // warehouse_id 在表上 NOT NULL 无默认值（已核实 DDL），必须显式给
        jdbcTemplate.update(
                "insert into qxx_wm_product_sales_line (factory_id, sales_id, sales_order_line_id, warehouse_id, "
                        + "item_id, item_code, item_name, unit_of_measure, unit_name, quantity_sales, create_by, create_time) "
                        + "values (1, ?, ?, 1, 1, 'P001', '产品', 'PCS', '个', ?, 'admin', NOW())",
                salesId, lineId, boxQty);
        Long slId = jdbcTemplate.queryForObject(
                "select line_id from qxx_wm_product_sales_line where sales_id=?", Long.class, salesId);
        jdbcTemplate.update(
                "insert into qxx_wm_product_sales_box (factory_id, sales_id, line_id, box_no, quantity, "
                        + "status, create_by, create_time) values (1, ?, ?, ?, ?, 'SHIPPED', 'admin', NOW())",
                salesId, slId, salesCode + "-B1", boxQty);
    }

    // ==================== 辅助：测试 schema 补缺（幂等） ====================

    /** 与 V88 Part 2 逐列一致的装箱明细表（测试 Flyway baseline=136，V88 不重放）。 */
    private void ensureShipmentBoxTable()
    {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS qxx_wm_product_sales_box ("
                        + "box_id bigint NOT NULL AUTO_INCREMENT COMMENT '装箱ID',"
                        + "factory_id bigint NOT NULL COMMENT '工厂ID',"
                        + "sales_id bigint NOT NULL COMMENT '销售出库单ID',"
                        + "line_id bigint DEFAULT NULL COMMENT '出库行ID',"
                        + "box_no varchar(32) NOT NULL COMMENT '箱号',"
                        + "item_id bigint DEFAULT NULL COMMENT '物料ID',"
                        + "item_code varchar(64) DEFAULT '' COMMENT '物料编码快照',"
                        + "item_name varchar(200) DEFAULT '' COMMENT '物料名称快照',"
                        + "specification varchar(200) DEFAULT '' COMMENT '规格快照',"
                        + "quantity decimal(16,4) DEFAULT 0.0000 COMMENT '本箱数量',"
                        + "unit_of_measure varchar(64) DEFAULT '' COMMENT '计量单位编码',"
                        + "unit_name varchar(64) DEFAULT '' COMMENT '单位名称',"
                        + "box_spec varchar(100) DEFAULT '' COMMENT '箱规描述',"
                        + "box_length decimal(10,2) DEFAULT 0.00 COMMENT '箱长cm',"
                        + "box_width decimal(10,2) DEFAULT 0.00 COMMENT '箱宽cm',"
                        + "box_height decimal(10,2) DEFAULT 0.00 COMMENT '箱高cm',"
                        + "volume decimal(12,4) DEFAULT 0.0000 COMMENT '体积m3',"
                        + "weight decimal(12,4) DEFAULT 0.0000 COMMENT '重量kg',"
                        + "shipment_id bigint DEFAULT NULL COMMENT '关联发运单ID',"
                        + "status varchar(20) DEFAULT 'PACKED' COMMENT 'PACKED/SHIPPED',"
                        + "remark varchar(500) DEFAULT '' COMMENT '备注',"
                        + "create_by varchar(64) DEFAULT '' COMMENT '创建者',"
                        + "create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                        + "update_by varchar(64) DEFAULT '' COMMENT '更新者',"
                        + "update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                        + "PRIMARY KEY (box_id),"
                        + "KEY idx_factory_id (factory_id),"
                        + "KEY idx_sales_id (sales_id),"
                        + "KEY idx_shipment_id (shipment_id),"
                        + "KEY idx_line_id (line_id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='销售出库-装箱明细'");
    }

    /** 幂等补齐 PRO_CARD_CODE 规则及分段（与 V40 一致，factory_id=1）。 */
    private void ensureCardCodeRule()
    {
        jdbcTemplate.update(
                "INSERT INTO sys_auto_code_rule "
                        + "(factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) "
                        + "SELECT 1, 'PRO_CARD_CODE', '流转卡编码', '格式:CRD20260620001', 17, '1', '0', 'L', '1', 'admin', NOW() "
                        + "WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code='PRO_CARD_CODE' AND factory_id=1)");
        jdbcTemplate.update(
                "INSERT INTO sys_auto_code_part "
                        + "(factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) "
                        + "SELECT 1, r.rule_id, 1, 'FIXCHAR', 'PREFIX_CRD', '固定前缀CRD', 3, NULL, 'CRD', NULL, NULL, NULL, NULL, 'admin', NOW() "
                        + "FROM sys_auto_code_rule r WHERE r.rule_code='PRO_CARD_CODE' AND r.factory_id=1 "
                        + "AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id=r.rule_id AND p.part_index=1)");
        jdbcTemplate.update(
                "INSERT INTO sys_auto_code_part "
                        + "(factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) "
                        + "SELECT 1, r.rule_id, 2, 'NOWDATE', 'DATE_PART', '日期yyyyMMdd', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, 'admin', NOW() "
                        + "FROM sys_auto_code_rule r WHERE r.rule_code='PRO_CARD_CODE' AND r.factory_id=1 "
                        + "AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id=r.rule_id AND p.part_index=2)");
        jdbcTemplate.update(
                "INSERT INTO sys_auto_code_part "
                        + "(factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) "
                        + "SELECT 1, r.rule_id, 3, 'SERIALNO', 'SERIAL_PART', '流水号3位', 3, NULL, NULL, 1, 1, '1', 'DAY', 'admin', NOW() "
                        + "FROM sys_auto_code_rule r WHERE r.rule_code='PRO_CARD_CODE' AND r.factory_id=1 "
                        + "AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id=r.rule_id AND p.part_index=3)");
    }
}
