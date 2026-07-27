-- V83: 销售出库单头表 warehouse_id 改为允许 NULL
-- 背景：仓库已下沉到 line（按仓拆行），头表不再承载仓库。
--      原 DDL 约束 warehouse_id NOT NULL 会阻断「从销售订单生成」时头表无仓库的插入。
-- 幂等：通过 information_schema 检查 IS_NULLABLE，已是 'YES' 则跳过。
SET @col_nullable := (SELECT IS_NULLABLE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_product_sales' AND COLUMN_NAME = 'warehouse_id');
SET @sql := IF(@col_nullable = 'NO',
    'ALTER TABLE qxx_wm_product_sales MODIFY COLUMN warehouse_id bigint(20) NULL COMMENT ''仓库ID(已下沉到行,头表可空)''',
    'SELECT ''warehouse_id already nullable''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
