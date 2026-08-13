-- V130__warehouse_split_ownership_type.sql
-- 拆分 warehouse_type 为两个正交维度：
--   warehouse_type  回归内容维度（RAW/FINISHED/AUX/LINE/TEMP，纯标签）
--   ownership_type  归属维度（PUBLIC-公共仓/CUSTOMER-客户仓/SUPPLIER-供应商仓，驱动隔离逻辑）
-- 旧数据回填：warehouse_type=CUSTOMER → ownership_type=CUSTOMER, warehouse_type=FINISHED
--            warehouse_type=SUPPLIER → ownership_type=SUPPLIER, warehouse_type=RAW
--            其余 → ownership_type=PUBLIC（DEFAULT 已覆盖）

-- 1. 新增 ownership_type 列（幂等）
DROP PROCEDURE IF EXISTS proc_add_warehouse_ownership_type;
DELIMITER $$
CREATE PROCEDURE proc_add_warehouse_ownership_type()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_warehouse'
                   AND COLUMN_NAME = 'ownership_type') THEN
        ALTER TABLE qxx_wm_warehouse
          ADD COLUMN ownership_type varchar(20) NOT NULL DEFAULT 'PUBLIC'
          COMMENT '归属类型:PUBLIC-公共仓,CUSTOMER-客户仓,SUPPLIER-供应商仓'
          AFTER warehouse_type;
    END IF;
END$$
DELIMITER ;
CALL proc_add_warehouse_ownership_type();
DROP PROCEDURE IF EXISTS proc_add_warehouse_ownership_type;

-- 2. 回填旧数据：CUSTOMER/SUPPLIER 从 warehouse_type 迁到 ownership_type，warehouse_type 复位为内容类型
--    幂等：迁移后 warehouse_type 不再有 CUSTOMER/SUPPLIER 值，重跑 WHERE 不命中
UPDATE qxx_wm_warehouse
SET ownership_type = 'CUSTOMER',
    warehouse_type = 'FINISHED',
    update_time = NOW()
WHERE warehouse_type = 'CUSTOMER';

UPDATE qxx_wm_warehouse
SET ownership_type = 'SUPPLIER',
    warehouse_type = 'RAW',
    update_time = NOW()
WHERE warehouse_type = 'SUPPLIER';

-- 3. 更新 client_id/vendor_id 列注释（引用 ownership_type 而非 warehouse_type）
ALTER TABLE qxx_wm_warehouse
    MODIFY COLUMN client_id bigint(20) DEFAULT NULL COMMENT '客户仓归属客户ID(ownership_type=CUSTOMER时必填)',
    MODIFY COLUMN vendor_id bigint(20) DEFAULT NULL COMMENT '供应商仓归属供应商ID(ownership_type=SUPPLIER时必填)';

-- 4. 性能索引：按工厂+归属+归属实体查专属仓（非唯一，保留多座专属仓能力）
DROP PROCEDURE IF EXISTS proc_add_warehouse_owner_idx;
DELIMITER $$
CREATE PROCEDURE proc_add_warehouse_owner_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_warehouse'
                   AND INDEX_NAME = 'idx_factory_owner_client') THEN
        ALTER TABLE qxx_wm_warehouse
          ADD KEY idx_factory_owner_client (factory_id, ownership_type, client_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qxx_wm_warehouse'
                   AND INDEX_NAME = 'idx_factory_owner_vendor') THEN
        ALTER TABLE qxx_wm_warehouse
          ADD KEY idx_factory_owner_vendor (factory_id, ownership_type, vendor_id);
    END IF;
END$$
DELIMITER ;
CALL proc_add_warehouse_owner_idx();
DROP PROCEDURE IF EXISTS proc_add_warehouse_owner_idx;

-- 5. 字典：新建 mes_warehouse_owner_type（归属类型，3 项）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '仓库归属类型', 'mes_warehouse_owner_type', '0', 'admin', NOW(), '仓库归属:公共/客户/供应商'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_warehouse_owner_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '公共仓', 'PUBLIC', 'mes_warehouse_owner_type', '', 'info', 'Y', '0', 'admin', NOW(), '归属类型-公共仓'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_warehouse_owner_type' AND dict_value='PUBLIC');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '客户仓', 'CUSTOMER', 'mes_warehouse_owner_type', '', 'success', 'N', '0', 'admin', NOW(), '归属类型-客户仓'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_warehouse_owner_type' AND dict_value='CUSTOMER');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '供应商仓', 'SUPPLIER', 'mes_warehouse_owner_type', '', 'warning', 'N', '0', 'admin', NOW(), '归属类型-供应商仓'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_warehouse_owner_type' AND dict_value='SUPPLIER');

-- 6. 从 mes_warehouse_type 删除 V129 加的 CUSTOMER/SUPPLIER（恢复为纯内容字典 RAW/FINISHED/AUX/LINE/TEMP）
DELETE FROM sys_dict_data WHERE dict_type = 'mes_warehouse_type' AND dict_value IN ('CUSTOMER', 'SUPPLIER');
