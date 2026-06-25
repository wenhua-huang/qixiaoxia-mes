package com.ruoyi.web.controller.mes.md;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工作站管理 Controller 集成测试 — 验证 processId（关联 qxx_pro_process）CRUD
 */
@DisplayName("工作站管理 Controller 集成测试")
class MdWorkstationControllerIT extends BaseIntegrationTest {

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
                    workshop_name varchar(255) NOT NULL,
                    enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_factory_id (factory_id), PRIMARY KEY (workshop_id))
                    ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_pro_process (
                    process_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL, process_code varchar(64) NOT NULL,
                    process_name varchar(255) NOT NULL, process_type varchar(50) DEFAULT 'INTERNAL',
                    enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_factory_id (factory_id), PRIMARY KEY (process_id),
                    UNIQUE KEY uk_process_code (process_code))
                    ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS qxx_md_workstation (
                    workstation_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL, workstation_code varchar(64) NOT NULL,
                    workstation_name varchar(255) NOT NULL, workshop_id bigint(20) NOT NULL,
                    workstation_type varchar(50) DEFAULT NULL,
                    process_id bigint(20) DEFAULT NULL,
                    process_type varchar(50) DEFAULT NULL,
                    capacity int(11) DEFAULT 0, status varchar(20) DEFAULT 'IDLE',
                    enable_flag char(1) DEFAULT '1', remark varchar(500) DEFAULT '',
                    create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_factory_id (factory_id), KEY idx_process_id (process_id),
                    PRIMARY KEY (workstation_id))
                    ENGINE=InnoDB CHARSET=utf8mb4""");

            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_factory (factory_id,factory_code,factory_name,enable_flag,create_by,create_time) VALUES (1,'SX','圣享工厂','1','admin',NOW())");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_workshop (workshop_id,factory_id,workshop_code,workshop_name,enable_flag,create_by,create_time) VALUES (200,1,'WS-TEST','测试车间','1','admin',NOW())");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_pro_process (process_id,factory_id,process_code,process_name,process_type,enable_flag,create_by,create_time) VALUES (100,1,'PRC-001','印刷','INTERNAL','1','admin',NOW())");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_pro_process (process_id,factory_id,process_code,process_name,process_type,enable_flag,create_by,create_time) VALUES (200,1,'PRC-002','制袋','INTERNAL','1','admin',NOW())");
        }
        // 清理所有测试数据（使用 TRUNCATE 避免自增 ID 累积）
        jdbcTemplate.execute("DELETE FROM qxx_md_workstation");
    }

    // ══════════════════════════════════════════════
    // 辅助方法：用 HashMap 构造 body（避免 Map.of 类型推断问题）
    // ══════════════════════════════════════════════
    private static Map<String, Object> body() {
        return new LinkedHashMap<>();
    }

    // ══════════════════════════════════════════════
    // 1. 新增工作站含 processId → DB 验证
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("新增工作站时 processId 应正确入库")
    void shouldInsertWorkstationWithProcessId() {
        String code = "WS_" + System.nanoTime();
        Map<String, Object> b = body();
        b.put("workstationCode", code);
        b.put("workstationName", "测试印刷机");
        b.put("workshopId", 200);
        b.put("processId", 100);
        b.put("capacity", 500);
        b.put("status", "IDLE");
        b.put("enableFlag", "1");

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/mes/md/workstation", authRequest(b), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        // 直接查 DB 验证 process_id
        Long dbProcessId = jdbcTemplate.queryForObject(
                "SELECT process_id FROM qxx_md_workstation WHERE workstation_code = ?", Long.class, code);
        assertThat(dbProcessId).isEqualTo(100L);
    }

    // ══════════════════════════════════════════════
    // 2. 直接查 DB 确认 process_id 存在 → 通过 API 获取验证序列化
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("通过 JDBC 写入后 API getInfo 应返回 processId 和 processName")
    void shouldReturnProcessIdAndNameOnGetInfo() {
        String code = "WS_GET_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_md_workstation (workstation_id,factory_id,workstation_code,workstation_name,workshop_id,process_id,process_type,status,enable_flag,create_by,create_time) VALUES (?,1,?,?,200,100,'INTERNAL','IDLE','1','admin',NOW())",
                510, code, "详情测试工作站");

        // 先直接查 DB 确认写入
        Long dbProcessId = jdbcTemplate.queryForObject(
                "SELECT process_id FROM qxx_md_workstation WHERE workstation_id = 510", Long.class);
        assertThat(dbProcessId).isEqualTo(100L);

        // 通过 API 获取
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workstation/510",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        Map<String, Object> data = (Map<String, Object>) resp.getBody().get("data");
        assertThat(data.get("processId")).isEqualTo(100);
        assertThat(data.get("processName")).isEqualTo("印刷");
    }

    // ══════════════════════════════════════════════
    // 3. 列表查询含 processId + processName
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("列表查询应通过 LEFT JOIN 返回 processName")
    void shouldReturnProcessNameOnList() {
        String code = "WS_LIST_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_md_workstation (workstation_id,factory_id,workstation_code,workstation_name,workshop_id,process_id,process_type,status,enable_flag,create_by,create_time) VALUES (?,1,?,?,200,100,'INTERNAL','IDLE','1','admin',NOW())",
                520, code, "列表测试工作站");

        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workstation/list?workstationCode=" + code,
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) resp.getBody().get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("processId")).isEqualTo(100);
        assertThat(rows.get(0).get("processName")).isEqualTo("印刷");
    }

    // ══════════════════════════════════════════════
    // 4. 按 processId 筛选
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("应按 processId 筛选工作站列表")
    void shouldFilterByProcessId() {
        String code1 = "WS_F1_" + System.nanoTime();
        String code2 = "WS_F2_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_md_workstation (workstation_id,factory_id,workstation_code,workstation_name,workshop_id,process_id,process_type,status,enable_flag,create_by,create_time) VALUES (?,1,?,?,200,100,'INTERNAL','IDLE','1','admin',NOW())",
                531, code1, "印刷工作站");
        jdbcTemplate.update(
                "INSERT INTO qxx_md_workstation (workstation_id,factory_id,workstation_code,workstation_name,workshop_id,process_id,process_type,status,enable_flag,create_by,create_time) VALUES (?,1,?,?,200,200,'INTERNAL','IDLE','1','admin',NOW())",
                532, code2, "制袋工作站");

        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workstation/list?processId=100",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        List<Map<String, Object>> rows = (List<Map<String, Object>>) resp.getBody().get("rows");
        // 只应返回 processId=100 的记录
        assertThat(rows).isNotEmpty();
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.get("processId")).isEqualTo(100);
            assertThat(r.get("processName")).isEqualTo("印刷");
        });
    }

    // ══════════════════════════════════════════════
    // 5. 更新 processId
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("更新工作站时应可修改 processId 关联的工序")
    void shouldUpdateProcessId() {
        String code = "WS_UPD_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_md_workstation (workstation_id,factory_id,workstation_code,workstation_name,workshop_id,process_id,process_type,status,enable_flag,create_by,create_time) VALUES (?,1,?,?,200,100,'INTERNAL','IDLE','1','admin',NOW())",
                540, code, "更新测试工作站");

        Map<String, Object> b = body();
        b.put("workstationId", 540);
        b.put("workstationCode", code);
        b.put("workstationName", "更新测试工作站");
        b.put("workshopId", 200);
        b.put("processId", 200);
        b.put("enableFlag", "1");

        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workstation",
                HttpMethod.PUT, authRequest(b), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        Long dbProcessId = jdbcTemplate.queryForObject(
                "SELECT process_id FROM qxx_md_workstation WHERE workstation_id = 540", Long.class);
        assertThat(dbProcessId).isEqualTo(200L);
    }

    // ══════════════════════════════════════════════
    // 6. processId 为 null 时容错
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("processId 为 null 时新增和列表不应报错（LEFT JOIN 容错）")
    void shouldHandleNullProcessId() {
        String code = "WS_NULL_" + System.nanoTime();
        Map<String, Object> b = body();
        b.put("workstationCode", code);
        b.put("workstationName", "无工序工作站");
        b.put("workshopId", 200);
        b.put("status", "IDLE");
        b.put("enableFlag", "1");

        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/mes/md/workstation", authRequest(b), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);

        ResponseEntity<Map> listResp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/workstation/list?workstationCode=" + code,
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(listResp.getBody().get("code")).isEqualTo(200);
    }
}
