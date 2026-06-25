package com.ruoyi.web.controller.mes.pro;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 生产工单 Controller 集成测试
 * <p>
 * 技术栈：SpringBootTest（RANDOM_PORT）+ Testcontainers + RestTemplate。
 * 覆盖：创建工单全流程（含 BOM）、重复编码拒绝、工单列表查询等。
 *
 * @author qixiaoxia
 */
@DisplayName("生产工单 Controller 集成测试")
class ProWorkorderServiceIntegrationTest extends BaseIntegrationTest {

    private static boolean tablesReady = false;

    @BeforeEach
    void setUp() {
        if (!tablesReady) {
            tablesReady = true;
            // 工厂表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_md_factory (
                    factory_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_code varchar(64) NOT NULL,
                    factory_name varchar(255) NOT NULL,
                    short_name varchar(64) DEFAULT NULL,
                    address varchar(500) DEFAULT NULL,
                    contact varchar(64) DEFAULT NULL,
                    phone varchar(20) DEFAULT NULL,
                    enable_flag char(1) DEFAULT '1',
                    remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '',
                    create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (factory_id),
                    UNIQUE KEY uk_factory_code (factory_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            jdbcTemplate.execute("""
                INSERT IGNORE INTO qxx_md_factory (factory_id, factory_code, factory_name, enable_flag, create_by, create_time)
                VALUES (1, 'SX', '圣享工厂', '1', 'admin', NOW())
            """);

            // 物料/产品表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_md_item (
                    item_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL DEFAULT 1,
                    item_code varchar(64) NOT NULL,
                    item_name varchar(255) NOT NULL,
                    item_spc varchar(255) DEFAULT NULL,
                    category_id bigint(20) DEFAULT NULL,
                    unit_of_measure varchar(32) DEFAULT NULL,
                    unit_name varchar(64) DEFAULT NULL,
                    enable_flag char(1) DEFAULT '1',
                    remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '',
                    create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (item_id),
                    UNIQUE KEY uk_item_code (item_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            jdbcTemplate.execute("""
                INSERT IGNORE INTO qxx_md_item (item_id, factory_id, item_code, item_name, enable_flag, create_by, create_time)
                VALUES (1, 1, 'PROD-001', '测试产品', '1', 'admin', NOW())
            """);
            jdbcTemplate.execute("""
                INSERT IGNORE INTO qxx_md_item (item_id, factory_id, item_code, item_name, enable_flag, create_by, create_time)
                VALUES (100, 1, 'MAT-001', '测试物料', '1', 'admin', NOW())
            """);

            // 工单表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_pro_workorder (
                    workorder_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL DEFAULT 1,
                    workorder_code varchar(64) NOT NULL,
                    workorder_name varchar(255) DEFAULT NULL,
                    workorder_type varchar(32) DEFAULT 'SELF',
                    order_source varchar(32) DEFAULT 'MANUAL',
                    source_code varchar(64) DEFAULT NULL,
                    product_id bigint(20) DEFAULT NULL,
                    product_code varchar(64) DEFAULT NULL,
                    product_name varchar(255) DEFAULT NULL,
                    product_spc varchar(255) DEFAULT NULL,
                    route_product_id bigint(20) DEFAULT NULL,
                    unit_of_measure varchar(32) DEFAULT NULL,
                    unit_name varchar(64) DEFAULT NULL,
                    quantity decimal(18,6) DEFAULT NULL,
                    quantity_produced decimal(18,6) DEFAULT 0,
                    quantity_changed decimal(18,6) DEFAULT 0,
                    quantity_scheduled decimal(18,6) DEFAULT 0,
                    client_id bigint(20) DEFAULT NULL,
                    client_code varchar(64) DEFAULT NULL,
                    client_name varchar(255) DEFAULT NULL,
                    client_order_code varchar(64) DEFAULT NULL,
                    product_size varchar(255) DEFAULT NULL,
                    printing_req varchar(500) DEFAULT NULL,
                    rope_spec varchar(255) DEFAULT NULL,
                    package_req varchar(500) DEFAULT NULL,
                    shipping_req varchar(500) DEFAULT NULL,
                    order_type varchar(32) DEFAULT 'NEW',
                    vendor_id bigint(20) DEFAULT NULL,
                    vendor_code varchar(64) DEFAULT NULL,
                    vendor_name varchar(255) DEFAULT NULL,
                    outsource_factory_id bigint(20) DEFAULT NULL,
                    batch_code varchar(64) DEFAULT NULL,
                    request_date datetime DEFAULT NULL,
                    cancel_date datetime DEFAULT NULL,
                    finish_date datetime DEFAULT NULL,
                    status varchar(32) DEFAULT 'PREPARE',
                    remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '',
                    create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (workorder_id),
                    UNIQUE KEY uk_workorder_code (workorder_code),
                    KEY idx_factory_id (factory_id),
                    KEY idx_product_id (product_id),
                    KEY idx_status (status)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            // BOM表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_pro_workorder_bom (
                    line_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL DEFAULT 1,
                    workorder_id bigint(20) NOT NULL,
                    item_id bigint(20) NOT NULL,
                    item_code varchar(64) DEFAULT NULL,
                    item_name varchar(255) DEFAULT NULL,
                    item_spc varchar(255) DEFAULT NULL,
                    unit_of_measure varchar(32) DEFAULT NULL,
                    unit_name varchar(64) DEFAULT NULL,
                    unit2 varchar(32) DEFAULT NULL,
                    unit2_name varchar(64) DEFAULT NULL,
                    conversion_rate decimal(18,6) DEFAULT NULL,
                    process_id bigint(20) DEFAULT NULL,
                    process_name varchar(255) DEFAULT NULL,
                    item_or_product varchar(32) DEFAULT NULL,
                    quantity decimal(18,6) DEFAULT NULL,
                    total_quantity decimal(18,6) DEFAULT NULL,
                    total_quantity2 decimal(18,6) DEFAULT NULL,
                    remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '',
                    create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (line_id),
                    KEY idx_workorder_id (workorder_id),
                    KEY idx_item_id (item_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            // 工序参数表
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_pro_workorder_param (
                    record_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL DEFAULT 1,
                    workorder_id bigint(20) NOT NULL,
                    route_product_id bigint(20) DEFAULT NULL,
                    template_id bigint(20) DEFAULT NULL,
                    standard_value varchar(500) DEFAULT NULL,
                    adjusted_value varchar(500) DEFAULT NULL,
                    remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '',
                    create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (record_id),
                    KEY idx_workorder_id (workorder_id),
                    KEY idx_template_id (template_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            // 工单变更记录表（updateProWorkorder 在非PREPARE状态修改时会写入）
            // ⚠️ 必须包含 factory_id 列，否则 FactoryIdInterceptor 注入 INSERT 时 SQL 异常
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_pro_workorder_change (
                    change_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL DEFAULT 1,
                    workorder_id bigint(20) NOT NULL,
                    change_type varchar(32) DEFAULT NULL,
                    field_name varchar(64) DEFAULT NULL,
                    old_value varchar(500) DEFAULT NULL,
                    new_value varchar(500) DEFAULT NULL,
                    change_reason varchar(500) DEFAULT NULL,
                    status varchar(32) DEFAULT 'APPROVED',
                    approver varchar(64) DEFAULT NULL,
                    approve_time datetime DEFAULT NULL,
                    create_by varchar(64) DEFAULT '',
                    create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (change_id),
                    KEY idx_workorder_id (workorder_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        }

        // 每个测试前清理数据
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder_change");
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder_param");
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder_bom");
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder");
    }

    // ══════════════════════════════════════════════
    // testCreateWorkorderFullChain
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("创建工单全流程：POST 创建工单头 → 返回 workorderId")
    void testCreateWorkorderFullChain() {
        String code = "WO_FULL_" + System.nanoTime();

        // Step 1：创建工单头
        Map<String, Object> woBody = new HashMap<>();
        woBody.put("workorderCode", code);
        woBody.put("workorderName", "集成测试工单");
        woBody.put("productId", 1);
        woBody.put("productCode", "PROD-001");
        woBody.put("productName", "测试产品");
        woBody.put("quantity", 50);
        woBody.put("factoryId", 1);
        woBody.put("status", "PREPARE");

        String addUrl = "http://localhost:" + port + "/mes/pro/workorder";
        ResponseEntity<Map> addResp = restTemplate.postForEntity(addUrl, authRequest(woBody), Map.class);

        assertThat(addResp.getBody().get("code")).isEqualTo(200);

        // 工单ID由数据库自增回填，通过数据库查询获取
        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);
        assertThat(workorderId).isNotNull();
        assertThat(workorderId).isGreaterThan(0);

        // Step 2：创建含BOM的工单（通过数据库验证BOM行插入）
        String code2 = "WO_BOM_" + System.nanoTime();
        Map<String, Object> woBody2 = new HashMap<>();
        woBody2.put("workorderCode", code2);
        woBody2.put("workorderName", "含BOM工单");
        woBody2.put("productId", 1);
        woBody2.put("productCode", "PROD-001");
        woBody2.put("quantity", 100);
        woBody2.put("factoryId", 1);

        // 先普通创建工单头
        ResponseEntity<Map> addResp2 = restTemplate.postForEntity(addUrl, authRequest(woBody2), Map.class);
        assertThat(addResp2.getBody().get("code")).isEqualTo(200);

        Long workorderId2 = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code2);
        assertThat(workorderId2).isNotNull();

        // 验证工单状态默认值
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM qxx_pro_workorder WHERE workorder_id = ?", String.class, workorderId2);
        assertThat(status).isEqualTo("PREPARE");
    }

    // ══════════════════════════════════════════════
    // testCreateWorkorderRollback
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("创建工单失败：编码重复返回错误")
    void testCreateWorkorderRollback() {
        String code = "WO_DUP_" + System.nanoTime();

        // 第一次创建成功
        Map<String, Object> body = new HashMap<>();
        body.put("workorderCode", code);
        body.put("workorderName", "重复测试");
        body.put("productId", 1);
        body.put("productCode", "PROD-001");
        body.put("quantity", 10);
        body.put("factoryId", 1);

        String url = "http://localhost:" + port + "/mes/pro/workorder";
        ResponseEntity<Map> resp1 = restTemplate.postForEntity(url, authRequest(body), Map.class);
        assertThat(resp1.getBody().get("code")).isEqualTo(200);

        // 第二次创建相同编码 → 应返回错误
        ResponseEntity<Map> resp2 = restTemplate.postForEntity(url, authRequest(body), Map.class);
        assertThat(resp2.getBody().get("code")).isEqualTo(500);
        assertThat((String) resp2.getBody().get("msg")).contains("已存在");
    }

    // ══════════════════════════════════════════════
    // testConcurrentEdit
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("并发编辑：更新同一工单应返回受影响行数")
    void testConcurrentEdit() {
        // 先创建一个工单
        String code = "WO_CONC_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_workorder (workorder_code, workorder_name, product_id, factory_id, quantity, status, create_by, create_time) " +
                "VALUES (?, '并发测试', 1, 1, 100, 'PREPARE', 'admin', NOW())", code);

        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);

        // 第一次更新
        Map<String, Object> updateBody1 = new HashMap<>();
        updateBody1.put("workorderId", workorderId);
        updateBody1.put("workorderCode", code);
        updateBody1.put("quantity", 150);

        String editUrl = "http://localhost:" + port + "/mes/pro/workorder";
        ResponseEntity<Map> resp1 = restTemplate.exchange(editUrl, HttpMethod.PUT, authRequest(updateBody1), Map.class);
        assertThat(resp1.getBody().get("code")).isEqualTo(200);

        // 第二次更新 (模拟并发场景)
        Map<String, Object> updateBody2 = new HashMap<>();
        updateBody2.put("workorderId", workorderId);
        updateBody2.put("workorderCode", code);
        updateBody2.put("quantity", 200);

        ResponseEntity<Map> resp2 = restTemplate.exchange(editUrl, HttpMethod.PUT, authRequest(updateBody2), Map.class);
        assertThat(resp2.getBody().get("code")).isEqualTo(200);

        // 验证最终数据为第二次更新的值（后写覆盖）
        java.math.BigDecimal finalQty = jdbcTemplate.queryForObject(
                "SELECT quantity FROM qxx_pro_workorder WHERE workorder_id = ?",
                java.math.BigDecimal.class, workorderId);
        assertThat(finalQty).isEqualByComparingTo("200");
    }

    // ══════════════════════════════════════════════
    // testWorkorderListQuery
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("工单列表查询：GET /list 返回分页数据")
    void testWorkorderListQuery() {
        // 插入测试数据
        String code1 = "WO_LIST1_" + System.nanoTime();
        String code2 = "WO_LIST2_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_workorder (workorder_code, workorder_name, product_id, factory_id, quantity, status, create_by, create_time) " +
                "VALUES (?, '列表测试1', 1, 1, 50, 'PREPARE', 'admin', NOW())", code1);
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_workorder (workorder_code, workorder_name, product_id, factory_id, quantity, status, create_by, create_time) " +
                "VALUES (?, '列表测试2', 1, 1, 80, 'PRODUCING', 'admin', NOW())", code2);

        String listUrl = "http://localhost:" + port + "/mes/pro/workorder/list";

        // 无筛选条件查询
        ResponseEntity<Map> resp = restTemplate.exchange(listUrl, HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat((Integer) resp.getBody().get("total")).isGreaterThanOrEqualTo(2);

        // 按状态筛选
        ResponseEntity<Map> respFiltered = restTemplate.exchange(
                listUrl + "?status=PRODUCING", HttpMethod.GET, authRequest(), Map.class);
        assertThat(respFiltered.getBody().get("code")).isEqualTo(200);
        assertThat((Integer) respFiltered.getBody().get("total")).isGreaterThanOrEqualTo(1);
    }

    // ══════════════════════════════════════════════
    // 补充：查询工单详情
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("查询工单详情：GET /{workorderId}")
    void testGetWorkorderById() {
        String code = "WO_DETAIL_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_workorder (workorder_code, workorder_name, product_id, product_code, factory_id, quantity, status, create_by, create_time) " +
                "VALUES (?, '详情测试', 1, 'PROD-001', 1, 200, 'PREPARE', 'admin', NOW())", code);

        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/pro/workorder/" + workorderId,
                HttpMethod.GET, authRequest(), Map.class);

        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat(resp.getBody().get("data")).isNotNull();
    }

