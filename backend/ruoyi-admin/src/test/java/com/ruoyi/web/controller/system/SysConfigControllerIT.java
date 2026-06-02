package com.ruoyi.web.controller.system;

import com.ruoyi.BaseIntegrationTest;
import com.ruoyi.system.domain.SysConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数配置 Controller 集成测试。
 * <p>
 * 全链路：Controller → Service → Mapper → Testcontainers MySQL。
 *
 * @author qixiaoxia
 */
@DisplayName("参数配置 Controller 集成测试")
class SysConfigControllerIT extends BaseIntegrationTest {

    @Autowired
    private com.ruoyi.system.mapper.SysConfigMapper configMapper;

    @BeforeEach
    void setUp() {
        // 清理测试创建的配置（ID >= 100），保留系统内置配置（ID 1-9）
        jdbcTemplate.execute("DELETE FROM sys_config WHERE config_id >= 100");
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("should return config list with rows")
    void shouldReturnConfigList_whenAuthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/system/config/list",
                HttpMethod.GET,
                authRequest(),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("rows")).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        assertThat(rows).isNotEmpty();
    }

    @Test
    @DisplayName("should return config detail when configId exists")
    void shouldReturnConfigDetail_whenConfigIdExists() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/system/config/1",
                HttpMethod.GET,
                authRequest(),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("msg")).isEqualTo("操作成功");
        assertThat(body.get("data")).isNotNull();
    }

    // ==================== 新增 ====================

    @Test
    @DisplayName("should create config and persist to database")
    void shouldCreateConfigAndPersistToDb_whenValidInput() {
        SysConfig newConfig = new SysConfig();
        newConfig.setConfigName("集成测试参数");
        newConfig.setConfigKey("it.test.key");
        newConfig.setConfigValue("testValue");
        newConfig.setConfigType("N");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/system/config",
                authRequest(newConfig),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("code")).isEqualTo(200);

        // 验证数据库最终状态
        SysConfig saved = configMapper.checkConfigKeyUnique("it.test.key");
        assertThat(saved).isNotNull();
        assertThat(saved.getConfigName()).isEqualTo("集成测试参数");
        assertThat(saved.getConfigValue()).isEqualTo("testValue");
    }

    @Test
    @DisplayName("should return error when config key already exists")
    void shouldReturnError_whenConfigKeyAlreadyExists() {
        // 先创建一个
        SysConfig first = new SysConfig();
        first.setConfigName("第一个");
        first.setConfigKey("it.duplicate.key");
        first.setConfigValue("v1");
        first.setConfigType("N");
        ResponseEntity<Map> firstResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/system/config",
                authRequest(first),
                Map.class
        );
        // 验证第一个创建成功，避免 false positive
        assertThat(firstResponse.getBody().get("code")).isEqualTo(200);

        // 再用相同键名创建
        SysConfig second = new SysConfig();
        second.setConfigName("第二个");
        second.setConfigKey("it.duplicate.key");
        second.setConfigValue("v2");
        second.setConfigType("N");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/system/config",
                authRequest(second),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("code")).isEqualTo(500);
        assertThat((String) body.get("msg")).contains("参数键名已存在");
    }

    // ==================== 删除 ====================

    @Test
    @DisplayName("should delete non-built-in config successfully")
    void shouldDeleteConfig_whenNotBuiltIn() {
        // 先创建一个非内置参数
        SysConfig toDelete = new SysConfig();
        toDelete.setConfigName("待删除参数");
        toDelete.setConfigKey("it.delete.key");
        toDelete.setConfigValue("val");
        toDelete.setConfigType("N");
        restTemplate.postForEntity(
                "http://localhost:" + port + "/system/config",
                authRequest(toDelete),
                Map.class
        );

        // 获取创建后的ID
        SysConfig saved = configMapper.checkConfigKeyUnique("it.delete.key");
        assertThat(saved).isNotNull();

        // 删除
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/system/config/" + saved.getConfigId(),
                HttpMethod.DELETE,
                authRequest(),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("code")).isEqualTo(200);

        // 验证已删除
        SysConfig afterDelete = configMapper.checkConfigKeyUnique("it.delete.key");
        assertThat(afterDelete).isNull();
    }

    @Test
    @DisplayName("should return error when deleting built-in config")
    void shouldReturnError_whenDeletingBuiltInConfig() {
        // sys_config ID=1 是内置参数 (configType='Y')
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/system/config/1",
                HttpMethod.DELETE,
                authRequest(),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("code")).isEqualTo(500);
        assertThat((String) body.get("msg")).contains("内置参数").contains("不能删除");
    }
}
