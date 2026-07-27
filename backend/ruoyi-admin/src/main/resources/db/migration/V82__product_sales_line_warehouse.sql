-- V81: 销售出库单行表补仓库展示字段 + 本仓可用量快照
-- 配合「从销售订单生成」时按仓库拆行：每行携带独立仓库信息与建议时的可用量

-- 安全加列（已存在则跳过，保证幂等）
-- warehouse_code
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_product_sales_line' AND COLUMN_NAME = 'warehouse_code');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_product_sales_line ADD COLUMN warehouse_code varchar(64) DEFAULT NULL COMMENT ''仓库编码'' AFTER warehouse_id',
    'SELECT ''warehouse_code already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- warehouse_name
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_product_sales_line' AND COLUMN_NAME = 'warehouse_name');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_product_sales_line ADD COLUMN warehouse_name varchar(128) DEFAULT NULL COMMENT ''仓库名称'' AFTER warehouse_code',
    'SELECT ''warehouse_name already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- available_qty（建议出库时的本仓可用量快照，用于前端展示与校验）
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_product_sales_line' AND COLUMN_NAME = 'available_qty');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_product_sales_line ADD COLUMN available_qty decimal(14,4) DEFAULT NULL COMMENT ''本仓可用量(建议快照)'' AFTER warehouse_name',
    'SELECT ''available_qty already exists''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
