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
 * listAll 端点 enableFlag 过滤集成测试。
 * <p>
 * 验证新增/修复的 listAll 端点只返回 enable_flag='1' 的记录。
 * 覆盖：MdItem、MdWorkstation、ProRoute、TmTool、WmWarehouse、WmStorageLocation、ProProcess。
 *
 * @author qixiaoxia
 */
@DisplayName("listAll 端点 enableFlag 过滤集成测试")
class ListAllFilterIT extends BaseIntegrationTest {

    @BeforeEach
    void setUp() {
        // 清理测试数据（按外键安全顺序，子表在前）
        truncateTables(
            "qxx_wm_storage_location", "qxx_wm_warehouse",
            "qxx_md_workstation", "qxx_md_workshop",
            "qxx_md_item", "qxx_md_item_type",
            "qxx_pro_process", "qxx_pro_route",
            "qxx_tm_tool", "qxx_tm_tool_type"
        );
    }

    // ═══════════════ WmWarehouse ═══════════════

    @Test
    @DisplayName("仓库 listAll — 只返回启用记录")
    void warehouseListAll_onlyEnabled() {
        // 插入启用仓库
        jdbcTemplate.update("INSERT INTO qxx_wm_warehouse (warehouse_id, warehouse_code, warehouse_name, enable_flag, factory_id) VALUES (90001, 'WH-ENABLED', '启用的仓库', '1', 1)");
        // 插入禁用仓库
        jdbcTemplate.update("INSERT INTO qxx_wm_warehouse (warehouse_id, warehouse_code, warehouse_name, enable_flag, factory_id) VALUES (90002, 'WH-DISABLED', '禁用的仓库', '0', 1)");

        // 直接验证 DB 过滤
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM qxx_wm_warehouse WHERE enable_flag = '1' AND warehouse_id IN (90001, 90002)", Integer.class);
        assertThat(count).isEqualTo(1);

        ResponseEntity<Map> resp = restTemplate.exchange(
            "http://localhost:" + port + "/mes/wm/warehouse/listAll",
            HttpMethod.GET, authRequest(), Map.class);
        Map<String, Object> body = resp.getBody();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");

        assertThat(data).isNotEmpty();
        assertThat(data).noneMatch(row -> "WH-DISABLED".equals(row.get("warehouseCode")));
    }

    // ═══════════════ WmStorageLocation ═══════════════

