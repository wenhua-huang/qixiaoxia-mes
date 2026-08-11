-- ============================================================
-- V94：分切工序重构 —— 库存驱动 + 报工自动建卷
--
-- 背景：V92 初版分切依赖 qxx_wm_roll_detail 预存母卷，但该表是孤儿表
--       （采购入库/报工/领料均不写入），导致功能悬空。
-- 本次重构（方案 A 修正版）：
--   1. 分切改为「库存驱动」：选物料 → 领料出库(扣 material_stock) → 分切报工
--   2. 母卷/子卷在报工事务内「自动创建」（roll_detail 从前提降级为产物）
--   3. 删除 SPLIT 事务（被领料 ISSUE_OUT 取代，避免双重扣减）
--
-- 本迁移只加字段，不改 roll_detail 表结构（不引入 ISSUED 状态）。
-- 幂等：ALTER 前查 information_schema。
-- ============================================================

SET NAMES utf8mb4;

-- ════════════════════════════════════════════
-- 1. qxx_pro_slitting_record 加领料字段
-- ════════════════════════════════════════════

-- 1.1 领料物料 source_item_id / code / name
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'source_item_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN source_item_id bigint(20) default null comment ''领料母卷物料ID'' AFTER parent_roll_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'source_item_code');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN source_item_code varchar(64) default null comment ''领料母卷物料编码'' AFTER source_item_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'source_item_name');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN source_item_name varchar(255) default null comment ''领料母卷物料名称'' AFTER source_item_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 领料仓库 source_warehouse_id / code / name
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'source_warehouse_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN source_warehouse_id bigint(20) default null comment ''领料出库仓库ID'' AFTER source_item_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'source_warehouse_code');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN source_warehouse_code varchar(64) default null comment ''领料出库仓库编码'' AFTER source_warehouse_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'source_warehouse_name');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN source_warehouse_name varchar(255) default null comment ''领料出库仓库名称'' AFTER source_warehouse_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 领料数量/时间/人
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'pick_qty');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN pick_qty decimal(14,4) default 0.0000 comment ''领料数量(吨)'' AFTER source_warehouse_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'pick_time');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN pick_time datetime default null comment ''领料时间'' AFTER pick_qty',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'pick_by');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN pick_by varchar(64) default null comment ''领料人'' AFTER pick_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ════════════════════════════════════════════
-- 2. relax roll_detail.recpt_id 约束（报工建卷时无入库单，允许 NULL）
-- ════════════════════════════════════════════
SET @is_nullable = (SELECT IS_NULLABLE FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_wm_roll_detail' AND column_name = 'recpt_id');
SET @sql = IF(@is_nullable = 'NO',
    'ALTER TABLE qxx_wm_roll_detail MODIFY COLUMN recpt_id bigint(20) default null comment ''入库单ID(关联qxx_wm_item_recpt;报工建卷时为NULL)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
