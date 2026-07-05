-- ============================================================
-- V39: 恢复 menu_id=2000 (MES管理顶层菜单)
-- V37 误删了 2000（与 menu_id=1 同为 parent_id=0, perms=NULL，
-- 被识别为重复并保留了 menu_id 最小的 1）
-- 没有 2000 作为父节点，所有子模块(2001-2009)在菜单树中不可见
-- ============================================================
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (2000, 'MES管理', 0, 5, 'mes', NULL, 1, 0, 'M', '0', '0', NULL, 'system', 'admin', NOW(), '', NULL, '');
