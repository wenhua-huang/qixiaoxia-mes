-- =====================================================
-- V74: 采购订单取消/强制关闭 + 采购退货单完善
-- 背景：补齐采购订单取消(CANCEL)、强制关闭(RECEIVING->CLOSED)、
--       采购退货单确认/过账回写PO(quantity_returned)三大能力
-- 幂等策略：加列用存储过程+information_schema检查；字典/菜单用WHERE NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-07-16
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- Part 1: 采购订单头表加列（取消/关闭相关）
-- =====================================================

DROP PROCEDURE IF EXISTS proc_add_pur_order_cancel_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_pur_order_cancel_cols() BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order' AND COLUMN_NAME='cancel_reason') THEN
        ALTER TABLE qxx_pur_order ADD COLUMN cancel_reason VARCHAR(50) NULL COMMENT '取消原因';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order' AND COLUMN_NAME='cancel_time') THEN
        ALTER TABLE qxx_pur_order ADD COLUMN cancel_time DATETIME NULL COMMENT '取消时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order' AND COLUMN_NAME='cancel_by') THEN
        ALTER TABLE qxx_pur_order ADD COLUMN cancel_by VARCHAR(64) NULL COMMENT '取消人';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order' AND COLUMN_NAME='close_reason') THEN
        ALTER TABLE qxx_pur_order ADD COLUMN close_reason VARCHAR(50) NULL COMMENT '关闭原因(强制关闭时填写)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order' AND COLUMN_NAME='close_time') THEN
        ALTER TABLE qxx_pur_order ADD COLUMN close_time DATETIME NULL COMMENT '关闭时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order' AND COLUMN_NAME='close_by') THEN
        ALTER TABLE qxx_pur_order ADD COLUMN close_by VARCHAR(64) NULL COMMENT '关闭人';
    END IF;
END$$
DELIMITER ;
CALL proc_add_pur_order_cancel_cols();
DROP PROCEDURE IF EXISTS proc_add_pur_order_cancel_cols;


-- =====================================================
-- Part 2: 采购订单行表加列（退货数量/取消/终止相关）
-- =====================================================

DROP PROCEDURE IF EXISTS proc_add_pur_order_line_cancel_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_pur_order_line_cancel_cols() BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order_line' AND COLUMN_NAME='quantity_returned') THEN
        ALTER TABLE qxx_pur_order_line ADD COLUMN quantity_returned DECIMAL(14,4) DEFAULT 0.0000 COMMENT '已退货数量(主单位)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order_line' AND COLUMN_NAME='cancel_reason') THEN
        ALTER TABLE qxx_pur_order_line ADD COLUMN cancel_reason VARCHAR(50) NULL COMMENT '取消/终止原因';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_pur_order_line' AND COLUMN_NAME='close_time') THEN
        ALTER TABLE qxx_pur_order_line ADD COLUMN close_time DATETIME NULL COMMENT '关闭/终止时间';
    END IF;
END$$
DELIMITER ;
CALL proc_add_pur_order_line_cancel_cols();
DROP PROCEDURE IF EXISTS proc_add_pur_order_line_cancel_cols;


-- =====================================================
-- Part 3: 采购退货单头表加列（PO关联/确认过账相关）
-- =====================================================

DROP PROCEDURE IF EXISTS proc_add_rt_vendor_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_rt_vendor_cols() BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor' AND COLUMN_NAME='pur_order_id') THEN
        ALTER TABLE qxx_wm_rt_vendor ADD COLUMN pur_order_id BIGINT NULL COMMENT '采购订单ID(冗余,加速回写)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor' AND COLUMN_NAME='pur_order_code') THEN
        ALTER TABLE qxx_wm_rt_vendor ADD COLUMN pur_order_code VARCHAR(64) NULL COMMENT '采购订单编码';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor' AND COLUMN_NAME='confirm_time') THEN
        ALTER TABLE qxx_wm_rt_vendor ADD COLUMN confirm_time DATETIME NULL COMMENT '确认时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor' AND COLUMN_NAME='confirm_by') THEN
        ALTER TABLE qxx_wm_rt_vendor ADD COLUMN confirm_by VARCHAR(64) NULL COMMENT '确认人';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor' AND COLUMN_NAME='post_time') THEN
        ALTER TABLE qxx_wm_rt_vendor ADD COLUMN post_time DATETIME NULL COMMENT '过账时间';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor' AND COLUMN_NAME='post_by') THEN
        ALTER TABLE qxx_wm_rt_vendor ADD COLUMN post_by VARCHAR(64) NULL COMMENT '过账人';
    END IF;
