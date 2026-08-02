-- ============================================================
-- V68: 领料单/报工记录加 card_id 字段（流转卡追溯链修复）
--
-- 背景：
--   领料单(qxx_wm_issue_header) / 报工(qxx_pro_feedback) 缺 card_id 字段，
--   导致追溯链断在流转卡节点（trace child_id 硬编码 0）。
--   本迁移加字段，让领料/报工关联流转卡，追溯链连通。
--
-- 幂等：ALTER 前先查 information_schema。
-- 日期：2026-07-14
-- ============================================================

-- ════════════════════════════════════════════
-- 1. 领料单头表加 card_id / card_code
--    用途：领料时可指定投给哪张流转卡，追溯精确到卡
-- ════════════════════════════════════════════
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'card_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE qxx_wm_issue_header ADD COLUMN card_id bigint DEFAULT NULL COMMENT ''流转卡ID(关联qxx_pro_card)'' AFTER workorder_id',
  'SELECT ''card_id already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_issue_header' AND COLUMN_NAME = 'card_code');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE qxx_wm_issue_header ADD COLUMN card_code varchar(64) DEFAULT NULL COMMENT ''流转卡编码'' AFTER card_id',
  'SELECT ''card_code already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ════════════════════════════════════════════
-- 2. 报工记录表加 card_id
--    用途：报工关联流转卡，产出追溯到卡；审核后推进卡工序状态
-- ════════════════════════════════════════════
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_pro_feedback' AND COLUMN_NAME = 'card_id');
SET @sql = IF(@col_exists = 0,
  'ALTER TABLE qxx_pro_feedback ADD COLUMN card_id bigint DEFAULT NULL COMMENT ''流转卡ID(关联qxx_pro_card)'' AFTER workorder_id',
  'SELECT ''feedback.card_id already exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