    // ══════════════════════════════════════════════
    // 补充：修改工单
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("修改工单：PUT 更新工单信息")
    void testUpdateWorkorder() {
        String code = "WO_EDIT_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_workorder (workorder_code, workorder_name, product_id, factory_id, quantity, status, create_by, create_time) " +
                "VALUES (?, '修改前', 1, 1, 100, 'PREPARE', 'admin', NOW())", code);

        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("workorderId", workorderId);
        updateBody.put("workorderCode", code);
        updateBody.put("workorderName", "修改后");
        updateBody.put("quantity", 250);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/pro/workorder",
                HttpMethod.PUT, authRequest(updateBody), Map.class);

        assertThat(resp.getBody().get("code")).isEqualTo(200);

        // 验证数据库
        String name = jdbcTemplate.queryForObject(
                "SELECT workorder_name FROM qxx_pro_workorder WHERE workorder_id = ?",
                String.class, workorderId);
        assertThat(name).isEqualTo("修改后");
    }

    // ══════════════════════════════════════════════
    // 补充：删除工单
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("删除工单：DELETE /{workorderIds}")
    void testDeleteWorkorder() {
        String code = "WO_DEL_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_workorder (workorder_code, workorder_name, product_id, factory_id, quantity, status, create_by, create_time) " +
                "VALUES (?, '待删除', 1, 1, 10, 'PREPARE', 'admin', NOW())", code);

        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);

        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/pro/workorder/" + workorderId,
                HttpMethod.DELETE, authRequest(), Map.class);

        assertThat(resp.getBody().get("code")).isEqualTo(200);

        // 验证已删除
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder WHERE workorder_id = ?",
                Integer.class, workorderId);
        assertThat(count).isEqualTo(0);
    }

    // ══════════════════════════════════════════════
    // 补充：createWithBom 创建含BOM的工单
    // ══════════════════════════════════════════════

    // ══════════════════════════════════════════════
    // updateWithBom：修改工单含BOM+参数全量替换
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("updateWithBom：修改工单头 + 全量替换BOM + 参数upsert")
    void testUpdateWithBomFullReplace() {
        // 1. 先创建一个含BOM+参数的工单
        String code = "WO_UWB_" + System.nanoTime();
        Map<String, Object> bomItem = new HashMap<>();
        bomItem.put("itemId", 100);
        bomItem.put("itemCode", "MAT-001");
        bomItem.put("itemName", "测试物料");
        bomItem.put("unitName", "kg");
        bomItem.put("quantity", new java.math.BigDecimal("0.5"));

        Map<String, Object> paramItem = new HashMap<>();
        paramItem.put("templateId", 1L);
        paramItem.put("standardValue", "100");
        paramItem.put("adjustedValue", "105");

        Map<String, Object> wo = new HashMap<>();
        wo.put("workorderCode", code);
        wo.put("workorderName", "updateWithBom测试");
        wo.put("productId", 1);
        wo.put("productCode", "PROD-001");
        wo.put("quantity", 100);
        wo.put("factoryId", 1);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("workorder", wo);
        requestBody.put("bomList", List.of(bomItem));
        requestBody.put("paramList", List.of(paramItem));

        String createUrl = "http://localhost:" + port + "/mes/pro/workorder/createWithBom";
        ResponseEntity<Map> createResp = restTemplate.postForEntity(createUrl, authRequest(requestBody), Map.class);
        assertThat(createResp.getBody().get("code")).isEqualTo(200);

        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);
        assertThat(workorderId).isNotNull();

        // 验证初始BOM：totalQuantity = 0.5 × 100 = 50
        java.math.BigDecimal initQty = jdbcTemplate.queryForObject(
                "SELECT total_quantity FROM qxx_pro_workorder_bom WHERE workorder_id = ?",
                java.math.BigDecimal.class, workorderId);
        assertThat(initQty).isEqualByComparingTo("50.00");

        // 验证初始参数
        Integer initParamCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder_param WHERE workorder_id = ?",
                Integer.class, workorderId);
        assertThat(initParamCount).isEqualTo(1);

        // 2. 调用 updateWithBom：修改工单名 + 数量 + 替换BOM + 替换参数
        Map<String, Object> newBom = new HashMap<>();
        newBom.put("itemId", 100);
        newBom.put("itemCode", "MAT-001");
        newBom.put("itemName", "测试物料");
        newBom.put("unitName", "kg");
        newBom.put("quantity", new java.math.BigDecimal("0.3")); // changed: 0.5 → 0.3

        Map<String, Object> newParam = new HashMap<>();
        newParam.put("templateId", 2L); // new template
        newParam.put("standardValue", "200");
        newParam.put("adjustedValue", "210");

        Map<String, Object> updatedWo = new HashMap<>();
        updatedWo.put("workorderId", workorderId);
        updatedWo.put("workorderCode", code);
        updatedWo.put("workorderName", "updateWithBom已修改");
        updatedWo.put("productId", 1);
        updatedWo.put("quantity", 200); // changed: 100 → 200
        updatedWo.put("factoryId", 1);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("workorder", updatedWo);
        updateBody.put("bomList", List.of(newBom));
        updateBody.put("paramList", List.of(newParam));

        String updateUrl = "http://localhost:" + port + "/mes/pro/workorder/updateWithBom";
        ResponseEntity<Map> updateResp = restTemplate.exchange(updateUrl, HttpMethod.PUT, authRequest(updateBody), Map.class);
        assertThat(updateResp.getBody().get("code")).isEqualTo(200);

        // 3. 验证：工单名已修改
        String updatedName = jdbcTemplate.queryForObject(
                "SELECT workorder_name FROM qxx_pro_workorder WHERE workorder_id = ?",
                String.class, workorderId);
        assertThat(updatedName).isEqualTo("updateWithBom已修改");

        // 4. 验证：BOM totalQuantity 重新计算 = 0.3 × 200 = 60
        Integer bomCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder_bom WHERE workorder_id = ?",
                Integer.class, workorderId);
        assertThat(bomCount).isEqualTo(1); // 旧BOM已删除，只有1条新的

        java.math.BigDecimal newTotalQty = jdbcTemplate.queryForObject(
                "SELECT total_quantity FROM qxx_pro_workorder_bom WHERE workorder_id = ?",
                java.math.BigDecimal.class, workorderId);
        assertThat(newTotalQty).isEqualByComparingTo("60.00");

        // 5. 验证：参数已upsert（旧templateId=1被替换为templateId=2）
        Integer paramCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder_param WHERE workorder_id = ?",
                Integer.class, workorderId);
        assertThat(paramCount).isEqualTo(1);

        String adjustedValue = jdbcTemplate.queryForObject(
                "SELECT adjusted_value FROM qxx_pro_workorder_param WHERE workorder_id = ?",
                String.class, workorderId);
        assertThat(adjustedValue).isEqualTo("210");
    }

    @Test
    @DisplayName("updateWithBom：参数upsert — 保留已有参数、删除移除的参数、新增参数")
    void testUpdateWithBomParamUpsert() {
        // 1. 创建工单含2个参数
        String code = "WO_UPSERT_" + System.nanoTime();
        Map<String, Object> param1 = new HashMap<>();
        param1.put("templateId", 1L);
        param1.put("standardValue", "100");
        param1.put("adjustedValue", "110");

        Map<String, Object> param2 = new HashMap<>();
        param2.put("templateId", 2L);
        param2.put("standardValue", "200");
        param2.put("adjustedValue", "220");

        Map<String, Object> wo = new HashMap<>();
        wo.put("workorderCode", code);
        wo.put("workorderName", "参数upsert测试");
        wo.put("productId", 1);
        wo.put("productCode", "PROD-001");
        wo.put("quantity", 50);
        wo.put("factoryId", 1);

        Map<String, Object> createBody = new HashMap<>();
        createBody.put("workorder", wo);
        createBody.put("bomList", List.of());
        createBody.put("paramList", List.of(param1, param2));

        String createUrl = "http://localhost:" + port + "/mes/pro/workorder/createWithBom";
        ResponseEntity<Map> createResp = restTemplate.postForEntity(createUrl, authRequest(createBody), Map.class);
        assertThat(createResp.getBody().get("code")).isEqualTo(200);

        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);

        // 获取已有参数的recordId
        Long recordId1 = jdbcTemplate.queryForObject(
                "SELECT record_id FROM qxx_pro_workorder_param WHERE workorder_id = ? AND template_id = 1",
                Long.class, workorderId);
        assertThat(recordId1).isNotNull();

        // 2. updateWithBom：保留param1（按recordId适配）、删除param2、新增param3
        Map<String, Object> updatedParam1 = new HashMap<>();
        updatedParam1.put("recordId", recordId1); // ← 已有recordId → 触发update
        updatedParam1.put("templateId", 1L);
        updatedParam1.put("standardValue", "100");
        updatedParam1.put("adjustedValue", "150"); // 修改值

        Map<String, Object> newParam3 = new HashMap<>();
        newParam3.put("templateId", 3L); // 无recordId → 触发insert
        newParam3.put("standardValue", "300");
        newParam3.put("adjustedValue", "330");

        Map<String, Object> updatedWo = new HashMap<>();
        updatedWo.put("workorderId", workorderId);
        updatedWo.put("workorderCode", code);
        updatedWo.put("workorderName", "参数upsert测试-修改");
        updatedWo.put("productId", 1);
        updatedWo.put("quantity", 50);
        updatedWo.put("factoryId", 1);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("workorder", updatedWo);
        updateBody.put("bomList", List.of());
        updateBody.put("paramList", List.of(updatedParam1, newParam3));

        String updateUrl = "http://localhost:" + port + "/mes/pro/workorder/updateWithBom";
        ResponseEntity<Map> updateResp = restTemplate.exchange(updateUrl, HttpMethod.PUT, authRequest(updateBody), Map.class);
        assertThat(updateResp.getBody().get("code")).isEqualTo(200);

        // 3. 验证：只保留了templateId=1和templateId=3
        Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder_param WHERE workorder_id = ?",
                Integer.class, workorderId);
        assertThat(totalCount).isEqualTo(2);

        // templateId=1 的值已更新
        String adj1 = jdbcTemplate.queryForObject(
                "SELECT adjusted_value FROM qxx_pro_workorder_param WHERE workorder_id = ? AND template_id = 1",
                String.class, workorderId);
        assertThat(adj1).isEqualTo("150");

        // templateId=2 已被删除
        Integer deletedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder_param WHERE workorder_id = ? AND template_id = 2",
                Integer.class, workorderId);
        assertThat(deletedCount).isEqualTo(0);

        // templateId=3 是新插入的
        String adj3 = jdbcTemplate.queryForObject(
                "SELECT adjusted_value FROM qxx_pro_workorder_param WHERE workorder_id = ? AND template_id = 3",
                String.class, workorderId);
        assertThat(adj3).isEqualTo("330");
    }

    @Test
    @DisplayName("updateWithBom：更新不存在的工单 — 返回成功但data为null（updateProWorkorder返回0行不抛异常）")
    void testUpdateWithBomNonExistent() {
        Map<String, Object> wo = new HashMap<>();
        wo.put("workorderId", 99999L);
        wo.put("workorderCode", "NONEXIST");
        wo.put("workorderName", "不存在的工单");
        wo.put("quantity", 100);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("workorder", wo);
        updateBody.put("bomList", List.of());
        updateBody.put("paramList", List.of());

        String updateUrl = "http://localhost:" + port + "/mes/pro/workorder/updateWithBom";
        ResponseEntity<Map> resp = restTemplate.exchange(updateUrl, HttpMethod.PUT, authRequest(updateBody), Map.class);
        // updateProWorkorder 返回0行但不抛异常，与 PUT /mes/pro/workorder 行为一致
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat(resp.getBody().get("data")).isNull();
    }

    @Test
    @DisplayName("updateWithBom：空BOM列表清空所有BOM行")
    void testUpdateWithBomClearBom() {
        // 1. 创建含BOM的工单
        String code = "WO_CLEAR_" + System.nanoTime();
        Map<String, Object> bomItem = new HashMap<>();
        bomItem.put("itemId", 100);
        bomItem.put("itemCode", "MAT-001");
        bomItem.put("itemName", "测试物料");
        bomItem.put("unitName", "kg");
        bomItem.put("quantity", new java.math.BigDecimal("1.0"));

        Map<String, Object> wo = new HashMap<>();
        wo.put("workorderCode", code);
        wo.put("workorderName", "清空BOM测试");
        wo.put("productId", 1);
        wo.put("productCode", "PROD-001");
        wo.put("quantity", 10);
        wo.put("factoryId", 1);

        Map<String, Object> createBody = new HashMap<>();
        createBody.put("workorder", wo);
        createBody.put("bomList", List.of(bomItem));
        createBody.put("paramList", List.of());

        String createUrl = "http://localhost:" + port + "/mes/pro/workorder/createWithBom";
        restTemplate.postForEntity(createUrl, authRequest(createBody), Map.class);

        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);

        // 验证BOM存在
        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder_bom WHERE workorder_id = ?", Integer.class, workorderId);
        assertThat(before).isEqualTo(1);

        // 2. updateWithBom with empty bomList
        Map<String, Object> updatedWo = new HashMap<>();
        updatedWo.put("workorderId", workorderId);
        updatedWo.put("workorderCode", code);
        updatedWo.put("workorderName", "清空BOM后");
        updatedWo.put("quantity", 10);
        updatedWo.put("factoryId", 1);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("workorder", updatedWo);
        updateBody.put("bomList", List.of()); // 空列表
        updateBody.put("paramList", List.of());

        String updateUrl = "http://localhost:" + port + "/mes/pro/workorder/updateWithBom";
        ResponseEntity<Map> resp = restTemplate.exchange(updateUrl, HttpMethod.PUT, authRequest(updateBody), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        // 3. 验证BOM已被清空
        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder_bom WHERE workorder_id = ?", Integer.class, workorderId);
        assertThat(after).isEqualTo(0);
    }

    @Test
    @DisplayName("updateWithBom：PREPARE状态修改不产生变更记录，PRODUCING状态修改产生变更记录")
    void testUpdateWithBomChangeTracking() {
        // 1. 创建PREPARE工单
        String code = "WO_CT_" + System.nanoTime();
        Map<String, Object> wo = new HashMap<>();
        wo.put("workorderCode", code);
        wo.put("workorderName", "变更追踪测试");
        wo.put("productId", 1);
        wo.put("productCode", "PROD-001");
        wo.put("quantity", 100);
        wo.put("factoryId", 1);

        Map<String, Object> createBody = new HashMap<>();
        createBody.put("workorder", wo);
        createBody.put("bomList", List.of());
        createBody.put("paramList", List.of());

        String createUrl = "http://localhost:" + port + "/mes/pro/workorder/createWithBom";
        restTemplate.postForEntity(createUrl, authRequest(createBody), Map.class);

        Long workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, code);

        // 2. PREPARE状态下修改 → 不产生变更记录
        Map<String, Object> updateBody1 = new HashMap<>();
        Map<String, Object> woBody1 = new HashMap<>();
        woBody1.put("workorderId", workorderId);
        woBody1.put("workorderCode", code);
        woBody1.put("workorderName", "PREPARE修改");
        woBody1.put("quantity", 150);
        woBody1.put("factoryId", 1);
        updateBody1.put("workorder", woBody1);
        updateBody1.put("bomList", List.of());
        updateBody1.put("paramList", List.of());

        String updateUrl = "http://localhost:" + port + "/mes/pro/workorder/updateWithBom";
        restTemplate.exchange(updateUrl, HttpMethod.PUT, authRequest(updateBody1), Map.class);

        // 确认值为150
        java.math.BigDecimal qty1 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM qxx_pro_workorder WHERE workorder_id = ?",
                java.math.BigDecimal.class, workorderId);
        assertThat(qty1).isEqualByComparingTo("150");

        // 3. 手动置为PRODUCING状态，然后修改 → 应产生变更记录
        jdbcTemplate.update("UPDATE qxx_pro_workorder SET status = 'PRODUCING' WHERE workorder_id = ?", workorderId);

        Map<String, Object> updateBody2 = new HashMap<>();
        Map<String, Object> woBody2 = new HashMap<>();
        woBody2.put("workorderId", workorderId);
        woBody2.put("workorderCode", code);
        woBody2.put("workorderName", "PRODUCING修改");
        woBody2.put("quantity", 200); // 变更：150 → 200
        woBody2.put("factoryId", 1);
        updateBody2.put("workorder", woBody2);
        updateBody2.put("bomList", List.of());
        updateBody2.put("paramList", List.of());

        restTemplate.exchange(updateUrl, HttpMethod.PUT, authRequest(updateBody2), Map.class);

        java.math.BigDecimal qty2 = jdbcTemplate.queryForObject(
                "SELECT quantity FROM qxx_pro_workorder WHERE workorder_id = ?",
                java.math.BigDecimal.class, workorderId);
        assertThat(qty2).isEqualByComparingTo("200");
    }
}
