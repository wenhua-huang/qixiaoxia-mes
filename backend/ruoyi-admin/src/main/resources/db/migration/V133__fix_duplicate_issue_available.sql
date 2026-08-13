-- ============================================================
-- V133：修复 V131/V132 重复领料单冲销只回补 onhand 未回补 available
--
-- 背景：
--   领料单确认时同时产生两类事务：
--     ALLOCATE  -1  （扣 quantity_available，预占可用）
--     ISSUE_OUT -1  （扣 quantity_onhand，实发在库）
--   V131/V132 清理重复单时只冲销了 ISSUE_OUT（回补 onhand），
--   未冲销 ALLOCATE（未回补 available），导致 FIFO 分配
--   (selectAvailableBatches WHERE quantity_available > 0) 少计可用库存。
--
-- 内容：
--   1. 为每张被冲销重复单的负 ALLOCATE 写正向 ALLOCATE 流水（审计）
--   2. 按该单 ALLOCATE 总量回补 quantity_available，上限 quantity_onhand
--
-- 幂等：
--   - step1 INSERT 用 NOT EXISTS 防重（remark 匹配）
--   - step2 UPDATE 用 material_stock.remark 标记 [avail-fixed-v133] 防重
--   两条语句各自独立幂等，重跑无副作用。
--   注意：step2 不依赖 step1 刚插入的正向流水（否则 NOT EXISTS 会把目标行过滤掉），
--   而是直接从 V131/V132 已写入的冲销 ISSUE_OUT 反查对应库存桶。
-- ============================================================

SET NAMES utf8mb4;

-- 1. 写正向 ALLOCATE 冲销流水（一条原始负 ALLOCATE 对应一条正向回补）
INSERT INTO qxx_wm_transaction
    (factory_id, transaction_type, source_doc_type, source_doc_id, source_doc_code,
     source_line_id, material_stock_id, item_id, item_code, item_name, specification,
     unit_of_measure, unit_name, quantity, warehouse_id, warehouse_code, warehouse_name,
     workorder_id, workorder_code, transaction_time, remark, create_by, create_time)
SELECT t.factory_id, 'ALLOCATE', 'ISSUE', t.source_doc_id, t.source_doc_code,
       t.source_line_id, t.material_stock_id, t.item_id, t.item_code, t.item_name, t.specification,
       t.unit_of_measure, t.unit_name, ABS(t.quantity), t.warehouse_id, t.warehouse_code, t.warehouse_name,
       t.workorder_id, t.workorder_code, NOW(),
       CONCAT('重复领料单可用库存回补：', t.source_doc_code), 'system', NOW()
FROM qxx_wm_transaction t
WHERE t.source_doc_type = 'ISSUE'
  AND t.transaction_type = 'ALLOCATE'
  AND t.quantity < 0
  AND t.source_doc_id IN (
      -- 被 V131/V132 冲销的重复领料单（已有正向 ISSUE_OUT 冲销流水）
      SELECT source_doc_id FROM qxx_wm_transaction
      WHERE remark LIKE '%重复领料单数据修复冲销%'
        AND transaction_type = 'ISSUE_OUT'
        AND quantity > 0
  )
  AND NOT EXISTS (
      SELECT 1 FROM qxx_wm_transaction x
      WHERE x.source_doc_id = t.source_doc_id
        AND x.material_stock_id = t.material_stock_id
        AND x.transaction_type = 'ALLOCATE'
        AND x.quantity > 0
        AND x.remark LIKE '%重复领料单可用库存回补%'
  );

-- 2. 回补 quantity_available
--    从 V131/V132 写入的正向 ISSUE_OUT 冲销流水反查受影响库存桶，
--    再聚合该桶上重复单的负 ALLOCATE 总量回补；remark 标记防重。
UPDATE qxx_wm_material_stock ms
JOIN (
    SELECT t.material_stock_id,
           SUM(ABS(t.quantity)) AS recover_qty
    FROM qxx_wm_transaction t
    WHERE t.source_doc_type = 'ISSUE'
      AND t.transaction_type = 'ALLOCATE'
      AND t.quantity < 0
      AND t.source_doc_id IN (
          SELECT source_doc_id FROM qxx_wm_transaction
          WHERE remark LIKE '%重复领料单数据修复冲销%'
            AND transaction_type = 'ISSUE_OUT'
            AND quantity > 0
      )
    GROUP BY t.material_stock_id
) r ON r.material_stock_id = ms.material_stock_id
SET ms.quantity_available = LEAST(ms.quantity_onhand,
        ms.quantity_available + r.recover_qty),
    ms.remark = CONCAT(IFNULL(ms.remark, ''), ' [avail-fixed-v133]'),
    ms.update_time = NOW()
WHERE IFNULL(ms.remark, '') NOT LIKE '%avail-fixed-v133%';
