package com.ruoyi.web.controller.mes.qc;

import java.math.BigDecimal;
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
 * IQC 来料检验全流程集成测试 — 创建生成 / 确认拦截 / 判定 / 让步 / 免检。
 *
 * 链路：模板绑定物料 → 建入库单(POST /mes/wm/item_recpt，携带行) → 断言 IQC 待检单生成 →
 * 录入行实测值(edit) → 判定(judge) → 确认收货(confirm) 断言拦截或放行 + 库存增加 + 列表 qcStatus 汇总。
 *
 * @author qixiaoxia
 */
@DisplayName("IQC 来料检验全流程集成测试")
class QcIqcIT extends BaseIntegrationTest
{
    private static final long FACTORY_ID = 1L;
    /** 两检测项快照：#1 标准 100±1（合格 [99,101]）、#2 标准 200±2（合格 [198,202]） */
    private static final String VAL1_PASS = "100.0";
    private static final String VAL1_FAIL = "110.0";
    private static final String VAL2_PASS = "200.0";

    private String recptUrl() { return "http://localhost:" + port + "/mes/wm/item_recpt"; }

    private String iqcUrl() { return "http://localhost:" + port + "/mes/qc/iqc"; }

    @BeforeEach
    void clean()
    {
        truncateTables(
            "qxx_qc_order_line", "qxx_qc_defect_record", "qxx_qc_iqc",
            "qxx_qc_template_index", "qxx_qc_template_product", "qxx_qc_template",
            "qxx_wm_item_recpt_detail", "qxx_wm_item_recpt_line", "qxx_wm_item_recpt",
            "qxx_wm_transaction", "qxx_wm_material_stock", "qxx_pro_material_trace");
    }

    @Test
    @DisplayName("PENDING 待检单阻断确认：confirm 拒绝且单据保持草稿")
    void should_reject_confirm_when_iqc_pending()
    {
        long itemId = 9101L;
        setupIqcBinding(itemId, "TPL-IT-01");
        Long recptId = createRecpt("RCP-IT-01", itemId);

        // 创建即生成 IQC 待检单
        Long iqcId = iqcIdOf(recptId);
        assertThat(iqcId).isNotNull();
        Map<String, Object> iqc = jdbcTemplate.queryForMap(
            "select status, check_result from qxx_qc_iqc where iqc_id = ?", iqcId);
        assertThat(iqc.get("status")).isEqualTo("PENDING");
        assertThat(iqc.get("check_result")).isNull();

        // confirm 被拦截
        Map<?, ?> resp = put(recptUrl() + "/confirm/" + recptId, null);
        assertThat(resp.get("code")).isEqualTo(500);
        assertThat(resp.get("msg").toString()).contains("需来料检验");

        // 单据保持草稿，无库存事务
        assertThat(recptStatus(recptId)).isEqualTo("DRAFT");
        assertThat(txCount(recptId)).isEqualTo(0);
    }

    @Test
    @DisplayName("判定 PASS 后放行：行值合格 → judge=PASS → confirm 成功，库存增加，列表 qcStatus=PASSED")
    void should_pass_confirm_after_iqc_judged_pass()
    {
        long itemId = 9102L;
        setupIqcBinding(itemId, "TPL-IT-02");
        Long recptId = createRecpt("RCP-IT-02", itemId);
        Long iqcId = iqcIdOf(recptId);

        recordLineValues(iqcId, VAL1_PASS, VAL2_PASS);
        Map<?, ?> judgeResp = put(iqcUrl() + "/judge/" + iqcId, Map.of());
        assertThat(judgeResp.get("code")).isEqualTo(200);

        // 判定结果回写：PASS / 合格数=检测数-不合格数 / 检验员+检验日期
        Map<String, Object> judged = jdbcTemplate.queryForMap(
            "select status, check_result, quantity_qualified, quantity_unqualified, inspector, inspect_date "
            + "from qxx_qc_iqc where iqc_id = ?", iqcId);
        assertThat(judged.get("status")).isEqualTo("COMPLETED");
        assertThat(judged.get("check_result")).isEqualTo("PASS");
        assertThat(((Number) judged.get("quantity_qualified")).intValue()).isEqualTo(5);
        assertThat(((Number) judged.get("quantity_unqualified")).intValue()).isEqualTo(0);
        assertThat(judged.get("inspector")).isEqualTo("admin");
        assertThat(judged.get("inspect_date")).isNotNull();

        // 已判定单据：edit 拒绝（防篡改）+ close 拒绝
        Map<?, ?> editResp = put(iqcUrl(), Map.of("iqcId", iqcId, "quantityCheck", 99));
        assertThat(editResp.get("code")).isEqualTo(500);
        assertThat(editResp.get("msg").toString()).contains("不可编辑");
        Map<?, ?> closeResp = put(iqcUrl() + "/close/" + iqcId, null);
        assertThat(closeResp.get("code")).isEqualTo(500);
        assertThat(closeResp.get("msg").toString()).contains("不可关闭");

        // confirm 放行 + 库存增加 10
        Map<?, ?> confirmResp = put(recptUrl() + "/confirm/" + recptId, null);
        assertThat(confirmResp.get("code")).isEqualTo(200);
        assertThat(recptStatus(recptId)).isEqualTo("CONFIRMED");
        assertThat(stockQty(itemId)).isEqualByComparingTo("10");

        // 列表 qcStatus 汇总列 = PASSED
        assertThat(qcStatusOf("RCP-IT-02")).isEqualTo("PASSED");
    }

