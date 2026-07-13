package com.ruoyi.web.controller.mes.pro;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import com.ruoyi.BaseIntegrationTest;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.domain.mes.wm.WmProductRecpt;
import com.ruoyi.system.domain.mes.wm.WmWarehouse;
import com.ruoyi.system.service.mes.pro.IProWorkorderDocService;
import com.ruoyi.system.service.mes.wm.IWmWarehouseService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;

/**
 * 产品入库单 feedback 级幂等 - 集成测试。
 *
 * 覆盖：
 * A) 中间工序审核不生成入库单 (onFeedbackAudited 判非末工序即 return)
 * B) 分批完工场景 — 末工序 40 → 生成入库单 A(40)；再 60 → 生成入库单 B(60)
 * C) 同一 feedbackId 二次审核 → 幂等不重复生成
 * D) 手动补录二次调用 → 差额 0 空返回，log 仍只有一条 NULL 行
 *
 * 前置：docker compose up -d redis，Testcontainers 启动 MySQL。
 *
 * @author qixiaoxia
 * @date 2026-07-13
 */
@DisplayName("产品入库单 feedback 级幂等集成测试")
class ProFeedbackReceiptIT extends BaseIntegrationTest {

    @Autowired private IProWorkorderDocService docService;
    @Autowired private IWmWarehouseService warehouseService;

    private static final Long TEST_FACTORY = 1L;
    private static final Long TEST_ROUTE = 900_000L;
    private static final Long PROC_MID = 900_001L;
    private static final Long PROC_LAST = 900_002L;
    private Long workorderId;

