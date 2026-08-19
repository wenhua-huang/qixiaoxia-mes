package com.ruoyi.web.controller.mes.qc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.ruoyi.BaseIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IPQC 过程检验全流程集成测试 — 工序检（报工确认触发）/ 完工检（成品入库触发+确认拦截）/ 手工巡检。
 *
 * 链路 A 工序检：路线工序 isCheck=Y + 模板绑定物料 → 报工确认(confirm) → 断言 IPQC 待检单生成 +
 * card_process.ipqc_id 回填；非 isCheck 工序/重复确认不生成。
 * 链路 B 完工检：模板绑定产品 → 建入库单(POST /mes/wm/product_recpt 携带行) → 生成 LAST_CHECK 单 →
 * 确认入库被拦截 → 判定 PASS 后放行 + 库存增加 + 列表 qcStatus 汇总。
 * 链路 C 手工单：POST /mes/qc/ipqc 创建巡检单 → 录值判定 COMPLETED。
 *
 * @author qixiaoxia
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("IPQC 过程检验全流程集成测试")
class QcIpqcIT extends BaseIntegrationTest
{
    private static final long FACTORY_ID = 1L;
    /** 两检测项快照：#1 标准 100±1（合格 [99,101]）、#2 标准 200±2（合格 [198,202]） */
    private static final String VAL1_PASS = "100.0";
    private static final String VAL2_PASS = "200.0";

    /** 工序检链路固定 ID 段（与完工检/巡检链路隔离） */
    private static final long ROUTE_ID = 9300L;
    private static final long PROCESS_ID = 9301L;
    private static final long CARD_ID = 9310L;
    private static final long WO_IPQC_ITEM = 9301L;
    /** 完工检链路产品 */
    private static final long PRODUCT_ITEM = 9311L;

    private String ipqcUrl() { return "http://localhost:" + port + "/mes/qc/ipqc"; }

    private String feedbackUrl() { return "http://localhost:" + port + "/mes/pro/feedback"; }

    private String recptUrl() { return "http://localhost:" + port + "/mes/wm/product_recpt"; }

    @BeforeEach
    void clean()
    {
        truncateTables(
            "qxx_qc_order_line", "qxx_qc_defect_record", "qxx_qc_ipqc",
            "qxx_qc_template_index", "qxx_qc_template_product", "qxx_qc_template",
            "qxx_pro_feedback", "qxx_pro_card_process", "qxx_pro_card", "qxx_pro_route_process",
            "qxx_wm_product_recpt_line", "qxx_wm_product_recpt",
            "qxx_wm_transaction", "qxx_wm_material_stock", "qxx_pro_material_trace");
    }

    // ==================== A) 工序检：报工确认触发 ====================

    @Test
    @DisplayName("isCheck 工序报工确认后生成 IPQC 待检单：card_process.ipqc_id 回填 + 响应携带编码 + 快照阈值")
    void should_generate_ipqc_when_ischeck_process_confirmed()
    {
        setupCommonBinding(WO_IPQC_ITEM, "TPL-IPQC-FB");
        Long cardProcessId = seedCardProcess("Y");
        Long feedbackId = seedFeedback("FB-IPQC-01");

        Map<?, ?> resp = put(feedbackUrl() + "/confirm/" + feedbackId, null);
        assertThat(resp.get("code")).isEqualTo(200);
        String ipqcCode = (String) ((Map<?, ?>) resp.get("data")).get("ipqcCode");
        assertThat(ipqcCode).isNotBlank();

        Map<String, Object> ipqc = jdbcTemplate.queryForMap(
            "select ipqc_id, ipqc_type, status, source_doc_type, source_doc_id, quantity_min_check, "
            + "quantity_max_unqualified, item_id, process_id, card_id, workstation_id "
            + "from qxx_qc_ipqc where ipqc_code = ?", ipqcCode);
        assertThat(ipqc.get("ipqc_type")).isEqualTo("LAST_CHECK");
        assertThat(ipqc.get("status")).isEqualTo("PENDING");
        assertThat(ipqc.get("source_doc_type")).isEqualTo("pro_card_process");
        assertThat(((Number) ipqc.get("source_doc_id")).longValue()).isEqualTo(cardProcessId);
        assertThat(((Number) ipqc.get("quantity_min_check")).intValue()).isEqualTo(5);
        assertThat(((Number) ipqc.get("quantity_max_unqualified")).intValue()).isEqualTo(0);
        assertThat(((Number) ipqc.get("item_id")).longValue()).isEqualTo(WO_IPQC_ITEM);
        assertThat(((Number) ipqc.get("process_id")).longValue()).isEqualTo(PROCESS_ID);
        assertThat(((Number) ipqc.get("card_id")).longValue()).isEqualTo(CARD_ID);
        assertThat(ipqc.get("workstation_id")).isNotNull();

        // 流转卡工序挂点回填
        Map<String, Object> cp = jdbcTemplate.queryForMap(
            "select ipqc_id, ipqc_code from qxx_pro_card_process where record_id = ?", cardProcessId);
        assertThat(((Number) cp.get("ipqc_id")).longValue())
            .isEqualTo(((Number) ipqc.get("ipqc_id")).longValue());
        assertThat(cp.get("ipqc_code")).isEqualTo(ipqcCode);

        // 检验行按模板快照生成（2 个检测项）
        Integer lineCount = jdbcTemplate.queryForObject(
            "select count(1) from qxx_qc_order_line where qc_type = 'IPQC' and qc_id = ?",
            Integer.class, ipqc.get("ipqc_id"));
        assertThat(lineCount).isEqualTo(2);
    }

