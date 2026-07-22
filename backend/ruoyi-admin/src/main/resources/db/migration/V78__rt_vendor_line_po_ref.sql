-- =====================================================
-- V78: 采购退货单行加 PO 行追溯字段 + "从采购订单生成"菜单
-- 背景：
--   1. 退货行原仅靠 itemId 匹配 PO 行回写 quantity_returned，
--      依赖 V49 uk_order_item(order_id,item_id) 唯一约束。
--      为支持精确回写 + 展示"原采购/可退量"，加 3 个追溯列。
--   2. 新增"从采购订单快捷生成退货单"功能菜单按钮。
-- 幂等策略：加列用存储过程+information_schema 检查；菜单用 WHERE NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-07-17
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- Part 1: qxx_wm_rt_vendor_line 加列（PO 行追溯）
-- =====================================================

DROP PROCEDURE IF EXISTS proc_add_rt_vendor_line_po_cols;
DELIMITER $$
CREATE PROCEDURE proc_add_rt_vendor_line_po_cols() BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor_line' AND COLUMN_NAME='pur_order_line_id') THEN
        ALTER TABLE qxx_wm_rt_vendor_line ADD COLUMN pur_order_line_id BIGINT NULL COMMENT '采购订单行ID(精确回写退货量)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor_line' AND COLUMN_NAME='pur_order_line_no') THEN
        ALTER TABLE qxx_wm_rt_vendor_line ADD COLUMN pur_order_line_no VARCHAR(32) NULL COMMENT '采购订单行号(展示用)';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_rt_vendor_line' AND COLUMN_NAME='quantity_ordered') THEN
        ALTER TABLE qxx_wm_rt_vendor_line ADD COLUMN quantity_ordered DECIMAL(14,4) DEFAULT 0.0000 COMMENT '原采购数量(展示可退上限)';
    END IF;
END$$
DELIMITER ;
CALL proc_add_rt_vendor_line_po_cols();
DROP PROCEDURE IF EXISTS proc_add_rt_vendor_line_po_cols;


-- =====================================================
-- Part 2: 菜单 — "从采购订单生成"按钮（F 类型）
-- =====================================================

-- 找到采购退货单菜单ID（C 类型父菜单）
SELECT @rtVendorMenuId := menu_id FROM sys_menu WHERE perms = 'mes:wm:rt_vendor:list' AND menu_type = 'C' LIMIT 1;

-- 从采购订单生成退货单权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '从采购订单生成', @rtVendorMenuId, '9', '#', '', 1, 0, 'F', '0', '0', 'mes:wm:rt_vendor:fromPurOrder', '#', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:wm:rt_vendor:fromPurOrder' AND menu_type = 'F');

-- 给管理员角色授权（role_id=1）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu
WHERE perms = 'mes:wm:rt_vendor:fromPurOrder'
  AND menu_type = 'F'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.menu_id = sys_menu.menu_id AND rm.role_id = 1 AND rm.factory_id = 0
  );
