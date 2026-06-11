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
 * 车间管理 Controller 集成测试
 */
@DisplayName("车间管理 Controller 集成测试")
class MdWorkshopControllerIT extends BaseIntegrationTest {

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
                CREATE TABLE IF NOT EXISTS qxx_md_workshop (
                    workshop_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL, workshop_code varchar(64) NOT NULL,
                    workshop_name varchar(255) NOT NULL, address varchar(500) DEFAULT NULL,
                    manager varchar(64) DEFAULT NULL,
                    enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_factory_id (factory_id), PRIMARY KEY (workshop_id))
                    ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_factory (factory_id,factory_code,factory_name,enable_flag,create_by,create_time) VALUES (1,'SX','圣享工厂','1','admin',NOW())");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_workshop (workshop_id,factory_id,workshop_code,workshop_name,enable_flag,create_by,create_time) VALUES (200,1,'WS-TEST','测试车间','1','admin',NOW())");
        }
        jdbcTemplate.execute("DELETE FROM qxx_md_workshop WHERE workshop_id > 200");
    }

    @Test
    @DisplayName("list 自动注入 factory_id")
    void shouldFilterByFactoryIdOnList() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workshop/list",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat((Integer) resp.getBody().get("total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("新增自动注入 factory_id")
    void shouldAutoInjectFactoryIdOnInsert() {
        String code = "WS_" + System.nanoTime();
        Map<String, Object> body = Map.of("workshopCode", code, "workshopName", "测试车间", "enableFlag", "1");
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/mes/md/workshop", authRequest(body), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        Long fid = jdbcTemplate.queryForObject(
                "SELECT factory_id FROM qxx_md_workshop WHERE workshop_code = ?", Long.class, code);
        assertThat(fid).isEqualTo(1L);
    }

    @Test
    @DisplayName("listAll 返回启用车间")
    void shouldListAllEnabled() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workshop/listAll",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("修改自动注入 factory_id")
    void shouldAutoInjectFactoryIdOnUpdate() {
        jdbcTemplate.execute("INSERT INTO qxx_md_workshop (workshop_id,factory_id,workshop_code,workshop_name,enable_flag,create_by,create_time) VALUES (300,1,'WS_UPD','旧名','1','admin',NOW())");
        Map<String, Object> body = Map.of("workshopId", 300, "workshopCode", "WS_UPD", "workshopName", "新车间名", "enableFlag", "1");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workshop",
                HttpMethod.PUT, authRequest(body), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("删除自动注入 factory_id")
    void shouldAutoInjectFactoryIdOnDelete() {
        jdbcTemplate.execute("INSERT INTO qxx_md_workshop (workshop_id,factory_id,workshop_code,workshop_name,enable_flag,create_by,create_time) VALUES (301,1,'WS_DEL','待删','1','admin',NOW())");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workshop/301",
                HttpMethod.DELETE, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }
}
