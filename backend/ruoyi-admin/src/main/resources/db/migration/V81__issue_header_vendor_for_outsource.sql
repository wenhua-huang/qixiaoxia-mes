-- ============================================================
-- V70: 领料单头表加 vendor_id/vendor_code（外协发料需求）
--
-- 背景：外协分切场景复用 qxx_wm_issue_header（issue_type='OUTSOURCE'），
--       需要 vendor_id 记录发给了哪个供应商，用于外协发料追溯(OUTSOURCE_ISSUE trace)。
-- 幂等：ALTER 前查 information_schema。
-- 日期：2026-07-14
-- ============================================================

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'vendor_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE qxx_wm_issue_header ADD COLUMN vendor_id bigint DEFAULT NULL COMMENT ''供应商ID(外协发料时填写)'' AFTER workstation_name',
  'SELECT ''vendor_id already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'vendor_code');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE qxx_wm_issue_header ADD COLUMN vendor_code varchar(64) DEFAULT NULL COMMENT ''供应商编码'' AFTER vendor_id',
  'SELECT ''vendor_code already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
