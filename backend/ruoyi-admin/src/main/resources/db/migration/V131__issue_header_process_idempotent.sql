-- ============================================================
-- V131：领料单按工序幂等修复（防重复生成 + 历史脏数据清理）
--
-- 背景：
--   领料单有两条自动生成路径：
--     A. 齐套看板"一键生成" (ProWorkorderDocServiceImpl, 锁 pro:workorder:doc-gen:{wid})
--     B. 工单"开工检查"   (ProWorkorderServiceImpl,  锁 pro:workorder:start:{wid})
--   两路径锁不互通，且幂等检查均仅认 task_id。若路径 A 在排产任务创建前执行
--   （task_id=NULL），路径 B 在排产后执行（task_id 非空），就会为同一工序
--   生成两张领料单，导致重复扣料。
--
-- 内容：
--   1. qxx_wm_issue_header 加 process_id / process_name 列（工序级幂等依据）
--   2. 加索引 idx_issue_header_wo_process (workorder_id, process_id)
--   3. 从 issue_line 回填历史 header 的 process_id / process_name
--   4. 清理重复领料单（task_id=NULL 且同工单同工序已有 task_id 非空单）：
--      - 回补库存 onhand
--      - 写冲销流水（ISSUE_OUT 正向）
--      - 删关联 material_trace
--      - 置 CANCELED
--
-- 幂等：列/索引 PROCEDURE 包装；DML 重跑无目标行。
-- ============================================================

SET NAMES utf8mb4;

-- ────────────────────────────────────────────
-- 0. 通用幂等工具
-- ────────────────────────────────────────────
DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = tbl AND column_name = col
    ) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

DROP PROCEDURE IF EXISTS add_index_if_missing;
CREATE PROCEDURE add_index_if_missing(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = tbl AND index_name = idx
    ) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

-- ════════════════════════════════════════════
-- 1. 加列 process_id / process_name
-- ════════════════════════════════════════════
CALL add_col_if_missing('qxx_wm_issue_header', 'process_id',
    'ALTER TABLE qxx_wm_issue_header ADD COLUMN process_id BIGINT NULL COMMENT ''工序ID（幂等分组依据）'' AFTER task_id');
CALL add_col_if_missing('qxx_wm_issue_header', 'process_name',
    'ALTER TABLE qxx_wm_issue_header ADD COLUMN process_name VARCHAR(255) NULL COMMENT ''工序名称'' AFTER process_id');

-- ════════════════════════════════════════════
-- 2. 索引
-- ════════════════════════════════════════════
CALL add_index_if_missing('qxx_wm_issue_header', 'idx_issue_header_wo_process',
    'CREATE INDEX idx_issue_header_wo_process ON qxx_wm_issue_header (workorder_id, process_id)');

-- ════════════════════════════════════════════
-- 3. 从 issue_line 回填 header.process_id / process_name
--    （自动生成路径每单对应一个工序，行的 process_id 一致）
-- ════════════════════════════════════════════
UPDATE qxx_wm_issue_header h
JOIN (
    SELECT issue_id, MIN(process_id) AS pid, MIN(process_name) AS pname
    FROM qxx_wm_issue_line
    WHERE process_id IS NOT NULL
    GROUP BY issue_id
    HAVING MIN(process_id) = MAX(process_id)  -- 仅当全行同工序时回填
) l ON l.issue_id = h.issue_id
SET h.process_id = l.pid,
    h.process_name = l.pname
WHERE h.process_id IS NULL;

-- ════════════════════════════════════════════
-- 4. 清理重复领料单
--    重复定义：issue_type='PRODUCE' AND task_id IS NULL AND status 非终态，
--    且同工单+同 process_id 存在一张 task_id IS NOT NULL 的非终态单。
-- ════════════════════════════════════════════

-- 4a. 回补库存：对每张重复单的 ISSUE_OUT 流水，将数量加回 material_stock
--     （ISSUE_OUT 为负值，加 ABS 即回补；与退料 executeReturn 一致仅动 onhand）
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

-- 4b. 写冲销流水（ISSUE_OUT 正值，remark 标注数据修复）
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
  AND t.quantity < 0;

-- 4c. 删关联的 material_trace（重复单的 ISSUE 追溯）
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

-- 4d. 置重复单为 CANCELED
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

-- 清理临时 PROCEDURE
DROP PROCEDURE IF EXISTS add_col_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
