package com.ruoyi.web.controller.mes.md;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("物料产品 Controller 集成测试")
class MdItemControllerIT extends BaseIntegrationTest {

    private static boolean tablesReady = false;

    @BeforeEach
    void setUp() {
        if (!tablesReady) {
            tablesReady = true;
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_md_factory (
                    factory_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_code varchar(64) NOT NULL, factory_name varchar(255) NOT NULL,
                    enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (factory_id), UNIQUE KEY uk_factory_code (factory_code))
                    ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_md_item (
                    item_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL, item_code varchar(64) NOT NULL,
                    item_name varchar(255) NOT NULL, specification varchar(500) DEFAULT NULL,
                    unit_of_measure varchar(64) NOT NULL, unit_name varchar(64) NOT NULL,
                    unit2 varchar(64) DEFAULT NULL, unit2_name varchar(64) DEFAULT NULL,
                    conversion_rate decimal(10,4) DEFAULT 1.0000,
                    item_type_id bigint(20) DEFAULT 0, item_type_code varchar(64) DEFAULT '',
                    item_type_name varchar(255) DEFAULT '',
                    parent_id bigint(20) DEFAULT 0, product_size varchar(100) DEFAULT NULL,
                    package_spec varchar(100) DEFAULT NULL, printing_req varchar(500) DEFAULT NULL,
                    enable_flag char(1) DEFAULT '1', safe_stock_flag char(1) DEFAULT '0',
                    min_stock decimal(14,4) DEFAULT 0.0000, max_stock decimal(14,4) DEFAULT 0.0000,
                    alert_stock decimal(14,4) DEFAULT 0.0000, high_value char(1) DEFAULT '0',
                    batch_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (item_id), UNIQUE KEY uk_item_code (item_code),
                    KEY idx_factory_id (factory_id)) ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_factory (factory_id,factory_code,factory_name,enable_flag,create_by,create_time) VALUES (1,'SX','圣享工厂','1','admin',NOW())");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_item (item_id,factory_id,item_code,item_name,unit_of_measure,unit_name,enable_flag,create_by,create_time) VALUES (200,1,'ITEM-TEST','测试物料','PCS','个','1','admin',NOW())");
        }
        jdbcTemplate.execute("DELETE FROM qxx_md_item WHERE item_id > 200");
    }

    @Test
    @DisplayName("list 自动注入 factory_id")
    void shouldListItems() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/item/list",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat((Integer) resp.getBody().get("total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("新增自动注入 factory_id")
    void shouldInsertItem() {
        String code = "ITM_" + System.nanoTime();
        Map<String, Object> body = Map.of("itemCode", code, "itemName", "测试物料2",
                "unitOfMeasure", "PCS", "unitName", "个", "enableFlag", "1");
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/mes/md/item", authRequest(body), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("getInfo 返回物料详情")
    void shouldGetItemInfo() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/item/200",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }
}