    @Test
    @DisplayName("库区 listAll — 只返回启用记录")
    void storageLocationListAll_onlyEnabled() {
        jdbcTemplate.update("INSERT INTO qxx_wm_warehouse (warehouse_id, warehouse_code, warehouse_name, enable_flag, factory_id) VALUES (90003, 'WH-LOC', '库区测试仓库', '1', 1)");
        jdbcTemplate.update("INSERT INTO qxx_wm_storage_location (location_id, warehouse_id, location_code, location_name, enable_flag, factory_id) VALUES (90001, 90003, 'LOC-ENABLED', '启用库区', '1', 1)");
        jdbcTemplate.update("INSERT INTO qxx_wm_storage_location (location_id, warehouse_id, location_code, location_name, enable_flag, factory_id) VALUES (90002, 90003, 'LOC-DISABLED', '禁用库区', '0', 1)");

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM qxx_wm_storage_location WHERE enable_flag = '1' AND location_id IN (90001, 90002)", Integer.class);
        assertThat(count).isEqualTo(1);

        ResponseEntity<Map> resp = restTemplate.exchange(
            "http://localhost:" + port + "/mes/wm/storage_location/listAll",
            HttpMethod.GET, authRequest(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getBody().get("data");

        assertThat(data).noneMatch(row -> "LOC-DISABLED".equals(row.get("locationCode")));
    }

    // ═══════════════ ProProcess ═══════════════

    @Test
    @DisplayName("工序 listAll — 只返回启用记录")
    void processListAll_onlyEnabled() {
        jdbcTemplate.update("INSERT INTO qxx_pro_process (process_id, process_code, process_name, enable_flag, factory_id) VALUES (90001, 'PROC-ENABLED', '启用的工序', '1', 1)");
        jdbcTemplate.update("INSERT INTO qxx_pro_process (process_id, process_code, process_name, enable_flag, factory_id) VALUES (90002, 'PROC-DISABLED', '禁用的工序', '0', 1)");

        ResponseEntity<Map> resp = restTemplate.exchange(
            "http://localhost:" + port + "/mes/pro/process/listAll",
            HttpMethod.GET, authRequest(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getBody().get("data");

        assertThat(data).noneMatch(row -> "PROC-DISABLED".equals(row.get("processCode")));
    }

    // ═══════════════ MdItem（新增 listAll） ═══════════════

    @Test
    @DisplayName("物料 listAll — 只返回启用记录")
    void itemListAll_onlyEnabled() {
        jdbcTemplate.update("INSERT INTO qxx_md_item_type (item_type_id, parent_type_id, item_type_code, item_type_name, enable_flag, factory_id, item_or_product) VALUES (90001, 0, 'TYPE01', '测试分类', '1', 1, 'ITEM')");
        jdbcTemplate.update("INSERT INTO qxx_md_item (item_id, item_code, item_name, item_type_id, enable_flag, factory_id, unit_of_measure, unit_name) VALUES (90001, 'ITEM-ENABLED', '启用的物料', 90001, '1', 1, 'PCS', '个')");
        jdbcTemplate.update("INSERT INTO qxx_md_item (item_id, item_code, item_name, item_type_id, enable_flag, factory_id, unit_of_measure, unit_name) VALUES (90002, 'ITEM-DISABLED', '禁用的物料', 90001, '0', 1, 'PCS', '个')");

        ResponseEntity<Map> resp = restTemplate.exchange(
            "http://localhost:" + port + "/mes/md/item/listAll",
            HttpMethod.GET, authRequest(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getBody().get("data");

        assertThat(data).noneMatch(row -> "ITEM-DISABLED".equals(row.get("itemCode")));
    }

    // ═══════════════ MdWorkstation（新增 listAll） ═══════════════

    @Test
    @DisplayName("工作站 listAll — 只返回启用记录")
    void workstationListAll_onlyEnabled() {
        jdbcTemplate.update("INSERT INTO qxx_md_workshop (workshop_id, workshop_code, workshop_name, enable_flag, factory_id) VALUES (90001, 'WSHOP-01', '测试车间', '1', 1)");
        jdbcTemplate.update("INSERT INTO qxx_md_workstation (workstation_id, workstation_code, workstation_name, workshop_id, enable_flag, factory_id) VALUES (90001, 'WST-ENABLED', '启用的工作站', 90001, '1', 1)");
        jdbcTemplate.update("INSERT INTO qxx_md_workstation (workstation_id, workstation_code, workstation_name, workshop_id, enable_flag, factory_id) VALUES (90002, 'WST-DISABLED', '禁用的工作站', 90001, '0', 1)");

        ResponseEntity<Map> resp = restTemplate.exchange(
            "http://localhost:" + port + "/mes/md/workstation/listAll",
            HttpMethod.GET, authRequest(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getBody().get("data");

        assertThat(data).noneMatch(row -> "WST-DISABLED".equals(row.get("workstationCode")));
    }

    // ═══════════════ ProRoute（新增 listAll） ═══════════════

    @Test
    @DisplayName("工艺路线 listAll — 只返回启用记录")
    void routeListAll_onlyEnabled() {
        jdbcTemplate.update("INSERT INTO qxx_pro_route (route_id, route_code, route_name, enable_flag, factory_id) VALUES (90001, 'ROUTE-ENABLED', '启用的路线', '1', 1)");
        jdbcTemplate.update("INSERT INTO qxx_pro_route (route_id, route_code, route_name, enable_flag, factory_id) VALUES (90002, 'ROUTE-DISABLED', '禁用的路线', '0', 1)");

        ResponseEntity<Map> resp = restTemplate.exchange(
            "http://localhost:" + port + "/mes/pro/proroute/listAll",
            HttpMethod.GET, authRequest(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getBody().get("data");

        assertThat(data).noneMatch(row -> "ROUTE-DISABLED".equals(row.get("routeCode")));
    }

    // ═══════════════ TmTool（新增 listAll） ═══════════════

    @Test
    @DisplayName("工装夹具 listAll — 只返回启用记录")
    void toolListAll_onlyEnabled() {
        jdbcTemplate.update("INSERT INTO qxx_tm_tool_type (tool_type_id, tool_type_code, tool_type_name, enable_flag, factory_id) VALUES (90001, 'TTYPE01', '测试夹具类型', '1', 1)");
        jdbcTemplate.update("INSERT INTO qxx_tm_tool (tool_id, tool_code, tool_name, tool_type_id, enable_flag, factory_id) VALUES (90001, 'TOOL-ENABLED', '启用的夹具', 90001, '1', 1)");
        jdbcTemplate.update("INSERT INTO qxx_tm_tool (tool_id, tool_code, tool_name, tool_type_id, enable_flag, factory_id) VALUES (90002, 'TOOL-DISABLED', '禁用的夹具', 90001, '0', 1)");

        ResponseEntity<Map> resp = restTemplate.exchange(
            "http://localhost:" + port + "/mes/tm/tool/listAll",
            HttpMethod.GET, authRequest(), Map.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getBody().get("data");

        assertThat(data).noneMatch(row -> "TOOL-DISABLED".equals(row.get("toolCode")));
    }

    // ═══════════════ 数据准备 ═══════════════

    private void ensureWarehouseData() {
        // 确保至少有仓库数据（依赖 qxx_md_factory）
    }

    private void ensureStorageLocationData() {
        // 确保至少有库区数据
    }
}
