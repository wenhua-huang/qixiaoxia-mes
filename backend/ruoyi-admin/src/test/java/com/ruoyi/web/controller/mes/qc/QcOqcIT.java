package com.ruoyi.web.controller.mes.qc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.ruoyi.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OQC 出货检验全流程集成测试 — 创建生成 / 出库确认拦截 / 判定放行。
 *
 * 链路：模板绑定物料 → 建出库单(POST /mes/wm/product_sales，携带行) → 断言 OQC 待检单生成 →
 * 录入行实测值(edit) → 判定(judge) → 出库确认(post) 断言拦截或放行 + 库存扣减 + 列表 qcStatus 汇总。
 * 含多物料两张检验单全过才放行场景。
 *
 * @author qixiaoxia
 */
@DisplayName("OQC 出货检验全流程集成测试")
class QcOqcIT extends BaseIntegrationTest
{
    private static final long FACTORY_ID = 1L;
    private static final long WAREHOUSE_ID = 1L;
    /** 两检测项快照：#1 标准 100±1（合格 [99,101]）、#2 标准 200±2（合格 [198,202]） */
    private static final String VAL1_PASS = "100.0";
    private static final String VAL2_PASS = "200.0";

    private String salesUrl() { return "http://localhost:" + port + "/mes/wm/product_sales"; }

    private String oqcUrl() { return "http://localhost:" + port + "/mes/qc/oqc"; }

    @BeforeEach
    void clean()
    {
        truncateTables(
            "qxx_qc_order_line", "qxx_qc_defect_record", "qxx_qc_oqc",
            "qxx_qc_template_index", "qxx_qc_template_product", "qxx_qc_template",
            "qxx_wm_product_sales_detail", "qxx_wm_product_sales_line", "qxx_wm_product_sales",
            "qxx_wm_transaction", "qxx_wm_material_stock", "qxx_pro_material_trace");
    }

