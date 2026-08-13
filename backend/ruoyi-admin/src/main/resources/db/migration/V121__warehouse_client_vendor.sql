-- V121__warehouse_client_vendor.sql
-- 客户/供应商专属仓库：仓库表加归属字段，产品入库表加客户字段

-- 仓库表：客户仓归属
SET @s = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_warehouse' AND COLUMN_NAME = 'client_id') = 0,
    'ALTER TABLE qxx_wm_warehouse ADD COLUMN client_id bigint(20) DEFAULT NULL COMMENT ''客户仓归属客户ID(warehouse_type=CUSTOMER时必填)'' AFTER warehouse_type',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 仓库表：供应商仓归属
SET @s = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_warehouse' AND COLUMN_NAME = 'vendor_id') = 0,
    'ALTER TABLE qxx_wm_warehouse ADD COLUMN vendor_id bigint(20) DEFAULT NULL COMMENT ''供应商仓归属供应商ID(warehouse_type=SUPPLIER时必填)'' AFTER client_id',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 产品入库表：成品归属客户（来源工单）
SET @s = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_product_recpt' AND COLUMN_NAME = 'client_id') = 0,
    'ALTER TABLE qxx_wm_product_recpt ADD COLUMN client_id bigint(20) DEFAULT NULL COMMENT ''成品归属客户ID(来源工单)'' AFTER workorder_code',
    'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 仓库类型字典 mes_warehouse_type 补 CUSTOMER/SUPPLIER（系统级字典，无 factory_id；dict_code 自增省略，按 dict_value 幂等）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '客户仓', 'CUSTOMER', 'mes_warehouse_type', '', 'success', 'N', '0', 'admin', NOW(), '仓库类型-客户仓'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_warehouse_type' AND dict_value='CUSTOMER');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '供应商仓', 'SUPPLIER', 'mes_warehouse_type', '', 'warning', 'N', '0', 'admin', NOW(), '仓库类型-供应商仓'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_warehouse_type' AND dict_value='SUPPLIER');
