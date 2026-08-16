-- 集成测试 schema 补丁：种子 SQL 与 dev 库现状的少量列差异补齐。
-- 1) ry_20260417.sql 是旧基线种子，缺 V70/V98 给 sys_user 加的列
--    （Flyway baseline=136 跳过 V61-V136，qxx 表由 manual_tables.sql 提供现状，避免 V73 等 ALTER 重放冲突）
-- 2) manual_tables.sql 缺 V79 的 qxx_wm_item_recpt_line.pur_order_line_id、
--    V98 的 qxx_pro_material_trace.vendor_id（与迁移后 dev 库逐列 diff 核实）
-- 幂等：ALTER 前查 information_schema（与 V98 同一写法）。

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'openid');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN openid varchar(100) DEFAULT NULL COMMENT ''微信openid(小程序登录绑定)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'wage_type');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN wage_type varchar(20) DEFAULT NULL COMMENT ''工资类型:MONTHLY-月工资,PIECE-计件,HOURLY-计时''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'employee_type');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN employee_type varchar(20) DEFAULT NULL COMMENT ''员工类型:REGULAR-正式工,TEMPORARY-临时工''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'hire_date');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN hire_date date DEFAULT NULL COMMENT ''入职日期''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'vendor_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN vendor_id bigint(20) DEFAULT NULL COMMENT ''关联厂商ID(外协厂商员工账号绑定qxx_md_vendor;我方员工为NULL)'' AFTER factory_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_vendor_id');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE sys_user ADD INDEX idx_vendor_id (vendor_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_wm_item_recpt_line' AND column_name = 'pur_order_line_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_item_recpt_line ADD COLUMN pur_order_line_id bigint(20) DEFAULT NULL COMMENT ''采购订单行ID(关联qxx_pur_order_line,退货精确回写)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_material_trace' AND column_name = 'vendor_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_material_trace ADD COLUMN vendor_id bigint(20) DEFAULT NULL COMMENT ''供应商ID''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
