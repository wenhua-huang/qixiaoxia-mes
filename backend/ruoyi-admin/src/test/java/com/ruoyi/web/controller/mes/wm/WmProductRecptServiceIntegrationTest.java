package com.ruoyi.web.controller.mes.wm;

import com.ruoyi.BaseIntegrationTest;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmProductRecptLine;
import com.ruoyi.system.service.mes.wm.IWmProductRecptLineService;
import com.ruoyi.system.service.mes.wm.IWmProductRecptService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * 产品入库单 Service 集成测试 - confirm/post 全链路（stock + transaction）。
 *
 * 覆盖：工单完工入库单 DRAFT -> CONFIRMED（库存增加 + 事务类型 PRODUCT_RECPT）-> POSTED。
 *
 * 注：依赖 Testcontainers MySQL + Flyway 迁移成功。
 *     若 Flyway migrate 失败（Testcontainers 干净 DB 缺 baseline 前手动建的表），
 *     本测试与所有 *IT 一样无法启动，属环境既有问题（见 memory integration-test-broken-by-flyway）。
 *
 * @author qixiaoxia
 * @date 2026-07-11
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.profiles.active=test,druid"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Sql(scripts = "classpath:sql/qxx_wm_core.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class WmProductRecptServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IWmProductRecptService productRecptService;

    @Autowired
    private IWmProductRecptLineService productRecptLineService;

    private static final Long ITEM_ID = 88801L;

    @BeforeAll
    void setupAuthAndWarehouses() {
        // 认证上下文（FactoryIdInterceptor 需要 SecurityUtils.getFactoryId()）
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setFactoryId(1L);
        LoginUser loginUser = new LoginUser(user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
        jdbcTemplate.update("INSERT INTO qxx_wm_warehouse (factory_id, warehouse_code, warehouse_name, enable_flag) " +
                "VALUES (1, 'WH-PR-IT', '产品入库测试仓', '1')");
    }

    @BeforeEach
    void cleanData() {
        truncateTables("qxx_wm_transaction", "qxx_wm_material_stock",
                "qxx_wm_product_recpt_line", "qxx_wm_product_recpt");
    }

    private Long whId() {
        return jdbcTemplate.queryForObject(
                "SELECT warehouse_id FROM qxx_wm_warehouse WHERE warehouse_code='WH-PR-IT'", Long.class);
    }

    private WmProductRecptLine newLine(Long recptId, Long wid, String code, BigDecimal qty) {
        WmProductRecptLine line = new WmProductRecptLine();
        line.setRecptId(recptId);
        line.setItemId(ITEM_ID);
        line.setItemCode(code);
        line.setItemName("测试产品");
        line.setUnitOfMeasure("PCS");
        line.setUnitName("个");
        line.setQuantityRecpt(qty);
        line.setWarehouseId(wid);
        return line;
    }

    @Test
    @DisplayName("产品入库单 confirm -> 库存增加 + 事务类型 PRODUCT_RECPT")
    void testConfirmProductRecpt() {
        Long wid = whId();
        WmProductRecpt recpt = new WmProductRecpt();
        recpt.setRecptCode("PR-IT-001");
        recpt.setRecptName("集成测试-产品入库");
        recpt.setWarehouseId(wid);
        recpt.setTotalQuantity(new BigDecimal("100"));
        recpt.setStatus("DRAFT");
        productRecptService.insertWmProductRecpt(recpt);

        productRecptLineService.insertWmProductRecptLine(newLine(recpt.getRecptId(), wid, "ITEM-PR-001", new BigDecimal("100")));

        productRecptService.confirmProductRecpt(recpt.getRecptId());

        // 状态 -> CONFIRMED
        WmProductRecpt updated = productRecptService.selectWmProductRecptByRecptId(recpt.getRecptId());
        assertThat(updated.getStatus()).isEqualTo("CONFIRMED");

        // 库存增加
        Integer stockCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_wm_material_stock WHERE item_id = ? AND warehouse_id = ?",
                Integer.class, ITEM_ID, wid);
        assertThat(stockCount).isEqualTo(1);

        // 事务类型 PRODUCT_RECPT + sourceDocType=wm_product_recpt
        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_wm_transaction WHERE transaction_type = 'PRODUCT_RECPT' " +
                "AND source_doc_type = 'wm_product_recpt' AND source_doc_id = ?",
                Integer.class, recpt.getRecptId());
        assertThat(txCount).isEqualTo(1);
    }

    @Test
    @DisplayName("产品入库单 post -> CONFIRMED -> POSTED")
    void testPostProductRecpt() {
        Long wid = whId();
        WmProductRecpt recpt = new WmProductRecpt();
        recpt.setRecptCode("PR-IT-002");
        recpt.setRecptName("集成测试-过账");
        recpt.setWarehouseId(wid);
        recpt.setTotalQuantity(new BigDecimal("50"));
        recpt.setStatus("CONFIRMED");
        productRecptService.insertWmProductRecpt(recpt);

        productRecptLineService.insertWmProductRecptLine(newLine(recpt.getRecptId(), wid, "ITEM-PR-002", new BigDecimal("50")));

        productRecptService.postProductRecpt(recpt.getRecptId());

        WmProductRecpt updated = productRecptService.selectWmProductRecptByRecptId(recpt.getRecptId());
        assertThat(updated.getStatus()).isEqualTo("POSTED");
    }

    @Test
    @DisplayName("confirm 幂等重试不重复加库存（storageCore selectExistingTransaction 去重）")
    void testConfirmIdempotent() {
        Long wid = whId();
        WmProductRecpt recpt = new WmProductRecpt();
        recpt.setRecptCode("PR-IT-003");
        recpt.setRecptName("集成测试-幂等");
        recpt.setWarehouseId(wid);
        recpt.setTotalQuantity(new BigDecimal("30"));
        recpt.setStatus("DRAFT");
        productRecptService.insertWmProductRecpt(recpt);
        productRecptLineService.insertWmProductRecptLine(newLine(recpt.getRecptId(), wid, "ITEM-PR-003", new BigDecimal("30")));

        productRecptService.confirmProductRecpt(recpt.getRecptId());
        // 第二次 confirm 会抛"仅草稿状态可确认收货"（状态机保护），不会重复加库存
        assertThatThrownBy(() -> productRecptService.confirmProductRecpt(recpt.getRecptId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("草稿状态");

        Integer txCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_wm_transaction WHERE source_doc_id = ?",
                Integer.class, recpt.getRecptId());
        assertThat(txCount).isEqualTo(1);
    }
}