    @Test
    @DisplayName("创建即生成：出库单携行创建后生成 OQC 待检单（快照+挂点回填），列表 qcStatus=PENDING")
    void should_generate_oqc_on_sales_create()
    {
        long itemId = 9201L;
        setupOqcBinding(itemId, "TPL-OQC-IT-01");
        insertStock(itemId, 100);
        Long salesId = createSales("SAL-IT-01", itemId);

        // 创建即生成 OQC 待检单（一物料一单）
        List<Long> oqcIds = oqcIdsOf(salesId);
        assertThat(oqcIds).hasSize(1);
        Map<String, Object> oqc = jdbcTemplate.queryForMap(
            "select status, check_result, quantity_out, client_code, client_name, oqc_code "
            + "from qxx_qc_oqc where oqc_id = ?", oqcIds.get(0));
        assertThat(oqc.get("status")).isEqualTo("PENDING");
        assertThat(oqc.get("check_result")).isNull();
        assertThat(((Number) oqc.get("quantity_out")).doubleValue()).isEqualTo(10d);
        // 客户三件套从出库单头快照
        assertThat(oqc.get("client_code")).isEqualTo("C-IT");
        assertThat(oqc.get("client_name")).isEqualTo("IT客户");

        // 出库单头 OQC 挂点回填
        Map<String, Object> sales = jdbcTemplate.queryForMap(
            "select oqc_id, oqc_code from qxx_wm_product_sales where sales_id = ?", salesId);
        assertThat(((Number) sales.get("oqc_id")).longValue()).isEqualTo(oqcIds.get(0));
        assertThat(sales.get("oqc_code")).isEqualTo(oqc.get("oqc_code"));

        // 列表 qcStatus 汇总列 = PENDING
        assertThat(qcStatusOf("SAL-IT-01")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("PENDING 待检单阻断出库：post 拒绝且单据保持草稿、库存未扣、无库存事务")
    void should_reject_postOut_when_pending()
    {
        long itemId = 9202L;
        setupOqcBinding(itemId, "TPL-OQC-IT-02");
        insertStock(itemId, 100);
        Long salesId = createSales("SAL-IT-02", itemId);

        Map<?, ?> resp = put(salesUrl() + "/post/" + salesId, postDetails(salesId, itemId));
        assertThat(resp.get("code")).isEqualTo(500);
        assertThat(resp.get("msg").toString()).contains("需出货检验合格后方可出库确认");

        assertThat(salesStatus(salesId)).isEqualTo("DRAFT");
        assertThat(stockQty(itemId)).isEqualByComparingTo("100");
        assertThat(txCount(salesId)).isEqualTo(0);
    }

    @Test
    @DisplayName("判定后放行：多物料两张检验单全过才放行 → 全 PASS 后 post 成功扣库存，列表 qcStatus=PASSED")
    void should_pass_postOut_after_judged()
    {
        long itemA = 9203L;
        long itemB = 9204L;
        setupOqcBinding(itemA, "TPL-OQC-IT-03");
        setupOqcBinding(itemB, "TPL-OQC-IT-04");
        insertStock(itemA, 100);
        insertStock(itemB, 100);
        Long salesId = createSales("SAL-IT-03", itemA, itemB);

        // 两物料 → 两张 OQC 待检单
        List<Long> oqcIds = oqcIdsOf(salesId);
        assertThat(oqcIds).hasSize(2);

        // 仅判第一张 → 仍被拦截（另一物料未过检）
        judgePass(oqcIds.get(0));
        Map<?, ?> halfResp = put(salesUrl() + "/post/" + salesId, postDetails(salesId, itemA, itemB));
        assertThat(halfResp.get("code")).isEqualTo(500);
        assertThat(halfResp.get("msg").toString()).contains("需出货检验合格后方可出库确认");
        assertThat(stockQty(itemA)).isEqualByComparingTo("100");

        // 第二张也判定 → 放行，两物料库存各扣 10
        judgePass(oqcIds.get(1));
        Map<?, ?> resp = put(salesUrl() + "/post/" + salesId, postDetails(salesId, itemA, itemB));
        assertThat(resp.get("code")).isEqualTo(200);
        assertThat(salesStatus(salesId)).isEqualTo("POSTED");
        assertThat(stockQty(itemA)).isEqualByComparingTo("90");
        assertThat(stockQty(itemB)).isEqualByComparingTo("90");
        assertThat(qcStatusOf("SAL-IT-03")).isEqualTo("PASSED");
    }

    // ============ 前置数据与辅助 ============

    /** 建 模板(2 个数值检测项,标准 100±1 / 200±2) + 物料绑定(抽检 5, Ac=0, 三率阈值 0) */
    private void setupOqcBinding(long itemId, String templateCode)
    {
        jdbcTemplate.update(
            "insert into qxx_qc_template (factory_id, template_code, template_name, qc_types, enable_flag, create_by) "
            + "values (?, ?, ?, 'OQC', '1', 'admin')", FACTORY_ID, templateCode, templateCode + "-名称");
        Long templateId = jdbcTemplate.queryForObject(
            "select template_id from qxx_qc_template where template_code = ?", Long.class, templateCode);
        insertTemplateIndex(templateId, 2001L, "OQC-LEN", "长度", 100, -1, 1, 1);
        insertTemplateIndex(templateId, 2002L, "OQC-WID", "宽度", 200, -2, 2, 2);
        jdbcTemplate.update(
            "insert into qxx_qc_template_product (factory_id, template_id, item_id, item_code, item_name, "
            + "quantity_check, quantity_unqualified, cr_rate, maj_rate, min_rate, create_by) "
            + "values (?, ?, ?, ?, ?, 5, 0, 0, 0, 0, 'admin')",
            FACTORY_ID, templateId, itemId, itemCodeOf(itemId), itemCodeOf(itemId) + "-物料");
    }

    private void insertTemplateIndex(Long templateId, long indexId, String code, String name,
        double stander, double min, double max, int orderNum)
    {
        jdbcTemplate.update(
            "insert into qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, "
            + "index_type, qc_result_type, stander_val, threshold_min, threshold_max, order_num, create_by) "
            + "values (?, ?, ?, ?, ?, 'OQC', 'NUMBER', ?, ?, ?, ?, 'admin')",
            FACTORY_ID, templateId, indexId, code, name, stander, min, max, orderNum);
    }

    /** 直接建可用库存（vendor=0/workorder=0/NORMAL，与出库 FIFO 查询口径一致） */
    private void insertStock(long itemId, int qty)
    {
        jdbcTemplate.update(
            "insert into qxx_wm_material_stock (factory_id, item_id, item_code, item_name, unit_of_measure, "
            + "unit_name, quantity_onhand, quantity_available, batch_id, warehouse_id, vendor_id, workorder_id, "
            + "quality_status, create_by) values (?, ?, ?, ?, 'PCS', '个', ?, ?, 0, ?, 0, 0, 'NORMAL', 'admin')",
            FACTORY_ID, itemId, itemCodeOf(itemId), itemCodeOf(itemId) + "-物料", qty, qty, WAREHOUSE_ID);
    }

    /** 建销售出库单（PC 创建路径，携带行）— 触发 OQC 生成 hook */
    private Long createSales(String salesCode, long... itemIds)
    {
        List<Map<String, Object>> lines = new ArrayList<>();
        for (long itemId : itemIds)
        {
            Map<String, Object> line = new HashMap<>();
            line.put("itemId", itemId);
            line.put("itemCode", itemCodeOf(itemId));
            line.put("itemName", itemCodeOf(itemId) + "-物料");
            line.put("unitOfMeasure", "PCS");
            line.put("unitName", "个");
            line.put("quantitySales", new BigDecimal("10"));
            line.put("warehouseId", WAREHOUSE_ID);
            lines.add(line);
        }

        Map<String, Object> header = new HashMap<>();
        header.put("salesCode", salesCode);
        header.put("salesName", "OQC集成测试-" + salesCode);
        header.put("clientId", 7L);
        header.put("clientCode", "C-IT");
        header.put("clientName", "IT客户");
        header.put("warehouseId", WAREHOUSE_ID);
        header.put("lines", lines);

        Map<?, ?> resp = post(salesUrl(), header);
        assertThat(resp.get("code")).isEqualTo(200);
        return jdbcTemplate.queryForObject(
            "select sales_id from qxx_wm_product_sales where sales_code = ?", Long.class, salesCode);
    }

    /** 出库确认明细（行维度全量出库） */
    private List<Map<String, Object>> postDetails(Long salesId, long... itemIds)
    {
        List<Map<String, Object>> details = new ArrayList<>();
        for (long itemId : itemIds)
        {
            Long lineId = jdbcTemplate.queryForObject(
                "select line_id from qxx_wm_product_sales_line where sales_id = ? and item_id = ? limit 1",
                Long.class, salesId, itemId);
            Map<String, Object> d = new HashMap<>();
            d.put("lineId", lineId);
            d.put("itemId", itemId);
            d.put("itemCode", itemCodeOf(itemId));
            d.put("itemName", itemCodeOf(itemId) + "-物料");
            d.put("unitOfMeasure", "PCS");
            d.put("unitName", "个");
            d.put("quantity", new BigDecimal("10"));
            d.put("warehouseId", WAREHOUSE_ID);
            details.add(d);
        }
        return details;
    }

    /** 取检验单详情 → 回填两行合格实测值 + 实际检测数 5 → edit 落库 → judge → PASS */
    private void judgePass(Long oqcId)
    {
        Map<?, ?> detail = (Map<?, ?>) get(oqcUrl() + "/" + oqcId).get("data");
        List<Map<String, Object>> lines = (List<Map<String, Object>>) detail.get("lines");
        assertThat(lines).hasSize(2);
        lines.get(0).put("checkValText", VAL1_PASS);
        lines.get(1).put("checkValText", VAL2_PASS);

        Map<String, Object> editBody = new HashMap<>();
        editBody.put("oqcId", oqcId);
        editBody.put("quantityCheck", 5);
        editBody.put("lines", lines);
        Map<?, ?> editResp = put(oqcUrl(), editBody);
        assertThat(editResp.get("code")).isEqualTo(200);

        Map<?, ?> judgeResp = put(oqcUrl() + "/judge/" + oqcId, Map.of());
        assertThat(judgeResp.get("code")).isEqualTo(200);
        Map<String, Object> judged = jdbcTemplate.queryForMap(
            "select status, check_result from qxx_qc_oqc where oqc_id = ?", oqcId);
        assertThat(judged.get("status")).isEqualTo("COMPLETED");
        assertThat(judged.get("check_result")).isEqualTo("PASS");
    }

    private List<Long> oqcIdsOf(Long salesId)
    {
        return jdbcTemplate.queryForList(
            "select oqc_id from qxx_qc_oqc where source_doc_type = 'wm_product_sales' and source_doc_id = ? order by oqc_id",
            Long.class, salesId);
    }

    private String salesStatus(Long salesId)
    {
        return jdbcTemplate.queryForObject("select status from qxx_wm_product_sales where sales_id = ?", String.class, salesId);
    }

    private int txCount(Long salesId)
    {
        return jdbcTemplate.queryForObject(
            "select count(1) from qxx_wm_transaction where source_doc_type = 'SALES_OUT' and source_doc_id = ?",
            Integer.class, salesId);
    }

    private BigDecimal stockQty(long itemId)
    {
        return jdbcTemplate.queryForObject(
            "select ifnull(sum(quantity_onhand), 0) from qxx_wm_material_stock where item_id = ?",
            BigDecimal.class, itemId);
    }

    /** 列表接口 qcStatus 汇总计算列 */
    private String qcStatusOf(String salesCode)
    {
        Map<?, ?> resp = get(salesUrl() + "/list?salesCode=" + salesCode);
        List<?> rows = (List<?>) resp.get("rows");
        assertThat(rows).hasSize(1);
        return (String) ((Map<?, ?>) rows.get(0)).get("qcStatus");
    }

    private String itemCodeOf(long itemId)
    {
        return "IT-ITEM-" + itemId % 100;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> post(String url, Object body)
    {
        ResponseEntity<Map> resp = restTemplate.postForEntity(url, authRequest(body), Map.class);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> put(String url, Object body)
    {
        ResponseEntity<Map> resp = restTemplate.exchange(
            url, HttpMethod.PUT, body == null ? authRequest() : authRequest(body), Map.class);
        return resp.getBody();
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> get(String url)
    {
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, authRequest(), Map.class);
        return resp.getBody();
    }
}