    @Test
    @DisplayName("非 isCheck 工序报工确认不生成 IPQC 单（响应 data 为空）")
    void should_not_generate_ipqc_when_not_ischeck()
    {
        setupCommonBinding(WO_IPQC_ITEM, "TPL-IPQC-NOCHK");
        seedCardProcess("N");
        Long feedbackId = seedFeedback("FB-IPQC-02");

        Map<?, ?> resp = put(feedbackUrl() + "/confirm/" + feedbackId, null);
        assertThat(resp.get("code")).isEqualTo(200);
        assertThat(resp.get("data")).isNull();

        Integer count = jdbcTemplate.queryForObject(
            "select count(1) from qxx_qc_ipqc where source_doc_type = 'pro_card_process'", Integer.class);
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("同流转卡工序第二次报工确认幂等：不重复生成（已有未关闭单）")
    void should_not_generate_ipqc_twice()
    {
        setupCommonBinding(WO_IPQC_ITEM, "TPL-IPQC-IDEM");
        seedCardProcess("Y");
        Long fb1 = seedFeedback("FB-IPQC-03");
        Long fb2 = seedFeedback("FB-IPQC-04");

        Map<?, ?> first = put(feedbackUrl() + "/confirm/" + fb1, null);
        assertThat(first.get("code")).isEqualTo(200);
        assertThat(((Map<?, ?>) first.get("data")).get("ipqcCode")).isNotNull();

        Map<?, ?> second = put(feedbackUrl() + "/confirm/" + fb2, null);
        assertThat(second.get("code")).isEqualTo(200);
        assertThat(second.get("data")).isNull();

        Integer count = jdbcTemplate.queryForObject(
            "select count(1) from qxx_qc_ipqc where source_doc_type = 'pro_card_process'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ==================== B) 完工检：成品入库触发 + 确认拦截 ====================

    @Test
    @DisplayName("成品入库单携行创建后生成完工检 LAST_CHECK 单：recpt.ipqc_id 回填 + 检验行生成")
    void should_generate_last_check_on_product_recpt()
    {
        setupCommonBinding(PRODUCT_ITEM, "TPL-IPQC-PR");
        Long recptId = createRecpt("PR-IPQC-01");

        List<Long> ipqcIds = ipqcIdsOfRecpt(recptId);
        assertThat(ipqcIds).hasSize(1);
        Map<String, Object> ipqc = jdbcTemplate.queryForMap(
            "select ipqc_type, status, source_doc_type, item_id, quantity_min_check, item_name "
            + "from qxx_qc_ipqc where ipqc_id = ?", ipqcIds.get(0));
        assertThat(ipqc.get("ipqc_type")).isEqualTo("LAST_CHECK");
        assertThat(ipqc.get("status")).isEqualTo("PENDING");
        assertThat(ipqc.get("source_doc_type")).isEqualTo("wm_product_recpt");
        assertThat(((Number) ipqc.get("item_id")).longValue()).isEqualTo(PRODUCT_ITEM);
        assertThat(((Number) ipqc.get("quantity_min_check")).intValue()).isEqualTo(5);
        assertThat((String) ipqc.get("item_name")).contains("物料");

        // 入库单头挂点回填
        Map<String, Object> recpt = jdbcTemplate.queryForMap(
            "select ipqc_id, ipqc_code from qxx_wm_product_recpt where recpt_id = ?", recptId);
        assertThat(((Number) recpt.get("ipqc_id")).longValue()).isEqualTo(ipqcIds.get(0));
        assertThat(String.valueOf(recpt.get("ipqc_code"))).isNotBlank();

        Integer lineCount = jdbcTemplate.queryForObject(
            "select count(1) from qxx_qc_order_line where qc_type = 'IPQC' and qc_id = ?",
            Integer.class, ipqcIds.get(0));
        assertThat(lineCount).isEqualTo(2);
    }

    @Test
    @DisplayName("PENDING 完工检单阻断确认入库：confirm 拒绝且单据保持草稿、库存未动")
    void should_reject_product_recpt_confirm_when_pending()
    {
        setupCommonBinding(PRODUCT_ITEM, "TPL-IPQC-PENDING");
        Long recptId = createRecpt("PR-IPQC-02");
        assertThat(ipqcIdsOfRecpt(recptId)).hasSize(1);

        Map<?, ?> resp = put(recptUrl() + "/confirm/" + recptId, null);
        assertThat(resp.get("code")).isEqualTo(500);
        assertThat(resp.get("msg").toString()).contains("需完工检验合格后方可确认入库");

        assertThat(recptStatus(recptId)).isEqualTo("DRAFT");
        assertThat(stockQty(PRODUCT_ITEM)).isEqualByComparingTo("0");
        assertThat(qcStatusOf("PR-IPQC-02")).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("判定 PASS 后放行：完工检 COMPLETED+PASS → confirm 成功，库存增加，列表 qcStatus=PASSED")
    void should_pass_product_recpt_confirm_after_judged()
    {
        setupCommonBinding(PRODUCT_ITEM, "TPL-IPQC-PASS");
        Long recptId = createRecpt("PR-IPQC-03");
        Long ipqcId = ipqcIdsOfRecpt(recptId).get(0);

        recordLineValuesAndJudgePass(ipqcId);

        Map<String, Object> judged = jdbcTemplate.queryForMap(
            "select status, check_result from qxx_qc_ipqc where ipqc_id = ?", ipqcId);
        assertThat(judged.get("status")).isEqualTo("COMPLETED");
        assertThat(judged.get("check_result")).isEqualTo("PASS");

        Map<?, ?> confirmResp = put(recptUrl() + "/confirm/" + recptId, null);
        assertThat(confirmResp.get("code")).isEqualTo(200);
        assertThat(recptStatus(recptId)).isEqualTo("CONFIRMED");
        assertThat(stockQty(PRODUCT_ITEM)).isEqualByComparingTo("10");
        assertThat(qcStatusOf("PR-IPQC-03")).isEqualTo("PASSED");
    }

    // ==================== C) 手工单：巡检创建 + 判定 ====================

    @Test
    @DisplayName("手工巡检单：TOUR_CHECK 创建成功（编码自动生成）→ 录值判定 COMPLETED+PASS")
    void should_support_manual_tour_check()
    {
        long templateId = setupTemplate("TPL-IPQC-TOUR");

        Map<String, Object> body = new HashMap<>();
        body.put("ipqcName", "车间巡检-印刷工位");
        body.put("ipqcType", "TOUR_CHECK");
        body.put("templateId", templateId);
        body.put("workorderId", 3L);
        body.put("workorderCode", "WO-IT");
        body.put("itemId", WO_IPQC_ITEM);
        body.put("itemCode", itemCodeOf(WO_IPQC_ITEM));
        body.put("itemName", itemCodeOf(WO_IPQC_ITEM) + "-物料");
        body.put("processId", PROCESS_ID);
        body.put("processName", "印刷");
        Map<?, ?> addResp = post(ipqcUrl(), body);
        assertThat(addResp.get("code")).isEqualTo(200);

        Long ipqcId = jdbcTemplate.queryForObject(
            "select ipqc_id from qxx_qc_ipqc where ipqc_type = 'TOUR_CHECK' order by ipqc_id desc limit 1",
            Long.class);
        Map<String, Object> saved = jdbcTemplate.queryForMap(
            "select ipqc_code, status, template_id from qxx_qc_ipqc where ipqc_id = ?", ipqcId);
        assertThat(String.valueOf(saved.get("ipqc_code"))).isNotBlank();
        assertThat(saved.get("status")).isEqualTo("PENDING");
        assertThat(((Number) saved.get("template_id")).longValue()).isEqualTo(templateId);

        // 非法检验类型拒绝
        Map<String, Object> bad = new HashMap<>(body);
        bad.put("ipqcType", "WHATEVER");
        Map<?, ?> badResp = post(ipqcUrl(), bad);
        assertThat(badResp.get("code")).isEqualTo(500);
        assertThat(badResp.get("msg").toString()).contains("检验类型非法");

        // 录值 → 判定 PASS（手工单与自动单共用同一 judge 链路）
        recordLineValuesAndJudgePass(ipqcId);
        Map<String, Object> judged = jdbcTemplate.queryForMap(
            "select status, check_result, inspector from qxx_qc_ipqc where ipqc_id = ?", ipqcId);
        assertThat(judged.get("status")).isEqualTo("COMPLETED");
        assertThat(judged.get("check_result")).isEqualTo("PASS");
        assertThat(judged.get("inspector")).isEqualTo("admin");
    }

    // ============ 前置数据与辅助 ============

    /** 建模板(2 个数值检测项,标准 100±1 / 200±2) + 物料通用绑定(抽检 5, Ac=0, 三率阈值 0；process_id NULL) */
    private void setupCommonBinding(long itemId, String templateCode)
    {
        long templateId = setupTemplate(templateCode);
        jdbcTemplate.update(
            "insert into qxx_qc_template_product (factory_id, template_id, item_id, item_code, item_name, "
            + "quantity_check, quantity_unqualified, cr_rate, maj_rate, min_rate, create_by) "
            + "values (?, ?, ?, ?, ?, 5, 0, 0, 0, 0, 'admin')",
            FACTORY_ID, templateId, itemId, itemCodeOf(itemId), itemCodeOf(itemId) + "-物料");
    }

    private long setupTemplate(String templateCode)
    {
        jdbcTemplate.update(
            "insert into qxx_qc_template (factory_id, template_code, template_name, qc_types, enable_flag, create_by) "
            + "values (?, ?, ?, 'IPQC', '1', 'admin')", FACTORY_ID, templateCode, templateCode + "-名称");
        Long templateId = jdbcTemplate.queryForObject(
            "select template_id from qxx_qc_template where template_code = ?", Long.class, templateCode);
        insertTemplateIndex(templateId, 3001L, "IPQC-LEN", "长度", 100, -1, 1, 1);
        insertTemplateIndex(templateId, 3002L, "IPQC-WID", "宽度", 200, -2, 2, 2);
        return templateId;
    }

    private void insertTemplateIndex(Long templateId, long indexId, String code, String name,
        double stander, double min, double max, int orderNum)
    {
        jdbcTemplate.update(
            "insert into qxx_qc_template_index (factory_id, template_id, index_id, index_code, index_name, "
            + "index_type, qc_result_type, stander_val, threshold_min, threshold_max, order_num, create_by) "
            + "values (?, ?, ?, ?, ?, 'IPQC', 'NUMBER', ?, ?, ?, ?, 'admin')",
            FACTORY_ID, templateId, indexId, code, name, stander, min, max, orderNum);
    }

    /** 建路线工序(isCheck) + 流转卡 + 流转卡工序，返回 card_process.record_id */
    private Long seedCardProcess(String isCheck)
    {
        jdbcTemplate.update(
            "insert into qxx_pro_route_process (factory_id, route_id, process_id, process_code, process_name, "
            + "order_num, is_check, create_by, create_time) values (?, ?, ?, 'OP-IT', '印刷工序', 1, ?, 'admin', now())",
            FACTORY_ID, ROUTE_ID, PROCESS_ID, isCheck);
        jdbcTemplate.update(
            "insert into qxx_pro_card (factory_id, card_id, item_code, item_name, unit_of_measure, create_by, create_time) "
            + "values (?, ?, ?, ?, 'PCS', 'admin', now())",
            FACTORY_ID, CARD_ID, itemCodeOf(WO_IPQC_ITEM), itemCodeOf(WO_IPQC_ITEM) + "-物料");
        jdbcTemplate.update(
            "insert into qxx_pro_card_process (factory_id, card_id, card_code, process_id, process_code, process_name, "
            + "workstation_id, user_id, create_by, create_time) "
            + "values (?, ?, 'CARD-IT', ?, 'OP-IT', '印刷工序', 401, 1, 'admin', now())",
            FACTORY_ID, CARD_ID, PROCESS_ID);
        return jdbcTemplate.queryForObject(
            "select record_id from qxx_pro_card_process where card_id = ? and process_id = ?",
            Long.class, CARD_ID, PROCESS_ID);
    }

    /** 建 PREPARE 报工记录（route/process/item/card 关联齐备） */
    private Long seedFeedback(String feedbackCode)
    {
        jdbcTemplate.update(
            "insert into qxx_pro_feedback (factory_id, feedback_type, feedback_code, workstation_id, workstation_code, "
            + "workorder_id, workorder_code, card_id, route_id, process_id, process_code, process_name, "
            + "item_id, item_code, item_name, unit_of_measure, quantity_feedback, quantity_qualified, status, "
            + "create_by, create_time) "
            + "values (?, 'INTERNAL', ?, 401, 'WS-IT', 3, 'WO-IT', ?, ?, ?, 'OP-IT', '印刷工序', "
            + "?, ?, ?, 'PCS', 10, 10, 'PREPARE', 'admin', now())",
            FACTORY_ID, feedbackCode, CARD_ID, ROUTE_ID, PROCESS_ID, WO_IPQC_ITEM,
            itemCodeOf(WO_IPQC_ITEM), itemCodeOf(WO_IPQC_ITEM) + "-物料");
        return jdbcTemplate.queryForObject(
            "select record_id from qxx_pro_feedback where feedback_code = ?", Long.class, feedbackCode);
    }

    /** 建成品入库单（PC 创建路径，携带行）— 触发 IPQC 完工检生成 hook */
    private Long createRecpt(String recptCode)
    {
        Map<String, Object> line = new HashMap<>();
        line.put("itemId", PRODUCT_ITEM);
        line.put("itemCode", itemCodeOf(PRODUCT_ITEM));
        line.put("itemName", itemCodeOf(PRODUCT_ITEM) + "-物料");
        line.put("unitOfMeasure", "PCS");
        line.put("unitName", "个");
        line.put("quantityRecpt", new BigDecimal("10"));
        line.put("warehouseId", 1);

        Map<String, Object> header = new HashMap<>();
        header.put("recptCode", recptCode);
        header.put("recptName", "IPQC集成测试-" + recptCode);
        header.put("produceId", PRODUCT_ITEM);
        header.put("produceCode", itemCodeOf(PRODUCT_ITEM));
        header.put("workorderId", 3);
        header.put("workorderCode", "WO-IT");
        header.put("warehouseId", 1);
        header.put("totalQuantity", new BigDecimal("10"));
        header.put("lines", List.of(line));

        Map<?, ?> resp = post(recptUrl(), header);
        assertThat(resp.get("code")).isEqualTo(200);
        return jdbcTemplate.queryForObject(
            "select recpt_id from qxx_wm_product_recpt where recpt_code = ?", Long.class, recptCode);
    }

    /** 取检验单详情 → 回填两行合格实测值 + 实际检测数 5 → edit 落库 → judge → 断言 PASS */
    private void recordLineValuesAndJudgePass(Long ipqcId)
    {
        Map<?, ?> detail = (Map<?, ?>) get(ipqcUrl() + "/" + ipqcId).get("data");
        List<Map<String, Object>> lines = (List<Map<String, Object>>) detail.get("lines");
        assertThat(lines).hasSize(2);
        lines.get(0).put("checkValText", VAL1_PASS);
        lines.get(1).put("checkValText", VAL2_PASS);

        Map<String, Object> editBody = new HashMap<>();
        editBody.put("ipqcId", ipqcId);
        editBody.put("quantityCheck", 5);
        editBody.put("lines", lines);
        Map<?, ?> editResp = put(ipqcUrl(), editBody);
        assertThat(editResp.get("code")).isEqualTo(200);

        Map<?, ?> judgeResp = put(ipqcUrl() + "/judge/" + ipqcId, Map.of());
        assertThat(judgeResp.get("code")).isEqualTo(200);
    }

    private List<Long> ipqcIdsOfRecpt(Long recptId)
    {
        return jdbcTemplate.queryForList(
            "select ipqc_id from qxx_qc_ipqc where source_doc_type = 'wm_product_recpt' and source_doc_id = ? order by ipqc_id",
            Long.class, recptId);
    }

    private String recptStatus(Long recptId)
    {
        return jdbcTemplate.queryForObject("select status from qxx_wm_product_recpt where recpt_id = ?", String.class, recptId);
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
