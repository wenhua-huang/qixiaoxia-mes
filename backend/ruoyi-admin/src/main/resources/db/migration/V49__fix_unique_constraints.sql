-- ============================================================
-- V49: ① 采购订单行物料唯一约束 ② sys_auto_code_part 约束补 factory_id
-- ============================================================

-- Part 1: 禁止同一采购订单内出现重复物料行
-- 幂等：先检查约束是否存在
SET @cnt = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_pur_order_line'
    AND CONSTRAINT_NAME = 'uk_order_item');
SET @sql = IF(@cnt = 0,
    'ALTER TABLE qxx_pur_order_line ADD UNIQUE KEY uk_order_item (order_id, item_id)',
    'SELECT 1 AS skipped');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Part 2: 修复 V48 的 uk_rule_part 缺 factory_id — 改为 (factory_id, rule_id, part_index)
SET @cnt2 = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_auto_code_part'
    AND CONSTRAINT_NAME = 'uk_rule_part');
SET @sql2 = IF(@cnt2 > 0,
    'ALTER TABLE sys_auto_code_part DROP INDEX uk_rule_part, ADD UNIQUE KEY uk_factory_rule_part (factory_id, rule_id, part_index)',
    'SELECT 1 AS skipped_v48');
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
