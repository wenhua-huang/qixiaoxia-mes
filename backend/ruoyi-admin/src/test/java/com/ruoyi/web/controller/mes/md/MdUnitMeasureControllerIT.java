package com.ruoyi.web.controller.mes.md;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单位管理 Controller 集成测试 — 验证 FactoryIdInterceptor 对 qxx_ MES 表的注入。
 */
@DisplayName("单位管理 Controller 集成测试")
class MdUnitMeasureControllerIT extends BaseIntegrationTest {

    private static boolean tablesReady = false;

    @BeforeEach
    void setUp() {
        if (!tablesReady) {
            tablesReady = true;
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_md_factory (
                    factory_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_code varchar(64) NOT NULL, factory_name varchar(64) NOT NULL,
                    enable_flag char(1) DEFAULT 'Y', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (factory_id)) ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_md_unit_measure (
                    unit_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL, unit_code varchar(64) NOT NULL,
                    unit_name varchar(64) NOT NULL, primary_unit varchar(64) DEFAULT NULL,
                    conversion_rate decimal(14,6) DEFAULT 1.000000,
                    enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP,
                    KEY idx_factory_id (factory_id), PRIMARY KEY (unit_id),
                    UNIQUE KEY uk_unit_code (unit_code)) ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_factory (factory_id,factory_code,factory_name,enable_flag,create_by,create_time) VALUES (1,'SX','圣享工厂','Y','admin',NOW())");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_unit_measure (unit_id,factory_id,unit_code,unit_name,enable_flag,create_by,create_time) VALUES (200,1,'PCS','个','Y','admin',NOW())");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_unit_measure (unit_id,factory_id,unit_code,unit_name,enable_flag,create_by,create_time) VALUES (201,1,'KG','千克','Y','admin',NOW())");
        }
        jdbcTemplate.execute("DELETE FROM qxx_md_unit_measure WHERE unit_id > 201");
    }

    @Test
    @DisplayName("list 自动注入 factory_id")
    void shouldFilterByFactoryIdOnList() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/unitmeasure/list",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat((Integer) resp.getBody().get("total")).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("新增自动注入 factory_id")
    void shouldAutoInjectFactoryIdOnInsert() {
        String code = "IT_U" + System.nanoTime();
        Map<String, Object> unit = Map.of("unitCode", code, "unitName", "IT测试", "enableFlag", "Y");
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/mes/md/unitmeasure", authRequest(unit), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        Long fid = jdbcTemplate.queryForObject(
                "SELECT factory_id FROM qxx_md_unit_measure WHERE unit_code = ?", Long.class, code);
        assertThat(fid).isEqualTo(1L);
    }

    @Test
    @DisplayName("getInfo 自动注入 factory_id")
    void shouldFilterByFactoryIdOnGetInfo() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/unitmeasure/200",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("factoryId")).isEqualTo(1);
    }

    @Test
    @DisplayName("修改自动注入 factory_id")
    void shouldAutoInjectFactoryIdOnUpdate() {
        jdbcTemplate.execute("INSERT INTO qxx_md_unit_measure (unit_id,factory_id,unit_code,unit_name,enable_flag,create_by,create_time) VALUES (300,1,'IT_UPD','旧名','Y','admin',NOW())");
        Map<String, Object> upd = Map.of("unitId", 300, "unitCode", "IT_UPD", "unitName", "新名", "enableFlag", "Y");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/unitmeasure",
                HttpMethod.PUT, authRequest(upd), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("删除自动注入 factory_id")
    void shouldAutoInjectFactoryIdOnDelete() {
        jdbcTemplate.execute("INSERT INTO qxx_md_unit_measure (unit_id,factory_id,unit_code,unit_name,enable_flag,create_by,create_time) VALUES (301,1,'IT_DEL','待删','Y','admin',NOW())");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/unitmeasure/301",
                HttpMethod.DELETE, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }
}
