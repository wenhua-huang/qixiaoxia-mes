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
 * 物料产品分类 Controller 集成测试
 */
@DisplayName("物料产品分类 Controller 集成测试")
class MdItemTypeControllerIT extends BaseIntegrationTest {

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
                CREATE TABLE IF NOT EXISTS qxx_md_item_type (
                    item_type_id bigint(20) NOT NULL AUTO_INCREMENT,
                    factory_id bigint(20) NOT NULL, item_type_code varchar(64) NOT NULL,
                    item_type_name varchar(255) NOT NULL, parent_type_id bigint(20) DEFAULT 0,
                    item_or_product varchar(20) NOT NULL,
                    order_num int(11) DEFAULT 1, enable_flag char(1) DEFAULT '1',
                    remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '',
                    create_time datetime DEFAULT CURRENT_TIMESTAMP,
                    update_by varchar(64) DEFAULT '',
                    update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    KEY idx_factory_id (factory_id), PRIMARY KEY (item_type_id))
                    ENGINE=InnoDB CHARSET=utf8mb4""");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_factory (factory_id,factory_code,factory_name,enable_flag,create_by,create_time) VALUES (1,'SX','圣享工厂','1','admin',NOW())");
            jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_item_type (item_type_id,factory_id,item_type_code,item_type_name,parent_type_id,item_or_product,order_num,enable_flag,create_by,create_time) VALUES (200,1,'RAW','原料',0,'RAW',1,'1','admin',NOW())");
        }
        jdbcTemplate.execute("DELETE FROM qxx_md_item_type WHERE item_type_id > 200");
    }

    @Test
    @DisplayName("list 返回分类列表")
    void shouldListTypes() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/itemtype/list",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        assertThat((Integer) resp.getBody().get("total")).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("treeselect 返回树形数据")
    void shouldReturnTreeSelect() {
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/itemtype/treeselect",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        List<Map> data = (List<Map>) resp.getBody().get("data");
        assertThat(data).isNotEmpty();
    }

    @Test
    @DisplayName("新增分类")
    void shouldInsertType() {
        String code = "IT_" + System.nanoTime();
        Map<String, Object> body = Map.of("itemTypeCode", code, "itemTypeName", "测试分类",
                "parentTypeId", 0, "itemOrProduct", "RAW", "enableFlag", "1");
        ResponseEntity<Map> resp = restTemplate.postForEntity(
                "http://localhost:" + port + "/mes/md/itemtype", authRequest(body), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("不能将父类型设为自己")
    void shouldRejectSelfParent() {
        jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_item_type (item_type_id,factory_id,item_type_code,item_type_name,parent_type_id,item_or_product,enable_flag,create_by,create_time) VALUES (300,1,'IT_SELF','自身','0','RAW','1','admin',NOW())");
        Map<String, Object> body = Map.of("itemTypeId", 300, "itemTypeCode", "IT_SELF", "itemTypeName", "自身",
                "parentTypeId", 300, "itemOrProduct", "RAW", "enableFlag", "1");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/itemtype",
                HttpMethod.PUT, authRequest(body), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(500);
    }

    @Test
    @DisplayName("排除子孙节点查询")
    void shouldExcludeDescendants() {
        jdbcTemplate.execute("INSERT IGNORE INTO qxx_md_item_type (item_type_id,factory_id,item_type_code,item_type_name,parent_type_id,item_or_product,enable_flag,create_by,create_time) VALUES (301,1,'IT_CHILD','子节点',200,'RAW','1','admin',NOW())");
        ResponseEntity<Map> resp = restTemplate.exchange(
                "http://localhost:" + port + "/mes/md/itemtype/list/exclude/200",
                HttpMethod.GET, authRequest(), Map.class);
        assertThat(resp.getBody().get("code")).isEqualTo(200);
        List<Map> data = (List<Map>) resp.getBody().get("data");
        // 不应包含200及其子节点301
        boolean has200 = data.stream().anyMatch(m -> m.get("itemTypeId").equals(200));
        boolean has301 = data.stream().anyMatch(m -> m.get("itemTypeId").equals(301));
        assertThat(has200).isFalse();
        assertThat(has301).isFalse();
    }
}