    @Test
    @DisplayName("判定 FAIL 后确认仍被拒：行值超差 → FAIL → confirm 拒绝，单据保持草稿")
    void should_reject_confirm_when_iqc_fail()
    {
        long itemId = 9103L;
        setupIqcBinding(itemId, "TPL-IT-03");
        Long recptId = createRecpt("RCP-IT-03", itemId);
        Long iqcId = iqcIdOf(recptId);

        recordLineValues(iqcId, VAL1_FAIL, VAL2_PASS);
        Map<?, ?> judgeResp = put(iqcUrl() + "/judge/" + iqcId, Map.of());
        assertThat(judgeResp.get("code")).isEqualTo(200);

        Map<String, Object> judged = jdbcTemplate.queryForMap(
            "select status, check_result, quantity_qualified from qxx_qc_iqc where iqc_id = ?", iqcId);
        assertThat(judged.get("status")).isEqualTo("COMPLETED");
        assertThat(judged.get("check_result")).isEqualTo("FAIL");
        assertThat(((Number) judged.get("quantity_qualified")).intValue()).isEqualTo(4);

        Map<?, ?> confirmResp = put(recptUrl() + "/confirm/" + recptId, null);
        assertThat(confirmResp.get("code")).isEqualTo(500);
        assertThat(confirmResp.get("msg").toString()).contains("需来料检验");
        assertThat(recptStatus(recptId)).isEqualTo("DRAFT");
        assertThat(txCount(recptId)).isEqualTo(0);
        assertThat(qcStatusOf("RCP-IT-03")).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("FAIL 带让步理由 → CONCESSION：confirm 放行，让步理由落库，列表 qcStatus=CONCESSION")
    void should_pass_confirm_when_concession()
    {
        long itemId = 9104L;
        setupIqcBinding(itemId, "TPL-IT-04");
        Long recptId = createRecpt("RCP-IT-04", itemId);
        Long iqcId = iqcIdOf(recptId);

        recordLineValues(iqcId, VAL1_FAIL, VAL2_PASS);
        Map<?, ?> judgeResp = put(iqcUrl() + "/judge/" + iqcId,
            Map.of("concessionReason", "轻微超差，生产同意让步接收"));
        assertThat(judgeResp.get("code")).isEqualTo(200);

        Map<String, Object> judged = jdbcTemplate.queryForMap(
            "select status, check_result, concession_reason from qxx_qc_iqc where iqc_id = ?", iqcId);
        assertThat(judged.get("status")).isEqualTo("COMPLETED");
        assertThat(judged.get("check_result")).isEqualTo("CONCESSION");
        assertThat(judged.get("concession_reason")).isEqualTo("轻微超差，生产同意让步接收");

        Map<?, ?> confirmResp = put(recptUrl() + "/confirm/" + recptId, null);
        assertThat(confirmResp.get("code")).isEqualTo(200);
        assertThat(recptStatus(recptId)).isEqualTo("CONFIRMED");
        assertThat(stockQty(itemId)).isEqualByComparingTo("10");
        assertThat(qcStatusOf("RCP-IT-04")).isEqualTo("CONCESSION");
    }

    @Test
    @DisplayName("未绑定模板=免检：不生成检验单，confirm 直接成功，列表 qcStatus=NONE")
    void should_skip_inspection_when_no_bind()
    {
        long itemId = 9105L;  // 无模板绑定
        Long recptId = createRecpt("RCP-IT-05", itemId);

        Integer iqcCount = jdbcTemplate.queryForObject(
            "select count(1) from qxx_qc_iqc where source_doc_type = 'wm_item_recpt' and source_doc_id = ?",
            Integer.class, recptId);
        assertThat(iqcCount).isZero();

        Map<?, ?> confirmResp = put(recptUrl() + "/confirm/" + recptId, null);
        assertThat(confirmResp.get("code")).isEqualTo(200);
        assertThat(recptStatus(recptId)).isEqualTo("CONFIRMED");
        assertThat(stockQty(itemId)).isEqualByComparingTo("10");
        assertThat(qcStatusOf("RCP-IT-05")).isEqualTo("NONE");
    }

    // ============ 前置数据与辅助 ============

    /** 建 模板(2 个数值检测项,标准 100±1 / 200±2) + 物料绑定(抽检 5, Ac=0, 三率阈值 0) */
    private void setupIqcBinding(long itemId, String templateCode)
    {
        jdbcTemplate.update(
            "insert into qxx_qc_template (factory_id, template_code, template_name, qc_types, enable_flag, create_by) "
            + "values (?, ?, ?, 'IQC', '1', 'admin')", FACTORY_ID, templateCode, templateCode + "-名称");
        Long templateId = jdbcTemplate.queryForObject(
            "select template_id from qxx_qc_template where template_code = ?", Long.class, templateCode);
        insertTemplateIndex(templateId, 1001L, "IDX-LEN", "长度", 100, -1, 1, 1);
        insertTemplateIndex(templateId, 1002L, "IDX-WID", "宽度", 200, -2, 2, 2);
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
            + "values (?, ?, ?, ?, ?, 'IQC', 'NUMBER', ?, ?, ?, ?, 'admin')",
            FACTORY_ID, templateId, indexId, code, name, stander, min, max, orderNum);
    }

