package com.ruoyi.web.controller.system;

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
 * FactoryIdInterceptor 集成测试 — 验证 Mapper 层自动注入 factory_id。
 */
@DisplayName("FactoryIdInterceptor 集成测试")
class FactoryIdInterceptorIT extends BaseIntegrationTest {

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sys_user_role WHERE user_id > 100");
        jdbcTemplate.execute("DELETE FROM sys_user_post WHERE user_id > 100");
        jdbcTemplate.execute("DELETE FROM sys_user WHERE user_id > 100");
        jdbcTemplate.execute("DELETE FROM sys_role_menu WHERE role_id > 100");
        jdbcTemplate.execute("DELETE FROM sys_role_dept WHERE role_id > 100");
        jdbcTemplate.execute("DELETE FROM sys_role WHERE role_id > 100");
        jdbcTemplate.execute("DELETE FROM sys_dept WHERE dept_id > 200");
    }

    @Test
    @DisplayName("INSERT sys_role 自动注入 factory_id = 1")
    void shouldAutoInjectFactoryIdOnInsertRole() {
        String key = "it_role_" + System.nanoTime();
        Map<String, Object> role = Map.of(
                "roleName", "IT角色", "roleKey", key, "roleSort", 99,
                "status", "0", "menuIds", List.of(1));

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/system/role", authRequest(role), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        Long fid = jdbcTemplate.queryForObject(
                "SELECT factory_id FROM sys_role WHERE role_key = ?", Long.class, key);
        assertThat(fid).isEqualTo(1L);
    }

    @Test
    @DisplayName("INSERT sys_dept 自动注入 factory_id = 1")
    void shouldAutoInjectFactoryIdOnInsertDept() {
        String name = "IT部门" + System.nanoTime();
        Map<String, Object> dept = Map.of(
                "parentId", 100, "deptName", name, "orderNum", 99, "status", "0");

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/system/dept", authRequest(dept), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        Long fid = jdbcTemplate.queryForObject(
                "SELECT factory_id FROM sys_dept WHERE dept_name = ? AND del_flag = '0'", Long.class, name);
        assertThat(fid).isEqualTo(1L);
    }

    @Test
    @DisplayName("sys_config 查询不注入 factory_id")
    void shouldNotInjectFactoryIdOnSysConfig() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/system/config/1",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat(resp.getBody().get("data")).isNotNull();
    }

    @Test
    @DisplayName("sys_user JOIN sys_dept 使用正确别名")
    void shouldUseCorrectAliasForUserJoin() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/system/user/list",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("sys_role JOIN 使用正确别名")
    void shouldUseCorrectAliasForRoleJoin() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/system/role/list",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("sys_dept 含子查询使用正确别名")
    void shouldUseCorrectAliasForDeptSubquery() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/system/dept/100",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }
}
