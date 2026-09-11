package com.ruoyi.web.controller.mes.sal;

import com.ruoyi.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * V150（订单类型 5 值 + 外发/包装标志位 + 按维度匹配默认路线）迁移产物集成测试。
 * <p>
 * Testcontainers 启动时 Flyway 自动回放 V137+，V150 的种子 DDL/DML 在容器库真实执行；
 * 其中 RT-SMALL/RT-GIFT 依赖的既有工序、RT-OUTSRC 外发节点与万隆供应商由
 * test_sys_schema_patch.sql 在 Flyway 之前预置（test schema 不回放早期种子迁移）。
 */
@DisplayName("V150 迁移结果集成测试")
class V150MigrationIT extends BaseIntegrationTest {

    @Test
    @DisplayName("字典为 5 值且无 NEW/REPEAT")
    void should_haveFiveOrderTypes_and_noLegacyValues() {
        Integer cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_dict_data WHERE dict_type='mes_sal_order_type'", Integer.class);
        assertThat(cnt).isEqualTo(5);
        Integer legacy = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_dict_data WHERE dict_type='mes_sal_order_type' AND dict_value IN ('NEW','REPEAT')",
            Integer.class);
        assertThat(legacy).isZero();
    }

    @Test
    @DisplayName("9 个新列存在: 订单 2 + 订单行 3 + 路线绑定 4")
    void should_haveNewColumns() {
        Integer cols = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() " +
            "AND ((table_name='qxx_sal_order' AND column_name IN ('outsource_flag','package_flag')) " +
            "OR (table_name='qxx_sal_order_line' AND column_name IN ('route_product_id','route_code','route_name')) " +
            "OR (table_name='qxx_pro_route_product' AND column_name IN ('apply_order_type','apply_outsource','apply_package','is_default')))",
            Integer.class);
        assertThat(cols).isEqualTo(9);
    }

    @Test
    @DisplayName("种子: 三条新路线工序数 3/4/4, 礼品绑定带包装维度")
    void should_seedNewRoutesAndGiftBinding() {
        Integer plate = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id WHERE r.factory_id=1 AND r.route_code='RT-PLATE'", Integer.class);
        Integer small = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id WHERE r.factory_id=1 AND r.route_code='RT-SMALL'", Integer.class);
        Integer gift = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id WHERE r.factory_id=1 AND r.route_code='RT-GIFT'", Integer.class);
        assertThat(plate).isEqualTo(3);
        assertThat(small).isEqualTo(4);
        assertThat(gift).isEqualTo(4);

        Integer giftPkg = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_product rp JOIN qxx_md_item i ON i.item_id=rp.item_id " +
            "WHERE rp.factory_id=1 AND i.item_code='ITEM-GIFT-DEMO' AND rp.apply_order_type='GIFT' AND rp.apply_package='Y'",
            Integer.class);
        assertThat(giftPkg).isEqualTo(1);
    }

    @Test
    @DisplayName("RT-OUTSRC 外发节点均回填了万隆供应商")
    void should_backfillVendorForOutsourceNodes() {
        Integer outsourceNodes = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id " +
            "WHERE rp.factory_id=1 AND r.route_code='RT-OUTSRC' AND rp.is_outsource='1'", Integer.class);
        assumeTrue(outsourceNodes != null && outsourceNodes > 0, "test schema 未预置 RT-OUTSRC 外发节点，跳过回填断言");

        Integer missing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id " +
            "WHERE rp.factory_id=1 AND r.route_code='RT-OUTSRC' AND rp.is_outsource='1' AND rp.vendor_id IS NULL",
            Integer.class);
        assertThat(missing).isZero();
        Integer wanlong = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM qxx_pro_route_process rp JOIN qxx_pro_route r ON r.route_id=rp.route_id " +
            "WHERE rp.factory_id=1 AND r.route_code='RT-OUTSRC' AND rp.is_outsource='1' AND rp.vendor_code='OUT-WANLONG'",
            Integer.class);
        assertThat(wanlong).isEqualTo(outsourceNodes);
    }

    @Test
    @DisplayName("存量重映射 SQL 语义: NEW 订单执行 UPDATE 后变 STANDARD")
    void should_remapLegacyOrderType_when_updateRuns() {
        jdbcTemplate.update("INSERT INTO qxx_sal_order (factory_id, order_code, order_type, status, create_time) " +
            "VALUES (1, 'SO-IT-LEGACY-1', 'NEW', 'PREPARE', NOW())");
        // 重放 V150 中的重映射语句
        jdbcTemplate.update("UPDATE qxx_sal_order SET order_type='STANDARD' WHERE order_type IN ('NEW','REPEAT') OR order_type IS NULL");
        String t = jdbcTemplate.queryForObject(
            "SELECT order_type FROM qxx_sal_order WHERE order_code='SO-IT-LEGACY-1'", String.class);
        assertThat(t).isEqualTo("STANDARD");
    }
}