END$$
DELIMITER ;
CALL proc_add_rt_vendor_cols();
DROP PROCEDURE IF EXISTS proc_add_rt_vendor_cols;


-- =====================================================
-- Part 4: 字典 - 取消/关闭原因 mes_cancel_reason
-- =====================================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '取消/关闭原因', 'mes_cancel_reason', '0', 'admin', NOW(), '采购订单取消/终止收货/强制关闭原因'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_cancel_reason');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '供应商断供', 'SUPPLIER_STOP', 'mes_cancel_reason', '', 'danger', 'N', '0', 'admin', NOW(), '供应商无法供货'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_cancel_reason' AND dict_value = 'SUPPLIER_STOP');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '需求变更', 'DEMAND_CHANGE', 'mes_cancel_reason', '', 'warning', 'N', '0', 'admin', NOW(), '生产计划调整，不再需要'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_cancel_reason' AND dict_value = 'DEMAND_CHANGE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '重复下单', 'DUPLICATE', 'mes_cancel_reason', '', 'info', 'N', '0', 'admin', NOW(), '操作失误重复创建'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_cancel_reason' AND dict_value = 'DUPLICATE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '价格问题', 'PRICE_ISSUE', 'mes_cancel_reason', '', 'warning', 'N', '0', 'admin', NOW(), '与供应商价格未谈拢'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_cancel_reason' AND dict_value = 'PRICE_ISSUE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '供应商不再发货', 'SUPPLIER_NO_MORE', 'mes_cancel_reason', '', 'danger', 'N', '0', 'admin', NOW(), '供应商明确不再发剩余货物'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_cancel_reason' AND dict_value = 'SUPPLIER_NO_MORE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '质量不合格', 'QUALITY_ISSUE', 'mes_cancel_reason', '', 'danger', 'N', '0', 'admin', NOW(), '来料质量不合格'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_cancel_reason' AND dict_value = 'QUALITY_ISSUE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '其他', 'OTHER', 'mes_cancel_reason', '', 'info', 'N', '0', 'admin', NOW(), '其他原因'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_cancel_reason' AND dict_value = 'OTHER');


-- =====================================================
-- Part 5: 字典 - 退货单状态 mes_rt_status
-- =====================================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '退货单状态', 'mes_rt_status', '0', 'admin', NOW(), '采购退货单状态列表'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_rt_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '草稿', 'DRAFT', 'mes_rt_status', '', 'info', 'Y', '0', 'admin', NOW(), '退货单状态-草稿'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_rt_status' AND dict_value = 'DRAFT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已确认', 'CONFIRMED', 'mes_rt_status', '', 'warning', 'N', '0', 'admin', NOW(), '退货单状态-已确认'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_rt_status' AND dict_value = 'CONFIRMED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已过账', 'POSTED', 'mes_rt_status', '', 'success', 'N', '0', 'admin', NOW(), '退货单状态-已过账'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_rt_status' AND dict_value = 'POSTED');


-- =====================================================
-- Part 6: 菜单权限按钮（F类型）
-- =====================================================

-- 找到采购订单头菜单ID
SELECT @purOrderMenuId := menu_id FROM sys_menu WHERE perms = 'mes:pur:order:list' AND menu_type = 'C' LIMIT 1;

-- 采购订单取消权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '采购订单取消', @purOrderMenuId, '7', '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:cancel', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:pur:order:cancel' AND menu_type = 'F');

-- 找到采购退货单菜单ID
SELECT @rtVendorMenuId := menu_id FROM sys_menu WHERE perms = 'mes:wm:rt_vendor:list' AND menu_type = 'C' LIMIT 1;

-- 退货单确认权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退货单确认', @rtVendorMenuId, '7', '#', '', 1, 0, 'F', '0', '0', 'mes:wm:rt_vendor:confirm', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:rt_vendor:confirm' AND menu_type = 'F');

-- 退货单过账权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '退货单过账', @rtVendorMenuId, '8', '#', '', 1, 0, 'F', '0', '0', 'mes:wm:rt_vendor:post', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:rt_vendor:post' AND menu_type = 'F');

-- 给管理员角色授权（role_id=1）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu
WHERE perms IN ('mes:pur:order:cancel', 'mes:wm:rt_vendor:confirm', 'mes:wm:rt_vendor:post')
  AND menu_type = 'F'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.menu_id = sys_menu.menu_id AND rm.role_id = 1 AND rm.factory_id = 0
  );
