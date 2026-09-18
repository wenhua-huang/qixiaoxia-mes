package com.ruoyi.web.controller.mes.md;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工厂定义 Controller 集成测试。
 * 工厂是只读主数据（由开发直接 SQL 维护），Controller 仅暴露 /list 与 /listAll，
 * 不提供详情、新增、修改、删除端点。
 */
@DisplayName("工厂定义 Controller 集成测试（只读）")
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
    @DisplayName("listAll 只返回启用工厂")
    void shouldListAllEnabled() {
        jdbcTemplate.update(
                "INSERT INTO qxx_md_factory (factory_code,factory_name,enable_flag,create_by,create_time) " +
                "VALUES (?, '停用工厂', '0', 'admin', NOW())",
                "FT_DISABLED_" + System.nanoTime());

        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/factory/listAll",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        List<Map> rows = (List<Map>) resp.getBody().get("data");
        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(row -> assertThat(row.get("enableFlag")).isEqualTo("1"));
    }

    @Test
    @DisplayName("只读契约：不暴露详情/新增/修改/删除端点")
    void shouldNotExposeWriteEndpoints() {
        String base = "http://localhost:" + port + "/mes/md/factory";
        Map<String, Object> body = Map.of(
                "factoryCode", "FT_RO_" + System.nanoTime(),
                "factoryName", "只读测试", "enableFlag", "1");

        ResponseEntity<Map> getDetail = restTemplate.exchange(
                base + "/1", HttpMethod.GET, authRequest(), Map.class);
        ResponseEntity<Map> post = restTemplate.postForEntity(base, authRequest(body), Map.class);
        ResponseEntity<Map> put = restTemplate.exchange(
                base, HttpMethod.PUT, authRequest(body), Map.class);
        ResponseEntity<Map> delete = restTemplate.exchange(
                base + "/1", HttpMethod.DELETE, authRequest(), Map.class);

        // 端点不存在时落到全局异常处理，返回非 200 业务码；任何一个返回 200 都意味着写接口被重新开放
        assertThat(getDetail.getBody().get("code")).isNotEqualTo(200);
        assertThat(post.getBody().get("code")).isNotEqualTo(200);
        assertThat(put.getBody().get("code")).isNotEqualTo(200);
        assertThat(delete.getBody().get("code")).isNotEqualTo(200);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_md_factory WHERE factory_name = '只读测试'", Integer.class);
        assertThat(count).isZero();
    }
}
