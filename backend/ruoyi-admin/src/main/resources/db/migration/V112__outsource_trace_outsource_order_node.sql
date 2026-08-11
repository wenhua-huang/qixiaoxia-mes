-- ============================================================
-- V112：外协追溯根治 —— 引入 OUTSOURCE_ORDER 节点 + 回填断链历史数据
--
-- 背景：
--   旧模型外协发料/收货边都挂在全局 VENDOR 节点上（MATERIAL_STOCK→VENDOR→FEEDBACK），
--   反向追溯时同一供应商的全部历史外协单汇聚成扇出枢纽，且存在三段断链：
--     ① 发料边 parent_id=0（原料批次丢失）
--     ② 收货缺 FEEDBACK→MATERIAL_STOCK 边（成品库存孤儿）
--     ③ 外协报工缺 CARD/WORKORDER→FEEDBACK 边（未挂回生产主链）
--
-- 新模型：外协单 OUTSOURCE_ORDER 取代供应商成为追溯作用域锚点
--   MATERIAL_STOCK → OUTSOURCE_ORDER → FEEDBACK → MATERIAL_STOCK
--                                    ↑(CARD/WORKORDER → FEEDBACK)
--   供应商信息保留在 trace.vendor_id 和外协单节点描述上。
--
-- 幂等：所有 UPDATE 带类型/值条件，所有 INSERT 带 NOT EXISTS，可重复执行。
-- 注意：sys_dict_* 无 factory_id；业务表 INSERT 显式写 factory_id。
-- ============================================================

SET NAMES utf8mb4;

-- ============ 1. 字典：新增 OUTSOURCE_ORDER 节点类型 ============
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 11, '外协单', 'OUTSOURCE_ORDER', 'mes_material_trace_node_type', '', 'info', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'OUTSOURCE_ORDER');

-- ============ 2. 回填外协发料边 ============
-- 旧：MATERIAL_STOCK(parent_id=0) → VENDOR
-- 新：MATERIAL_STOCK(真实库存ID) → OUTSOURCE_ORDER(外协单ID)
-- 关联路径：trace → 外协单(vendor+workorder+时间窗) → 发料行(按单位消歧同秒多单) → 事务(source_line_id)
-- 无法匹配的孤立发料边（如早期无单据的 120 ROLL 测试数据）保持原样，不强行造数据。
UPDATE qxx_pro_material_trace tr
JOIN qxx_wm_outsource_order o ON o.vendor_id = tr.vendor_id
    AND ((o.workorder_id = tr.workorder_id) OR (o.workorder_id IS NULL AND tr.workorder_id IS NULL))
    AND ABS(TIMESTAMPDIFF(SECOND, o.issue_time, tr.trace_time)) <= 2
JOIN qxx_wm_outsource_issue_line il ON il.order_id = o.order_id AND il.unit_of_measure = tr.unit_of_measure
JOIN qxx_wm_transaction t ON t.source_doc_id = o.order_id
    AND t.source_line_id = il.line_id
    AND t.transaction_type = 'OUTSOURCE_ISSUE'
SET tr.parent_id = t.material_stock_id,
    tr.child_type = 'OUTSOURCE_ORDER',
    tr.child_id = o.order_id,
    tr.card_id = COALESCE(tr.card_id, o.card_id),
    tr.process_id = COALESCE(tr.process_id, o.process_id)
WHERE tr.trace_type = 'OUTSOURCE_ISSUE'
  AND tr.child_type = 'VENDOR'
  AND t.material_stock_id IS NOT NULL;

-- ============ 3. 转换外协收货边 ============
-- 旧：VENDOR → FEEDBACK
-- 新：OUTSOURCE_ORDER(外协单ID) → FEEDBACK
-- 通过 order.feedback_id = trace.child_id 精确关联（1:1）。
UPDATE qxx_pro_material_trace tr
JOIN qxx_wm_outsource_order o ON o.feedback_id = tr.child_id
SET tr.parent_type = 'OUTSOURCE_ORDER',
    tr.parent_id = o.order_id,
    tr.card_id = COALESCE(tr.card_id, o.card_id),
    tr.process_id = COALESCE(tr.process_id, o.process_id)
