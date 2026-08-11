-- ============================================================
-- V114：补外协发料 MATERIAL_STOCK → CARD 追溯边，修复反查断链
--
-- 背景：
--   OutsourceServiceImpl.writeIssueTrace 只写了 MATERIAL_STOCK → OUTSOURCE_ORDER 边，
--   漏写了与厂内 ISSUE 边对称的 MATERIAL_STOCK → CARD 边。反查追溯按 child_type+child_id
--   精确匹配，CARD 节点没有入边就断链（库存→报工→流转卡 后无法回溯到原料库存）。
--
--   本迁移对已存在的 OUTSOURCE_ISSUE / child_type=OUTSOURCE_ORDER / card_id 非空 记录，
--   补插对应的 MATERIAL_STOCK → CARD 边。新代码已在 writeIssueTrace 中同步写两条边。
--
-- 幂等：NOT EXISTS 防重复；显式写 factory_id（Flyway 裸 JDBC 不走拦截器）。
-- ============================================================

SET NAMES utf8mb4;

INSERT INTO qxx_pro_material_trace
    (factory_id, trace_type, parent_type, parent_id, child_type, child_id,
     quantity, unit_of_measure, workorder_id, card_id, vendor_id, process_id,
     trace_time, create_by, create_time)
SELECT
    t.factory_id,
    'OUTSOURCE_ISSUE',
    t.parent_type, t.parent_id,
    'CARD', t.card_id,
    t.quantity, t.unit_of_measure,
    t.workorder_id, t.card_id, t.vendor_id, t.process_id,
    t.trace_time, 'flyway-v114', NOW()
FROM qxx_pro_material_trace t
WHERE t.trace_type = 'OUTSOURCE_ISSUE'
  AND t.child_type = 'OUTSOURCE_ORDER'
  AND t.card_id IS NOT NULL
  AND t.parent_type = 'MATERIAL_STOCK'
  AND t.parent_id <> 0
  AND NOT EXISTS (
      SELECT 1 FROM qxx_pro_material_trace x
      WHERE x.parent_type = t.parent_type
        AND x.parent_id = t.parent_id
        AND x.child_type = 'CARD'
        AND x.child_id = t.card_id
        AND x.trace_type = 'OUTSOURCE_ISSUE'
  );
