-- =====================================================
-- V86: 补齐退料明细表 qxx_wm_rt_issue_line 缺失列
-- =====================================================
-- 背景：
--   退料明细表 DDL 与 WmRtIssueLine 实体/mapper 不一致，表缺失 3 列：
--     - issue_id      关联原领料单头（退料追溯必需）
--     - issue_line_id 关联原领料明细行（差额退/分批退追溯必需）
--     - quantity_rted 已退料累计（分批退料必需）
--   另 mapper 用 item_spc 但表列为 specification（列名不匹配，已在 mapper 侧适配，此处不改列名）。
--
-- 影响：
--   缺列导致 insertWmRtIssueLine 一直报 Unknown column，退料行无法落库，
--   整个退料功能（含 createFromIssue / buildFromIssue / 自动生成）从未真正可用。
--
-- 幂等：使用 IF NOT EXISTS（MySQL 8.0.29+ 支持）或 information_schema 判断，
--   此处用条件 ADD 确保 repeated execution 安全。

-- issue_id：关联原领料单头
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_wm_rt_issue_line' AND column_name = 'issue_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_rt_issue_line ADD COLUMN issue_id bigint NULL AFTER rt_id',
    'SELECT "issue_id already exists"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- issue_line_id：关联原领料明细行
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_wm_rt_issue_line' AND column_name = 'issue_line_id');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_rt_issue_line ADD COLUMN issue_line_id bigint NULL AFTER issue_id',
    'SELECT "issue_line_id already exists"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- quantity_rted：已退料累计
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_wm_rt_issue_line' AND column_name = 'quantity_rted');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_rt_issue_line ADD COLUMN quantity_rted decimal(14,4) NULL DEFAULT 0.0000 AFTER quantity_rt',
    'SELECT "quantity_rted already exists"');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