    @BeforeAll
    void setupAuth() {
        // Service 层需要 SecurityContext 提供用户名/factoryId
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        user.setFactoryId(1L);
        LoginUser loginUser = new LoginUser(user, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterAll
    void clearAuth() {
        // 【Fix Finding #8】@BeforeAll 设 auth 后必须清理，防止跨测试类线程复用时泄漏 admin 身份
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void seedData() {
        // 清理相关表 — 用 WO-IT- 编码前缀匹配 (工单 ID 是自增, 无法用 id 范围过滤)
        jdbcTemplate.execute(
                "DELETE FROM qxx_pro_doc_generation_log WHERE workorder_id IN "
                + "(SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code LIKE 'WO-IT-%')");
        jdbcTemplate.execute(
                "DELETE FROM qxx_wm_product_recpt_line WHERE recpt_id IN "
                + "(SELECT recpt_id FROM qxx_wm_product_recpt WHERE workorder_code LIKE 'WO-IT-%')");
        jdbcTemplate.execute("DELETE FROM qxx_wm_product_recpt WHERE workorder_code LIKE 'WO-IT-%'");
        jdbcTemplate.execute("DELETE FROM qxx_pro_feedback WHERE workorder_id IN "
                + "(SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code LIKE 'WO-IT-%')");
        jdbcTemplate.execute("DELETE FROM qxx_pro_workorder WHERE workorder_code LIKE 'WO-IT-%'");
        jdbcTemplate.execute("DELETE FROM qxx_pro_route_process WHERE route_id = " + TEST_ROUTE);

        // 兜底建缺失表 (老 test schema 可能没有)
        try { jdbcTemplate.execute("SELECT 1 FROM qxx_wm_product_recpt LIMIT 1"); }
        catch (Exception ex) {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_wm_product_recpt ("
                    + "recpt_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1,"
                    + "recpt_code varchar(64) NOT NULL, recpt_name varchar(255) DEFAULT NULL,"
                    + "produce_id bigint(20) DEFAULT NULL, produce_code varchar(64) DEFAULT NULL,"
                    + "workorder_id bigint(20) DEFAULT NULL, workorder_code varchar(64) DEFAULT NULL,"
                    + "warehouse_id bigint(20) DEFAULT NULL, recpt_date datetime DEFAULT CURRENT_TIMESTAMP,"
                    + "total_quantity decimal(14,4) DEFAULT 0, total_box int DEFAULT 0,"
                    + "source_warehouse_id bigint(20) DEFAULT NULL, warehouse_code varchar(64) DEFAULT NULL, warehouse_name varchar(255) DEFAULT NULL,"
                    + "ipqc_id bigint(20) DEFAULT NULL, ipqc_code varchar(64) DEFAULT NULL,"
                    + "status varchar(50) DEFAULT 'DRAFT', remark varchar(500) DEFAULT '',"
                    + "create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                    + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (recpt_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS qxx_wm_product_recpt_line ("
                    + "line_id bigint(20) NOT NULL AUTO_INCREMENT, factory_id bigint(20) NOT NULL DEFAULT 1,"
                    + "recpt_id bigint(20) NOT NULL, item_id bigint(20) NOT NULL,"
                    + "item_code varchar(64) NOT NULL, item_name varchar(255) NOT NULL,"
                    + "specification varchar(500) DEFAULT NULL,"
                    + "unit_of_measure varchar(32) NOT NULL, unit_name varchar(64) NOT NULL,"
                    + "quantity_recpt decimal(14,4) DEFAULT 0, quantity_box int DEFAULT 0,"
                    + "batch_id bigint(20) DEFAULT NULL, batch_code varchar(64) DEFAULT NULL,"
                    + "warehouse_id bigint(20) DEFAULT NULL, location_id bigint(20) DEFAULT NULL, area_id bigint(20) DEFAULT NULL,"
                    + "remark varchar(500) DEFAULT '', create_by varchar(64) DEFAULT '', create_time datetime DEFAULT CURRENT_TIMESTAMP,"
                    + "update_by varchar(64) DEFAULT '', update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (line_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }

        // 补齐 doc_log 表新字段+索引 (V65 已跑，这里兜底以防某些 testcontainer 场景)
        // manual_tables.sql 已建 qxx_pro_doc_generation_log 基础表，此处只补新字段/索引
        try { jdbcTemplate.execute("ALTER TABLE qxx_pro_doc_generation_log ADD COLUMN source_feedback_id BIGINT DEFAULT NULL AFTER doc_code"); }
        catch (Exception ignored) {}
        try { jdbcTemplate.execute("CREATE UNIQUE INDEX uk_doc_log_wo_type_feedback ON qxx_pro_doc_generation_log (workorder_id, doc_type, source_feedback_id)"); }
        catch (Exception ignored) {}
        // 清理旧的 null_marker + 索引 B (若前一次跑测遗留)
        try { jdbcTemplate.execute("ALTER TABLE qxx_pro_doc_generation_log DROP INDEX uk_doc_log_wo_type_no_feedback"); }
        catch (Exception ignored) {}
        try { jdbcTemplate.execute("ALTER TABLE qxx_pro_doc_generation_log DROP COLUMN null_marker"); }
        catch (Exception ignored) {}

        // 确保有仓库 (Service 内部会自动选默认仓库)
        List<WmWarehouse> whs = warehouseService.selectWmWarehouseAll();
        if (whs == null || whs.isEmpty()) {
            jdbcTemplate.execute("INSERT INTO qxx_wm_warehouse (factory_id, warehouse_code, warehouse_name, enable_flag, create_by, create_time) VALUES (1, 'WH-IT', '测试仓', '1', 'admin', NOW())");
        }

        // 建工单 (用 jdbc 直插，绕开 FactoryIdInterceptor 需要 SecurityContext 的限制)
        String woCode = "WO-IT-" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_workorder (factory_id, workorder_code, workorder_name, order_source, "
                + "product_id, product_code, product_name, unit_of_measure, unit_name, "
                + "quantity, quantity_produced, status, create_by, create_time) "
                + "VALUES (?, ?, ?, 'MANUAL', 1, 'PROD-IT', '测试产品', 'PCS', '个', 100, 0, 'PRODUCING', 'admin', NOW())",
                TEST_FACTORY, woCode, "集成测试工单");
        workorderId = jdbcTemplate.queryForObject(
                "SELECT workorder_id FROM qxx_pro_workorder WHERE workorder_code = ?", Long.class, woCode);
        assertThat(workorderId).isNotNull();

        // 路线工序 (2 道：中间 + 末)
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, order_num, create_by, create_time) VALUES (?, ?, ?, ?, ?, ?, 'admin', NOW())",
                TEST_FACTORY, TEST_ROUTE, PROC_MID, "P-MID", "中间工序", 1);
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, order_num, create_by, create_time) VALUES (?, ?, ?, ?, ?, ?, 'admin', NOW())",
                TEST_FACTORY, TEST_ROUTE, PROC_LAST, "P-LAST", "末工序", 2);
    }

    // ═══════════════════════════════════════
    // A) 中间工序审核 → 不生成入库单
    // ═══════════════════════════════════════
    @Test
    @DisplayName("中间工序 feedback 审核 → onFeedbackAudited 判非末工序 return，无入库单")
    void midProcessAudit_shouldNotGenerateReceipt() {
        Long midFeedbackId = insertFeedback(PROC_MID, new BigDecimal("100"));

        docService.onFeedbackAudited(midFeedbackId);

        assertThat(countRecpts(workorderId)).isZero();
        assertThat(countRecptLogs(workorderId)).isZero();
    }

    // ═══════════════════════════════════════
    // B) 分批完工 — 末工序 40 → 60 生成两张入库单
    // ═══════════════════════════════════════
    @Test
    @DisplayName("末工序分批报工 40 + 60 → 生成两张独立入库单，总量 100")
    void lastProcessBatched_shouldGenerateTwoReceipts() {
        // 第一批：40
        Long fb1 = insertFeedback(PROC_LAST, new BigDecimal("40"));
        docService.onFeedbackAudited(fb1);

        assertThat(countRecpts(workorderId)).isEqualTo(1);
        assertThat(sumRecptQty(workorderId)).isEqualByComparingTo(new BigDecimal("40.0000"));

        // 第二批：60
        Long fb2 = insertFeedback(PROC_LAST, new BigDecimal("60"));
        docService.onFeedbackAudited(fb2);

        assertThat(countRecpts(workorderId)).isEqualTo(2);
        assertThat(sumRecptQty(workorderId)).isEqualByComparingTo(new BigDecimal("100.0000"));
        // 应有两条 RECPT log，source_feedback_id 分别指向 fb1/fb2
        assertThat(countRecptLogs(workorderId)).isEqualTo(2);
        // 验证每张单量正确 (第一张 40、第二张 60) — 防退化为 (50, 50) 之类的假绿
        List<BigDecimal> qtys = jdbcTemplate.queryForList(
                "SELECT total_quantity FROM qxx_wm_product_recpt WHERE workorder_id = ? ORDER BY recpt_id",
                BigDecimal.class, workorderId);
        assertThat(qtys).hasSize(2);
        assertThat(qtys.get(0)).isEqualByComparingTo(new BigDecimal("40.0000"));
        assertThat(qtys.get(1)).isEqualByComparingTo(new BigDecimal("60.0000"));
    }

    // ═══════════════════════════════════════
    // C) 同一 feedbackId 二次审核 → 幂等
    // ═══════════════════════════════════════
    @Test
    @DisplayName("同一 feedbackId 二次调 onFeedbackAudited → 不重复生成")
    void sameFeedbackTwice_shouldBeIdempotent() {
        Long fb = insertFeedback(PROC_LAST, new BigDecimal("100"));

        docService.onFeedbackAudited(fb);
        docService.onFeedbackAudited(fb);

        assertThat(countRecpts(workorderId)).isEqualTo(1);
        assertThat(sumRecptQty(workorderId)).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    // ═══════════════════════════════════════
    // D) 手动补录二次调用 → 差额 0 空返回
    // ═══════════════════════════════════════
    @Test
    @DisplayName("手动补录二次调 generateProductReceipt → 返回既有单，不重复生成")
    void manualGenerateTwice_shouldReturnExistingReceipt() {
        // 模拟已生产 100 (工单 quantity_produced 也置 100)
        jdbcTemplate.update("UPDATE qxx_pro_workorder SET quantity_produced = 100 WHERE workorder_id = ?", workorderId);

        WmProductRecpt r1 = docService.generateProductReceipt(workorderId);
        assertThat(r1).isNotNull();
        assertThat(r1.getTotalQuantity()).isEqualByComparingTo(new BigDecimal("100.0000"));

        // 第二次：hasReceiptLogForFeedback(wid, null) 命中已有 log → 走 buildExistingRecptInfo
        // → 返回既有单，不重复生成
        WmProductRecpt r2 = docService.generateProductReceipt(workorderId);
        assertThat(r2).isNotNull();
        assertThat(r2.getRecptId()).isEqualTo(r1.getRecptId());  // 是同一张单
        assertThat(countRecpts(workorderId)).isEqualTo(1);
        assertThat(countRecptLogs(workorderId)).isEqualTo(1);
    }

    // ---- helpers ----
    private Long insertFeedback(Long processId, BigDecimal qualified) {
        String code = "FB-" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO qxx_pro_feedback (factory_id, feedback_type, feedback_code, workorder_id, "
                + "route_id, process_id, item_id, item_code, item_name, workstation_id, status, "
                + "quantity_feedback, quantity_qualified, create_by, create_time) "
                + "VALUES (?, 'INTERNAL', ?, ?, ?, ?, 1, 'PROD-IT', '测试产品', 1, 'AUDITED', ?, ?, 'admin', NOW())",
                TEST_FACTORY, code, workorderId, TEST_ROUTE, processId, qualified, qualified);
        return jdbcTemplate.queryForObject(
                "SELECT record_id FROM qxx_pro_feedback WHERE feedback_code = ?", Long.class, code);
    }

    private int countRecpts(Long wid) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_wm_product_recpt WHERE workorder_id = ?",
                Integer.class, wid);
        return c != null ? c : 0;
    }

    private BigDecimal sumRecptQty(Long wid) {
        BigDecimal s = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_quantity), 0) FROM qxx_wm_product_recpt WHERE workorder_id = ?",
                BigDecimal.class, wid);
        return s != null ? s : BigDecimal.ZERO;
    }

    private int countRecptLogs(Long wid) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM qxx_pro_doc_generation_log WHERE workorder_id = ? AND doc_type = 'RECPT'",
                Integer.class, wid);
        return c != null ? c : 0;
    }
}
