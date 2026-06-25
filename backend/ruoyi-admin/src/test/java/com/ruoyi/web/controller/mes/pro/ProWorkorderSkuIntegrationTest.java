package com.ruoyi.web.controller.mes.pro;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 生产工单 SKU变体 集成测试
 * 覆盖：checkDeviation（偏离检测）+ createWithBom（变体创建+L3→L2回填）
 *
 * @author qixiaoxia
 */
@DisplayName("生产工单 SKU变体 集成测试")
class ProWorkorderSkuIntegrationTest extends BaseIntegrationTest {

    private static boolean tablesReady = false;

    @BeforeEach
    void setUp() {
        if (!tablesReady) {
            tablesReady = true;
            // 工厂
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_md_factory (factory_id bigint(20) NOT NULL AUTO_INCREMENT, factory_code varchar(64) NOT NULL, factory_name varchar(255) NOT NULL, enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (factory_id), UNIQUE KEY uk_factory_code (factory_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_factory (factory_id, factory_code, factory_name, enable_flag, create_by, create_time) VALUES (1, 'SX', '圣享工厂', '1', 'admin', NOW())");

            // 物料行业子表
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_md_item_attr_paper (attr_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, item_id bigint(20) NOT NULL, paper_width varchar(20) DEFAULT NULL, paper_weight varchar(20) DEFAULT NULL, paper_source varchar(100) DEFAULT NULL, paper_type varchar(50) DEFAULT NULL, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (attr_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_md_item_attr_paper_bag (attr_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, item_id bigint(20) NOT NULL, rope_spec varchar(200) DEFAULT NULL, mouth_type varchar(50) DEFAULT NULL, bottom_type varchar(50) DEFAULT NULL, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (attr_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_md_item_attr_gift_box (attr_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, item_id bigint(20) NOT NULL, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (attr_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_md_product_bom (bom_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, item_id bigint(20) NOT NULL, bom_item_id bigint(20) NOT NULL, unit_of_measure varchar(64) DEFAULT NULL, quantity decimal(14,4) DEFAULT 1.0000, scrap_rate decimal(5,4) DEFAULT 0.0000, enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (bom_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 物料表（含 parent_id）
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_md_item (item_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, item_code varchar(64) NOT NULL, item_name varchar(255) NOT NULL, specification varchar(500) DEFAULT NULL, unit_of_measure varchar(64) DEFAULT NULL, unit_name varchar(64) DEFAULT NULL, item_type_id bigint(20) DEFAULT NULL, item_type_code varchar(64) DEFAULT '', item_type_name varchar(255) DEFAULT '', parent_id bigint(20) DEFAULT 0, printing_req varchar(500) DEFAULT NULL, product_size varchar(100) DEFAULT NULL, package_spec varchar(100) DEFAULT NULL, enable_flag char(1) DEFAULT '1', batch_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (item_id), UNIQUE KEY uk_item_code (item_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 工序
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_pro_process (process_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, process_code varchar(64) NOT NULL, process_name varchar(255) NOT NULL, process_type varchar(32) DEFAULT 'INTERNAL', enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (process_id), UNIQUE KEY uk_process_code (process_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_pro_process (process_id, factory_id, process_code, process_name) VALUES (100, 1, 'PRINT', '印刷'), (101, 1, 'BAG', '制袋')");

            // 参数模版
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_pro_param_template (template_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, param_code varchar(64) DEFAULT NULL, param_name varchar(255) DEFAULT NULL, param_type varchar(32) DEFAULT 'STRING', process_id bigint(20) DEFAULT NULL, default_value varchar(500) DEFAULT NULL, param_group varchar(255) DEFAULT NULL, enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (template_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_pro_param_template (template_id, factory_id, param_name, process_id, default_value) VALUES (301, 1, '印刷色数', 100, '4色'), (302, 1, '印刷速度', 100, '120'), (303, 1, '制袋温度', 101, '80℃')");

            // 工艺路线（含 route_process）
            jdbcTemplate.execute("DROP TABLE IF EXISTS qxx_pro_route_process");
            jdbcTemplate.execute("DROP TABLE IF EXISTS qxx_pro_route");
            jdbcTemplate.execute("CREATE TABLE qxx_pro_route (route_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, route_code varchar(64) NOT NULL, route_name varchar(255) DEFAULT NULL, enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (route_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("CREATE TABLE qxx_pro_route_process (record_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, route_id bigint(20) NOT NULL, process_id bigint(20) DEFAULT NULL, process_code varchar(64) DEFAULT NULL, process_name varchar(255) DEFAULT NULL, process_type varchar(32) DEFAULT NULL, order_num int(11) DEFAULT 1, next_process_id bigint(20) DEFAULT NULL, next_process_code varchar(64) DEFAULT NULL, next_process_name varchar(255) DEFAULT NULL, link_type varchar(32) DEFAULT NULL, default_pre_time int(11) DEFAULT NULL, default_suf_time int(11) DEFAULT NULL, color_code varchar(32) DEFAULT NULL, key_flag char(1) DEFAULT '0', is_check char(1) DEFAULT 'N', is_outsource char(1) DEFAULT 'N', vendor_id bigint(20) DEFAULT NULL, vendor_code varchar(64) DEFAULT NULL, vendor_name varchar(255) DEFAULT NULL, outsource_factory_id bigint(20) DEFAULT NULL, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (record_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_pro_route (route_id, factory_id, route_code, route_name) VALUES (200, 1, 'RT-001', '纸袋标准路线')");

            // 路线产品关联
            jdbcTemplate.execute("DROP TABLE IF EXISTS qxx_pro_route_product");
            jdbcTemplate.execute("CREATE TABLE qxx_pro_route_product (record_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, route_id bigint(20) NOT NULL, item_id bigint(20) NOT NULL, item_code varchar(64) DEFAULT NULL, item_name varchar(255) DEFAULT NULL, specification varchar(500) DEFAULT NULL, unit_of_measure varchar(64) DEFAULT NULL, unit_name varchar(64) DEFAULT NULL, quantity int(11) DEFAULT 1, production_time bigint(20) DEFAULT NULL, time_unit_type varchar(32) DEFAULT NULL, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (record_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 路线BOM
            jdbcTemplate.execute("DROP TABLE IF EXISTS qxx_pro_route_product_bom");
            jdbcTemplate.execute("CREATE TABLE qxx_pro_route_product_bom (record_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, route_id bigint(20) NOT NULL, process_id bigint(20) DEFAULT NULL, product_id bigint(20) DEFAULT NULL, item_id bigint(20) NOT NULL, item_code varchar(64) DEFAULT NULL, item_name varchar(255) DEFAULT NULL, specification varchar(500) DEFAULT NULL, unit_of_measure varchar(64) DEFAULT NULL, unit_name varchar(64) DEFAULT NULL, quantity double DEFAULT 1.0, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (record_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 路线工序参数
            jdbcTemplate.execute("DROP TABLE IF EXISTS qxx_pro_route_process_param");
            jdbcTemplate.execute("CREATE TABLE qxx_pro_route_process_param (record_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, route_product_id bigint(20) NOT NULL, process_id bigint(20) DEFAULT NULL, template_id bigint(20) DEFAULT NULL, param_value varchar(500) DEFAULT NULL, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (record_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 工单表
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_pro_workorder (workorder_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, workorder_code varchar(64) NOT NULL, workorder_name varchar(255) DEFAULT NULL, workorder_type varchar(32) DEFAULT 'SELF', order_source varchar(32) DEFAULT 'MANUAL', source_code varchar(64) DEFAULT NULL, product_id bigint(20) DEFAULT NULL, product_code varchar(64) DEFAULT NULL, product_name varchar(255) DEFAULT NULL, product_spc varchar(255) DEFAULT NULL, route_product_id bigint(20) DEFAULT NULL, unit_of_measure varchar(32) DEFAULT NULL, unit_name varchar(64) DEFAULT NULL, quantity decimal(18,6) DEFAULT NULL, quantity_produced decimal(18,6) DEFAULT 0, quantity_changed decimal(18,6) DEFAULT 0, quantity_scheduled decimal(18,6) DEFAULT 0, request_date datetime DEFAULT NULL, status varchar(32) DEFAULT 'PREPARE', remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (workorder_id), UNIQUE KEY uk_workorder_code (workorder_code)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_pro_workorder_bom (line_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, workorder_id bigint(20) NOT NULL, item_id bigint(20) NOT NULL, item_code varchar(64) DEFAULT NULL, item_name varchar(255) DEFAULT NULL, item_spc varchar(255) DEFAULT NULL, unit_of_measure varchar(32) DEFAULT NULL, unit_name varchar(64) DEFAULT NULL, item_or_product varchar(32) DEFAULT NULL, quantity decimal(18,6) DEFAULT NULL, total_quantity decimal(18,6) DEFAULT NULL, process_id bigint(20) DEFAULT NULL, process_name varchar(255) DEFAULT NULL, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (line_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_pro_workorder_param (record_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, workorder_id bigint(20) NOT NULL, route_product_id bigint(20) DEFAULT NULL, template_id bigint(20) DEFAULT NULL, standard_value varchar(500) DEFAULT NULL, adjusted_value varchar(500) DEFAULT NULL, remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (record_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_pro_workorder_change (change_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, workorder_id bigint(20) NOT NULL, change_type varchar(32) DEFAULT NULL, field_name varchar(64) DEFAULT NULL, old_value varchar(500) DEFAULT NULL, new_value varchar(500) DEFAULT NULL, change_reason varchar(500) DEFAULT NULL, status varchar(32) DEFAULT 'APPROVED', approver varchar(64) DEFAULT NULL, approve_time datetime DEFAULT NULL, create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (change_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // 物料批次配置表
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_md_item_batch_config (config_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1, item_id bigint(20) NOT NULL, produce_date_flag char(1) DEFAULT '0', expire_date_flag char(1) DEFAULT '0', vendor_flag char(1) DEFAULT '0', workorder_flag char(1) DEFAULT '0', lot_number_flag char(1) DEFAULT '0', enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP, update_by varchar(64) DEFAULT '', update_time datetime, PRIMARY KEY (config_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }

        // 补缺列（应对其他测试先建表导致 CREATE IF NOT EXISTS 跳过的情况）
        String[] alterSqls = {
            "ALTER TABLE qxx_pro_param_template ADD COLUMN param_group varchar(255) DEFAULT NULL",
            "ALTER TABLE qxx_pro_param_template ADD COLUMN unit varchar(32) DEFAULT NULL",
            "ALTER TABLE qxx_pro_param_template ADD COLUMN enum_values varchar(500) DEFAULT NULL",
            "ALTER TABLE qxx_pro_param_template ADD COLUMN min_value decimal(14,4) DEFAULT NULL",
            "ALTER TABLE qxx_pro_param_template ADD COLUMN max_value decimal(14,4) DEFAULT NULL",
            "ALTER TABLE qxx_pro_param_template ADD COLUMN sort_order int DEFAULT 1",
            "ALTER TABLE qxx_pro_param_template ADD COLUMN is_required char(1) DEFAULT 'Y'",
            "ALTER TABLE qxx_pro_param_template ADD COLUMN is_report_visible char(1) DEFAULT 'Y'",
            "ALTER TABLE qxx_pro_route_product ADD COLUMN remark varchar(500) DEFAULT ''",
            "ALTER TABLE qxx_pro_route_process_param ADD COLUMN remark varchar(500) DEFAULT ''",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN client_id bigint(20) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN client_code varchar(64) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN client_name varchar(255) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN product_size varchar(255) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN printing_req varchar(500) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN rope_spec varchar(255) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN package_req varchar(500) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN order_type varchar(32) DEFAULT 'NEW'",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN vendor_id bigint(20) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN client_order_code varchar(64) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN attention varchar(500) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN shipping_req varchar(500) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN vendor_code varchar(64) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN vendor_name varchar(255) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN outsource_factory_id bigint(20) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN batch_code varchar(64) DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN cancel_date datetime DEFAULT NULL",
            "ALTER TABLE qxx_pro_workorder ADD COLUMN finish_date datetime DEFAULT NULL",
            "ALTER TABLE qxx_pro_process ADD COLUMN attention varchar(500) DEFAULT NULL",
            "ALTER TABLE qxx_md_item ADD COLUMN unit2 varchar(64) DEFAULT NULL",
            "ALTER TABLE qxx_md_item ADD COLUMN unit2_name varchar(64) DEFAULT NULL",
            "ALTER TABLE qxx_md_item ADD COLUMN conversion_rate decimal(10,4) DEFAULT 1.0000",
            "ALTER TABLE qxx_md_item ADD COLUMN safe_stock_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item ADD COLUMN min_stock decimal(14,4) DEFAULT 0.0000",
            "ALTER TABLE qxx_md_item ADD COLUMN max_stock decimal(14,4) DEFAULT 0.0000",
            "ALTER TABLE qxx_md_item ADD COLUMN alert_stock decimal(14,4) DEFAULT 0.0000",
            "ALTER TABLE qxx_md_item ADD COLUMN high_value char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN recpt_date_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN client_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN co_code_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN po_code_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN task_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN workstation_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN tool_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN mold_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN quality_status_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN paper_roll_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN paper_width_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_md_item_batch_config ADD COLUMN expire_date_flag char(1) DEFAULT '0'",
            "ALTER TABLE qxx_pro_route ADD COLUMN route_desc varchar(500) DEFAULT NULL",
        };
        for (String sql : alterSqls) {
            try { jdbcTemplate.execute(sql); } catch (Exception ignored) {}
        }

        // 插入种子数据
        jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_item (item_id, factory_id, item_code, item_name, unit_of_measure, unit_name, parent_id, printing_req, enable_flag) VALUES " +
            "(201, 1, 'PROD-001', '奔趣纸袋', 'PCS', '个', 0, '1色满版黑', '1'), " +
            "(202, 1, 'MAT-INK-001', '水性油墨', 'KG', '千克', 0, null, '1'), " +
            "(203, 1, 'MAT-ROPE-001', '纸绳', 'PCS', '对', 0, null, '1')");

        jdbcTemplate.execute("DELETE FROM qxx_pro_route_product WHERE item_id IN (201)");
        jdbcTemplate.execute("DELETE FROM qxx_pro_route_product_bom WHERE product_id IN (201)");
        jdbcTemplate.execute("DELETE FROM qxx_pro_route_process_param");
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder_change");
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder_param");
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder_bom");
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder");
        jdbcTemplate.execute("DELETE FROM qxx_md_item WHERE item_id > 203");

        // 路线产品关联：SPU → route 200
        jdbcTemplate.execute("INSERT INTO qxx_pro_route_product (record_id, factory_id, route_id, item_id, item_code, item_name) VALUES (300, 1, 200, 201, 'PROD-001', '奔趣纸袋')");
        // 路线BOM（2行）
        jdbcTemplate.execute("INSERT INTO qxx_pro_route_product_bom (factory_id, route_id, process_id, product_id, item_id, item_code, item_name, unit_of_measure, unit_name, quantity) VALUES " +
            "(1, 200, 100, 201, 202, 'MAT-INK-001', '水性油墨', 'KG', '千克', 0.5), " +
            "(1, 200, 101, 201, 203, 'MAT-ROPE-001', '纸绳', 'PCS', '对', 1.0)");
        // 路线工序参数（2个参数）
        jdbcTemplate.execute("INSERT INTO qxx_pro_route_process_param (factory_id, route_product_id, process_id, template_id, param_value) VALUES " +
            "(1, 300, 100, 301, '4色'), " +
            "(1, 300, 100, 302, '120')");
    }

    // ══════════════════════════════════════════════
    // 1. checkDeviation — BOM 用量偏离
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("1. checkDeviation: BOM用量偏离 → 检测成功")
    void testCheckDeviation_BomQuantityChange() {
        Map<String, Object> woBody = new HashMap<>();
        woBody.put("productId", 201);
        woBody.put("productCode", "PROD-001");
        woBody.put("routeProductId", 300);

        // 提交的BOM：INK用量从0.5→0.8
        List<Map<String, Object>> bomList = new ArrayList<>();
        Map<String, Object> bom1 = new HashMap<>();
        bom1.put("itemId", 202); bom1.put("itemCode", "MAT-INK-001");
        bom1.put("itemName", "水性油墨"); bom1.put("unitName", "千克");
        bom1.put("quantity", 0.8);
        bomList.add(bom1);
        Map<String, Object> bom2 = new HashMap<>();
        bom2.put("itemId", 203); bom2.put("itemCode", "MAT-ROPE-001");
        bom2.put("itemName", "纸绳"); bom2.put("unitName", "对");
        bom2.put("quantity", 1.0);
        bomList.add(bom2);

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("workorder", woBody);
        reqBody.put("bomList", bomList);
        reqBody.put("paramList", new ArrayList<>());

        String url = "http://localhost:" + port + "/mes/pro/workorder/checkDeviation";
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, authRequest(reqBody), Map.class);

        assertThat(resp.getBody().get("code")).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("hasDeviation")).isEqualTo(true);
        List<Map<String, Object>> deviations = (List<Map<String, Object>>) data.get("deviations");
        assertThat(deviations).hasSize(1);
        assertThat(deviations.get(0).get("diffLabel")).isEqualTo("用量变更");
        assertThat(deviations.get(0).get("itemCode")).isEqualTo("MAT-INK-001");
    }

    // ══════════════════════════════════════════════
    // 2. checkDeviation — 参数偏离
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("2. checkDeviation: 参数调整 → 检测成功")
    void testCheckDeviation_ParamDeviation() {
        Map<String, Object> woBody = new HashMap<>();
        woBody.put("productId", 201);
        woBody.put("routeProductId", 300);

        // BOM 无偏离
        List<Map<String, Object>> bomList = new ArrayList<>();
        Map<String, Object> bom1 = new HashMap<>();
        bom1.put("itemId", 202); bom1.put("itemCode", "MAT-INK-001");
        bom1.put("itemName", "水性油墨"); bom1.put("unitName", "千克");
        bom1.put("quantity", 0.5);
        bomList.add(bom1);
        Map<String, Object> bom2 = new HashMap<>();
        bom2.put("itemId", 203); bom2.put("itemCode", "MAT-ROPE-001");
        bom2.put("itemName", "纸绳"); bom2.put("unitName", "对");
        bom2.put("quantity", 1.0);
        bomList.add(bom2);

        // 参数有偏离：adjustedValue="5色" ≠ standardValue="4色"
        List<Map<String, Object>> paramList = new ArrayList<>();
        Map<String, Object> p1 = new HashMap<>();
        p1.put("templateId", 301); p1.put("standardValue", "4色");
        p1.put("adjustedValue", "5色");
        paramList.add(p1);
        Map<String, Object> p2 = new HashMap<>();
        p2.put("templateId", 302); p2.put("standardValue", "120");
        p2.put("adjustedValue", "");  // 无调整
        paramList.add(p2);

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("workorder", woBody);
        reqBody.put("bomList", bomList);
        reqBody.put("paramList", paramList);

        String url = "http://localhost:" + port + "/mes/pro/workorder/checkDeviation";
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, authRequest(reqBody), Map.class);

        assertThat(resp.getBody().get("code")).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("hasDeviation")).isEqualTo(true);
        List<Map<String, Object>> deviations = (List<Map<String, Object>>) data.get("deviations");
        assertThat(deviations).hasSize(1);
        assertThat(deviations.get(0).get("diffLabel")).isEqualTo("参数调整");
        assertThat(deviations.get(0).get("standardVal")).isEqualTo("4色");
        assertThat(deviations.get(0).get("actualVal")).isEqualTo("5色");
    }

    // ══════════════════════════════════════════════
    // 3. checkDeviation — 无偏离
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("3. checkDeviation: 无偏离 → hasDeviation=false")
    void testCheckDeviation_NoDeviation() {
        Map<String, Object> woBody = new HashMap<>();
        woBody.put("productId", 201);
        woBody.put("routeProductId", 300);

        // BOM 与标准完全一致
        List<Map<String, Object>> bomList = new ArrayList<>();
        Map<String, Object> bom1 = new HashMap<>();
        bom1.put("itemId", 202); bom1.put("itemCode", "MAT-INK-001");
        bom1.put("itemName", "水性油墨"); bom1.put("unitName", "千克");
        bom1.put("quantity", 0.5);
        bomList.add(bom1);
        Map<String, Object> bom2 = new HashMap<>();
        bom2.put("itemId", 203); bom2.put("itemCode", "MAT-ROPE-001");
        bom2.put("itemName", "纸绳"); bom2.put("unitName", "对");
        bom2.put("quantity", 1.0);
        bomList.add(bom2);

        List<Map<String, Object>> paramList = new ArrayList<>();
        Map<String, Object> p1 = new HashMap<>();
        p1.put("templateId", 301); p1.put("standardValue", "4色");
        p1.put("adjustedValue", "");  // 无调整
        paramList.add(p1);

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("workorder", woBody);
        reqBody.put("bomList", bomList);
        reqBody.put("paramList", paramList);

        String url = "http://localhost:" + port + "/mes/pro/workorder/checkDeviation";
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, authRequest(reqBody), Map.class);

        assertThat(resp.getBody().get("code")).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("hasDeviation")).isEqualTo(false);
        List<Map<String, Object>> deviations = (List<Map<String, Object>>) data.get("deviations");
        assertThat(deviations).isEmpty();
    }

    // ══════════════════════════════════════════════
    // 4. createWithBom + SKU变体 全链路
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("4. createWithBom: SKU变体全链路（创建+路线复制+L3→L2回填）")
    void testCreateWithBom_SkuFullChain() {
        String code = "WO_SKU_FULL_" + System.nanoTime();
        String skuCode = "PROD-001-V99";

        Map<String, Object> woBody = new HashMap<>();
        woBody.put("workorderCode", code);
        woBody.put("workorderName", "SKU全链路测试");
        woBody.put("productId", 201);
        woBody.put("productCode", "PROD-001");
        woBody.put("productName", "奔趣纸袋");
        woBody.put("productSpc", "254*127*330mm");
        woBody.put("routeProductId", 300);
        woBody.put("unitOfMeasure", "PCS");
        woBody.put("unitName", "个");
        woBody.put("quantity", new BigDecimal("100"));
        woBody.put("factoryId", 1);
        woBody.put("createSkuVariant", true);
        woBody.put("skuCode", skuCode);
        woBody.put("skuName", "奔趣-集成测试变体");

        // BOM：INK用量从0.5→0.8（偏离）
        List<Map<String, Object>> bomList = new ArrayList<>();
        Map<String, Object> bom1 = new HashMap<>();
        bom1.put("itemId", 202); bom1.put("itemCode", "MAT-INK-001");
        bom1.put("itemName", "水性油墨"); bom1.put("unitOfMeasure", "KG");
        bom1.put("unitName", "千克"); bom1.put("itemOrProduct", "RAW");
        bom1.put("quantity", new BigDecimal("0.8"));
        bom1.put("processId", 100); bom1.put("processName", "印刷");
        bomList.add(bom1);
        Map<String, Object> bom2 = new HashMap<>();
        bom2.put("itemId", 203); bom2.put("itemCode", "MAT-ROPE-001");
        bom2.put("itemName", "纸绳"); bom2.put("unitOfMeasure", "PCS");
        bom2.put("unitName", "对"); bom2.put("itemOrProduct", "RAW");
        bom2.put("quantity", new BigDecimal("1.0"));
        bom2.put("processId", 101); bom2.put("processName", "制袋");
        bomList.add(bom2);

        // 参数：adjustedValue="5色" ≠ standardValue="4色"
        List<Map<String, Object>> paramList = new ArrayList<>();
        Map<String, Object> p1 = new HashMap<>();
        p1.put("templateId", 301); p1.put("standardValue", "4色");
        p1.put("adjustedValue", "5色");
        paramList.add(p1);

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("workorder", woBody);
        reqBody.put("bomList", bomList);
        reqBody.put("paramList", paramList);

        String url = "http://localhost:" + port + "/mes/pro/workorder/createWithBom";
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, authRequest(reqBody), Map.class);

        assertThat(resp.getBody().get("code")).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");

        // ① 变体 item 已创建
        Long skuItemId = ((Number) data.get("productId")).longValue();
        assertThat(skuItemId).isGreaterThan(201);
        assertThat(data.get("productCode")).isEqualTo(skuCode);

        Map<String, Object> skuItem = jdbcTemplate.queryForMap(
                "SELECT * FROM qxx_md_item WHERE item_id = ?", skuItemId);
        assertThat(skuItem.get("parent_id")).isEqualTo(201L);
        assertThat(skuItem.get("item_code")).isEqualTo(skuCode);
        assertThat(skuItem.get("item_name")).isEqualTo("奔趣-集成测试变体");

        // ② 路线产品已复制
        Integer rpCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_route_product WHERE item_id = ?", Integer.class, skuItemId);
        assertThat(rpCount).isEqualTo(1);

        // ③ ★ 变体路线BOM 已用工单BOM替换（INK用量=0.8，非父产品的0.5）
        Double skuBomQty = jdbcTemplate.queryForObject(
                "SELECT quantity FROM qxx_pro_route_product_bom WHERE product_id = ? AND item_id = 202",
                Double.class, skuItemId);
        assertThat(skuBomQty).isEqualTo(0.8);  // ← L3回填到L2

        // ④ ★ 变体路线参数 已用工单调整值覆盖（5色，非父产品的4色）
        Long skuRouteProductId = jdbcTemplate.queryForObject(
                "SELECT record_id FROM qxx_pro_route_product WHERE item_id = ? LIMIT 1",
                Long.class, skuItemId);
        String skuParamValue = jdbcTemplate.queryForObject(
                "SELECT param_value FROM qxx_pro_route_process_param WHERE route_product_id = ? AND template_id = 301",
                String.class, skuRouteProductId);
        assertThat(skuParamValue).isEqualTo("5色");  // ← L3回填到L2

        // ⑤ 变更记录已写入
        Integer changeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_workorder_change WHERE workorder_id = ?",
                Integer.class, ((Number) data.get("workorderId")).longValue());
        assertThat(changeCount).isGreaterThanOrEqualTo(1);
    }

    // ══════════════════════════════════════════════
    // 5. createWithBom — 不创建变体
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("5. createWithBom: createSkuVariant=false → 不创建变体")
    void testCreateWithBom_NoSku() {
        String code = "WO_NO_SKU_" + System.nanoTime();

        Map<String, Object> woBody = new HashMap<>();
        woBody.put("workorderCode", code);
        woBody.put("workorderName", "不创建变体测试");
        woBody.put("productId", 201);
        woBody.put("productCode", "PROD-001");
        woBody.put("productName", "奔趣纸袋");
        woBody.put("routeProductId", 300);
        woBody.put("quantity", new BigDecimal("50"));
        woBody.put("factoryId", 1);
        woBody.put("createSkuVariant", false);

        List<Map<String, Object>> bomList = new ArrayList<>();
        Map<String, Object> bom = new HashMap<>();
        bom.put("itemId", 202); bom.put("itemCode", "MAT-INK-001");
        bom.put("itemName", "水性油墨"); bom.put("unitName", "千克");
        bom.put("quantity", new BigDecimal("1.0"));  // 有偏离但不创建变体
        bom.put("processId", 100); bom.put("processName", "印刷");
        bomList.add(bom);

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("workorder", woBody);
        reqBody.put("bomList", bomList);
        reqBody.put("paramList", new ArrayList<>());

        String url = "http://localhost:" + port + "/mes/pro/workorder/createWithBom";
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, authRequest(reqBody), Map.class);

        assertThat(resp.getBody().get("code")).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");

        // productId 不变
        assertThat(data.get("productId")).isEqualTo(201);
        assertThat(data.get("productCode")).isEqualTo("PROD-001");

        // 没有创建新 item
        Integer skuCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_md_item WHERE parent_id = 201", Integer.class);
        assertThat(skuCount).isEqualTo(0);
    }

    // ══════════════════════════════════════════════
    // 6. checkDeviation — BOM新增行
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("6. checkDeviation: BOM新增物料行 → 检测到物料新增")
    void testCheckDeviation_BomAddLine() {
        Map<String, Object> woBody = new HashMap<>();
        woBody.put("productId", 201);
        woBody.put("routeProductId", 300);

        // 提交3个物料（标准只有2个）
        List<Map<String, Object>> bomList = new ArrayList<>();
        Map<String, Object> bom1 = new HashMap<>();
        bom1.put("itemId", 202); bom1.put("itemCode", "MAT-INK-001");
        bom1.put("itemName", "水性油墨"); bom1.put("quantity", 0.5);
        bomList.add(bom1);
        Map<String, Object> bom2 = new HashMap<>();
        bom2.put("itemId", 203); bom2.put("itemCode", "MAT-ROPE-001");
        bom2.put("itemName", "纸绳"); bom2.put("quantity", 1.0);
        bomList.add(bom2);
        Map<String, Object> bom3 = new HashMap<>();
        bom3.put("itemId", 999); bom3.put("itemCode", "MAT-NEW-001");
        bom3.put("itemName", "新增物料"); bom3.put("quantity", 2.0);
        bomList.add(bom3);

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("workorder", woBody);
        reqBody.put("bomList", bomList);
        reqBody.put("paramList", new ArrayList<>());

        String url = "http://localhost:" + port + "/mes/pro/workorder/checkDeviation";
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, authRequest(reqBody), Map.class);

        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("hasDeviation")).isEqualTo(true);
        List<Map<String, Object>> deviations = (List<Map<String, Object>>) data.get("deviations");
        assertThat(deviations).anyMatch(d -> "物料新增".equals(d.get("diffLabel")));
    }
}