    /** 建采购入库单（PC 创建路径，携带行）— 触发 IQC 生成 hook */
    private Long createRecpt(String recptCode, long itemId)
    {
        Map<String, Object> line = new HashMap<>();
        line.put("itemId", itemId);
        line.put("itemCode", itemCodeOf(itemId));
        line.put("itemName", itemCodeOf(itemId) + "-物料");
        line.put("unitOfMeasure", "PCS");
        line.put("unitName", "个");
        line.put("quantityRecpt", new BigDecimal("10"));
        line.put("batchId", 9000 + itemId % 100);
        line.put("batchCode", "IT-BATCH-" + itemId % 100);
        line.put("warehouseId", 1);
        line.put("warehouseCode", "WH-IT");
        line.put("warehouseName", "IT仓");

        Map<String, Object> header = new HashMap<>();
        header.put("recptCode", recptCode);
        header.put("recptName", "IQC集成测试-" + recptCode);
        header.put("recptType", "PURCHASE");
        header.put("vendorId", 1);
        header.put("vendorCode", "V-IT");
        header.put("vendorName", "IT供应商");
        header.put("warehouseId", 1);
        header.put("warehouseCode", "WH-IT");
        header.put("warehouseName", "IT仓");
        header.put("lines", List.of(line));

        Map<?, ?> resp = post(recptUrl(), header);
        assertThat(resp.get("code")).isEqualTo(200);
        return ((Number) ((Map<?, ?>) resp.get("data")).get("recptId")).longValue();
    }

    /** 取检验单详情 → 回填两行实测值 + 实际检测数 5 → edit 落库（检验员录值流程） */
    private void recordLineValues(Long iqcId, String val1, String val2)
    {
        Map<?, ?> detail = (Map<?, ?>) get(iqcUrl() + "/" + iqcId).get("data");
        List<Map<String, Object>> lines = (List<Map<String, Object>>) detail.get("lines");
        assertThat(lines).hasSize(2);
        lines.get(0).put("checkValText", val1);
        lines.get(1).put("checkValText", val2);

        Map<String, Object> editBody = new HashMap<>();
        editBody.put("iqcId", iqcId);
        editBody.put("quantityCheck", 5);
        editBody.put("lines", lines);
        Map<?, ?> editResp = put(iqcUrl(), editBody);
        assertThat(editResp.get("code")).isEqualTo(200);
    }

    private Long iqcIdOf(Long recptId)
    {
        List<Long> ids = jdbcTemplate.queryForList(
            "select iqc_id from qxx_qc_iqc where source_doc_type = 'wm_item_recpt' and source_doc_id = ?",
            Long.class, recptId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private String recptStatus(Long recptId)
    {
        return jdbcTemplate.queryForObject("select status from qxx_wm_item_recpt where recpt_id = ?", String.class, recptId);
    }

    private int txCount(Long recptId)
    {
        return jdbcTemplate.queryForObject(
            "select count(1) from qxx_wm_transaction where source_doc_type = 'wm_item_recpt' and source_doc_id = ?",
            Integer.class, recptId);
    }

    private BigDecimal stockQty(long itemId)
    {
        return jdbcTemplate.queryForObject(
            "select ifnull(sum(quantity_onhand), 0) from qxx_wm_material_stock where item_id = ?",
            BigDecimal.class, itemId);
    }

    /** 列表接口 qcStatus 汇总计算列 */
    private String qcStatusOf(String recptCode)
    {
        Map<?, ?> resp = get(recptUrl() + "/list?recptCode=" + recptCode);
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
