-- ============================================================
-- V132：补回填 V131 未覆盖的历史领料单 process_id + 执行重复单清理
--
-- 背景：
--   V131 从 issue_line.process_id 回填 header.process_id，但齐套看板路径
--   (ProWorkorderDocServiceImpl) 历史 bug 从未在 issue_line 上写 process_id，
--   导致重复单(432/433)的 header 和 line process_id 均为 NULL，V131 的去重
--   JOIN 条件 dup.process_id = good.process_id 无法匹配，清理未生效。
--
-- 内容：
--   1. 通过工单 BOM 按 item_id 反查 process_id，回填 header 和 issue_line
--      （仅当该 item 在工单 BOM 中只对应一个工序时回填，避免歧义）
--   2. 重跑 V131 的重复单清理逻辑（库存回补 + 冲销流水 + 删 trace + 置 CANCELED）
--
-- 幂等：UPDATE 重跑无目标行；INSERT 冲销流水用 NOT EXISTS 防重。
-- ============================================================

SET NAMES utf8mb4;

-- ════════════════════════════════════════════
-- 1. 从 BOM 反查 process_id 回填 issue_line
--    仅当 item 在工单 BOM 中唯一对应一个工序
-- ════════════════════════════════════════════
UPDATE qxx_wm_issue_line il
JOIN qxx_wm_issue_header h ON h.issue_id = il.issue_id
JOIN (
    SELECT b.workorder_id, b.item_id,
           MIN(b.process_id) AS pid, MIN(b.process_name) AS pname
    FROM qxx_pro_workorder_bom b
    WHERE b.process_id IS NOT NULL
    GROUP BY b.workorder_id, b.item_id
    HAVING COUNT(DISTINCT b.process_id) = 1   -- 仅当 item 唯一对应一工序
) bp ON bp.workorder_id = h.workorder_id AND bp.item_id = il.item_id
SET il.process_id = bp.pid,
    il.process_name = bp.pname
WHERE il.process_id IS NULL
  AND h.issue_type = 'PRODUCE'
  AND h.workorder_id IS NOT NULL;

-- ════════════════════════════════════════════
-- 2. 从 issue_line 回填 header.process_id（全行同工序）
-- ════════════════════════════════════════════
UPDATE qxx_wm_issue_header h
JOIN (
    SELECT issue_id, MIN(process_id) AS pid, MIN(process_name) AS pname
    FROM qxx_wm_issue_line
    WHERE process_id IS NOT NULL
    GROUP BY issue_id
    HAVING MIN(process_id) = MAX(process_id)
) l ON l.issue_id = h.issue_id
SET h.process_id = l.pid,
    h.process_name = l.pname
WHERE h.process_id IS NULL;

-- ════════════════════════════════════════════
-- 3. 重复单清理（同 V131 step 4，此时 process_id 已回填）
-- ════════════════════════════════════════════

-- 3a. 回补库存 onhand
UPDATE qxx_wm_material_stock ms
JOIN qxx_wm_transaction t ON t.material_stock_id = ms.material_stock_id
JOIN (
    SELECT dup.issue_id
    FROM qxx_wm_issue_header dup
    JOIN qxx_wm_issue_header good
      ON good.workorder_id = dup.workorder_id
     AND good.process_id = dup.process_id
     AND good.issue_type = 'PRODUCE'
     AND good.task_id IS NOT NULL
     AND good.status NOT IN ('CANCELED','CLOSED')
    WHERE dup.issue_type = 'PRODUCE'
      AND dup.task_id IS NULL
      AND dup.process_id IS NOT NULL
      AND dup.status NOT IN ('CANCELED','CLOSED')
) d ON d.issue_id = t.source_doc_id
SET ms.quantity_onhand = ms.quantity_onhand + ABS(t.quantity),
    ms.update_time = NOW()
WHERE t.source_doc_type = 'ISSUE'
  AND t.transaction_type = 'ISSUE_OUT';

-- 3b. 写冲销流水（NOT EXISTS 防重复执行）
INSERT INTO qxx_wm_transaction
    (factory_id, transaction_type, source_doc_type, source_doc_id, source_doc_code,
     source_line_id, material_stock_id, item_id, item_code, item_name, specification,
     unit_of_measure, unit_name, quantity, warehouse_id, warehouse_code, warehouse_name,
     workorder_id, workorder_code, transaction_time, remark, create_by, create_time)
SELECT t.factory_id, 'ISSUE_OUT', 'ISSUE', t.source_doc_id, t.source_doc_code,
       t.source_line_id, t.material_stock_id, t.item_id, t.item_code, t.item_name, t.specification,
       t.unit_of_measure, t.unit_name, ABS(t.quantity), t.warehouse_id, t.warehouse_code, t.warehouse_name,
       t.workorder_id, t.workorder_code, NOW(),
       CONCAT('重复领料单数据修复冲销：', t.source_doc_code), 'system', NOW()
FROM qxx_wm_transaction t
JOIN (
    SELECT dup.issue_id
    FROM qxx_wm_issue_header dup
    JOIN qxx_wm_issue_header good
      ON good.workorder_id = dup.workorder_id
     AND good.process_id = dup.process_id
     AND good.issue_type = 'PRODUCE'
     AND good.task_id IS NOT NULL
     AND good.status NOT IN ('CANCELED','CLOSED')
    WHERE dup.issue_type = 'PRODUCE'
      AND dup.task_id IS NULL
      AND dup.process_id IS NOT NULL
      AND dup.status NOT IN ('CANCELED','CLOSED')
) d ON d.issue_id = t.source_doc_id
WHERE t.source_doc_type = 'ISSUE'
  AND t.transaction_type = 'ISSUE_OUT'
  AND t.quantity < 0
  AND NOT EXISTS (
      SELECT 1 FROM qxx_wm_transaction x
      WHERE x.source_doc_type = 'ISSUE'
        AND x.source_doc_id = t.source_doc_id
        AND x.transaction_type = 'ISSUE_OUT'
        AND x.quantity > 0
        AND x.remark LIKE '%重复领料单数据修复冲销%'
  );

-- 3c. 删关联 material_trace
DELETE FROM qxx_pro_material_trace
WHERE issue_id IN (
    SELECT dup.issue_id
    FROM qxx_wm_issue_header dup
    JOIN qxx_wm_issue_header good
      ON good.workorder_id = dup.workorder_id
     AND good.process_id = dup.process_id
     AND good.issue_type = 'PRODUCE'
     AND good.task_id IS NOT NULL
     AND good.status NOT IN ('CANCELED','CLOSED')
    WHERE dup.issue_type = 'PRODUCE'
      AND dup.task_id IS NULL
      AND dup.process_id IS NOT NULL
      AND dup.status NOT IN ('CANCELED','CLOSED')
);

-- 3d. 置重复单为 CANCELED
UPDATE qxx_wm_issue_header dup
JOIN qxx_wm_issue_header good
  ON good.workorder_id = dup.workorder_id
 AND good.process_id = dup.process_id
 AND good.issue_type = 'PRODUCE'
 AND good.task_id IS NOT NULL
 AND good.status NOT IN ('CANCELED','CLOSED')
SET dup.status = 'CANCELED',
    dup.cancel_reason = '系统重复生成（齐套看板与开工检查跨路径），数据修复作废',
    dup.update_by = 'system',
    dup.update_time = NOW()
WHERE dup.issue_type = 'PRODUCE'
  AND dup.task_id IS NULL
  AND dup.process_id IS NOT NULL
  AND dup.status NOT IN ('CANCELED','CLOSED');