WHERE tr.trace_type = 'OUTSOURCE_RECPT'
  AND tr.parent_type = 'VENDOR';

-- ============ 4. 补插 FEEDBACK → MATERIAL_STOCK 入库边 ============
-- 每条收货行对应一条 OUTSOURCE_RECPT 事务（source_line_id = recpt_line.line_id），
-- 取事务的 material_stock_id 作为入库库存节点。与 writeStockinTrace 新代码对等。
INSERT INTO qxx_pro_material_trace
    (factory_id, trace_type, parent_type, parent_id, child_type, child_id, quantity, unit_of_measure,
     workorder_id, card_id, vendor_id, process_id, feedback_id, transaction_id, trace_time, create_by, create_time)
SELECT
    o.factory_id,
    'OUTSOURCE_RECPT',
    'FEEDBACK', o.feedback_id,
    'MATERIAL_STOCK', t.material_stock_id,
    rl.quantity,
    COALESCE(rl.unit_of_measure, 'TON'),
    o.workorder_id, o.card_id, o.vendor_id, o.process_id,
    o.feedback_id, t.transaction_id,
    COALESCE(t.transaction_time, NOW()),
    'flyway-v112', NOW()
FROM qxx_wm_outsource_recpt_line rl
JOIN qxx_wm_outsource_order o ON o.order_id = rl.order_id
JOIN qxx_wm_transaction t ON t.source_doc_id = o.order_id
    AND t.source_line_id = rl.line_id
    AND t.transaction_type = 'OUTSOURCE_RECPT'
    AND t.material_stock_id IS NOT NULL
WHERE o.feedback_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM qxx_pro_material_trace ex
      WHERE ex.trace_type = 'OUTSOURCE_RECPT'
        AND ex.parent_type = 'FEEDBACK' AND ex.parent_id = o.feedback_id
        AND ex.child_type = 'MATERIAL_STOCK' AND ex.child_id = t.material_stock_id
        AND ex.transaction_id = t.transaction_id
  );

-- ============ 5. 补插 CARD/WORKORDER → FEEDBACK 外协加工边 ============
-- 每个已收货外协单（feedback_id 非空）补一条主链边，把外协报工挂回流转卡或工单。
-- 有 card_id 用 CARD；无 card 有 workorder 用 WORKORDER；两者皆无（早期无单据测试）跳过。
-- 数量取报工聚合数量（pro_feedback.quantity）。
INSERT INTO qxx_pro_material_trace
    (factory_id, trace_type, parent_type, parent_id, child_type, child_id, quantity, unit_of_measure,
     workorder_id, card_id, vendor_id, process_id, feedback_id, trace_time, create_by, create_time)
SELECT
    o.factory_id,
    'OUTSOURCE_PROCESS',
    CASE WHEN o.card_id IS NOT NULL THEN 'CARD' ELSE 'WORKORDER' END,
    CASE WHEN o.card_id IS NOT NULL THEN o.card_id ELSE o.workorder_id END,
    'FEEDBACK', o.feedback_id,
    COALESCE(fb.quantity, 0),
    COALESCE(fb.unit_of_measure, 'TON'),
    o.workorder_id, o.card_id, o.vendor_id, o.process_id,
    o.feedback_id,
    COALESCE(o.receive_time, NOW()),
    'flyway-v112', NOW()
FROM qxx_wm_outsource_order o
JOIN qxx_pro_feedback fb ON fb.record_id = o.feedback_id
WHERE o.feedback_id IS NOT NULL
  AND o.status = 'RECEIVED'
  AND (o.card_id IS NOT NULL OR o.workorder_id IS NOT NULL)
  AND NOT EXISTS (
      SELECT 1 FROM qxx_pro_material_trace ex
      WHERE ex.child_type = 'FEEDBACK' AND ex.child_id = o.feedback_id
        AND ex.parent_type IN ('CARD', 'WORKORDER')
        AND ex.trace_type = 'OUTSOURCE_PROCESS'
  );
