-- =====================================================
-- V79: 入库单行加 PO 行引用 + 采购订单"退货"权限
-- 背景：
--   1. 过账回写 PO quantity_returned 需精确到 PO 行，但入库单行未记录 PO 行引用。
--      加 pur_order_line_id 列，入库时回填（从到货通知单行或按 itemId 匹配），
--      建立"入库行 → PO 行"关系。历史数据 NULL，回写时 fallback itemId。
--   2. 采购订单页新增"退货"入口，注册 mes:pur:order:return 权限按钮。
-- 幂等策略：加列用存储过程+information_schema 检查；菜单用 WHERE NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-07-18
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- Part 1: qxx_wm_item_recpt_line 加列（PO 行引用）
-- =====================================================

DROP PROCEDURE IF EXISTS proc_add_recpt_line_po_ref;
DELIMITER $$
CREATE PROCEDURE proc_add_recpt_line_po_ref() BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_wm_item_recpt_line' AND COLUMN_NAME='pur_order_line_id') THEN
        ALTER TABLE qxx_wm_item_recpt_line ADD COLUMN pur_order_line_id BIGINT NULL COMMENT '采购订单行ID(回写退货量精确匹配)';
    END IF;
END$$
DELIMITER ;
CALL proc_add_recpt_line_po_ref();
DROP PROCEDURE IF EXISTS proc_add_recpt_line_po_ref;


-- =====================================================
-- Part 2: 菜单 — 采购订单"退货"按钮（F 类型）
-- =====================================================

-- 找到采购订单头菜单ID（C 类型父菜单）
SELECT @purOrderMenuId := menu_id FROM sys_menu WHERE perms = 'mes:pur:order:list' AND menu_type = 'C' LIMIT 1;

-- 采购订单退货权限（从 PO 页发起退货，打开退货向导）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '采购订单退货', @purOrderMenuId, '8', '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:return', '#', 'admin', NOW(), '从采购订单发起退货'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:pur:order:return' AND menu_type = 'F');

-- 给管理员角色授权（role_id=1）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu
WHERE perms = 'mes:pur:order:return'
  AND menu_type = 'F'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.menu_id = sys_menu.menu_id AND rm.role_id = 1 AND rm.factory_id = 0
  );
