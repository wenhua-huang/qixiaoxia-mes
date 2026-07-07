-- ============================================================
-- 采购订单状态流转 — 新增审批/下单/关闭按钮权限
-- 依赖：V22__qxx_pur_order_menu.sql（采购订单头菜单已存在）
-- ============================================================

-- 找到采购订单头菜单ID
SELECT @purOrderMenuId := menu_id FROM sys_menu WHERE perms = 'mes:pur:order:list' AND menu_type = 'C' LIMIT 1;

-- 审批按钮
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头审批', @purOrderMenuId, '6', '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:approve', '#', 'admin', sysdate(), '', null, '');

-- 下达按钮
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头下达', @purOrderMenuId, '7', '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:order', '#', 'admin', sysdate(), '', null, '');

-- 关闭按钮
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单头关闭', @purOrderMenuId, '8', '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order:close', '#', 'admin', sysdate(), '', null, '');
