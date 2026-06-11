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
 * 工厂定义 Controller 集成测试
 */
@DisplayName("工厂定义 Controller 集成测试")
class MdFactoryControllerIT extends BaseIntegrationTest {

    private static boolean tablesReady = false;

    @BeforeEach
    void setUp() {
        if (!tablesReady) {
            tablesReady = true;
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_md_factory (
                    factory_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_code varchar(64) NOT NULL, factory_name varchar(255) NOT NULL,
                    short_name varchar(64) DEFAULT NULL, address varchar(500) DEFAULT NULL,
                    contact varchar(64) DEFAULT NULL, phone varchar(20) DEFAULT NULL,
                    enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (factory_id), UNIQUE KEY uk_factory_code (factory_code))
                    ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_factory (factory_id,factory_code,factory_name,enable_flag,create_by,create_time) VALUES (1,'SX','圣享工厂','1','admin',NOW())");
        }
        jdbcTemplate.execute("DELETE FROM qxx_md_factory WHERE factory_id > 1");
    }

    @Test
    @DisplayName("list 返回工厂列表")
    void shouldListFactories() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/factory/list",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat((Integer) resp.getBody().get("total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("listAll 返回启用工厂")
    void shouldListAllEnabled() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/factory/listAll",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("新增工厂")
    void shouldInsertFactory() {
        String code = "FT_" + System.nanoTime();
        Map<String, Object> body = Map.of("factoryCode", code, "factoryName", "测试工厂", "enableFlag", "1");
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/mes/md/factory", authRequest(body), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        String name = jdbcTemplate.queryForObject(
                "SELECT factory_name FROM qxx_md_factory WHERE factory_code = ?", String.class, code);
        assertThat(name).isEqualTo("测试工厂");
    }

    @Test
    @DisplayName("编码重复拒绝")
    void shouldRejectDuplicateCode() {
        String code = "DUP_" + System.nanoTime();
        jdbcTemplate.update("INSERT INTO qxx_md_factory (factory_code,factory_name,enable_flag,create_by,create_time) VALUES (?,?,?,?,NOW())",
                code, "第一", "1", "admin");
        Map<String, Object> body = Map.of("factoryCode", code, "factoryName", "第二", "enableFlag", "1");
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/mes/md/factory", authRequest(body), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(500);
    }

    @Test
    @DisplayName("getInfo 查询工厂详情")
    void shouldGetFactoryById() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/factory/1",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("修改工厂")
    void shouldUpdateFactory() {
        jdbcTemplate.update("INSERT INTO qxx_md_factory (factory_id,factory_code,factory_name,enable_flag,create_by,create_time) VALUES (100,'FT_UP','旧名','1','admin',NOW())");
        Map<String, Object> body = Map.of("factoryId", 100, "factoryCode", "FT_UP", "factoryName", "新名", "enableFlag", "1");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/factory",
                HttpMethod.PUT, authRequest(body), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("删除工厂")
    void shouldDeleteFactory() {
        jdbcTemplate.update("INSERT INTO qxx_md_factory (factory_id,factory_code,factory_name,enable_flag,create_by,create_time) VALUES (101,'FT_DEL','待删','1','admin',NOW())");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/factory/101",
                HttpMethod.DELETE, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }
}
