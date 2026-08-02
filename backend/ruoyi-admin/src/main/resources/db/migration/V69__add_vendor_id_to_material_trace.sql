-- ============================================================
-- V69: material_trace 加 vendor_id（采购入库 RECEIPT 追溯）
--
-- 背景：追溯链起点需从采购入库开始 — 记录原料从哪个供应商
--       /采购订单入的库，链入 material_trace。
-- 幂等：ALTER 前查 information_schema。
-- 日期：2026-07-14
-- ============================================================

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_pro_material_trace' AND COLUMN_NAME = 'vendor_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE qxx_pro_material_trace ADD COLUMN vendor_id bigint DEFAULT NULL COMMENT ''供应商ID(入库RECEIPT类型时记录)'' AFTER card_id',
  'SELECT ''vendor_id already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
