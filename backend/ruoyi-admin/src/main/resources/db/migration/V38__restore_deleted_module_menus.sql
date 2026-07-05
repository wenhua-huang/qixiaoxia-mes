-- ============================================================
-- V38: 紧急修复 — V37 误删了 parent_id=2000 下的 MES 模块菜单
-- V37 的 DELETE 条件对 NULL perms 处理不当，保留了 menu_id 最小的(2001)，
-- 删除了 2002-2009。本迁移恢复它们。
-- ============================================================

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES
(2002, '仓库管理',   2000, 2, 'wm',  NULL, 1, 0, 'M', '0', '0', NULL, 'tree', 'admin', NOW(), '', NULL, ''),
(2003, '生产管理',   2000, 3, 'pro', NULL, 1, 0, 'M', '0', '0', NULL, 'component', 'admin', NOW(), '', NULL, ''),
(2004, '质量管理',   2000, 4, 'qc',  NULL, 1, 0, 'M', '0', '0', NULL, 'guide', 'admin', NOW(), '', NULL, ''),
(2005, '设备管理',   2000, 5, 'dv',  NULL, 1, 0, 'M', '0', '0', NULL, 'tool', 'admin', NOW(), '', NULL, ''),
(2006, '采购管理',   2000, 6, 'pur', NULL, 1, 0, 'M', '0', '0', NULL, 'shopping', 'admin', NOW(), '', NULL, ''),
(2007, '日历管理',   2000, 7, 'cal', NULL, 1, 0, 'M', '0', '0', NULL, 'date', 'admin', NOW(), '', NULL, ''),
(2008, '工装管理',   2000, 8, 'tm',  NULL, 1, 0, 'M', '0', '0', NULL, 'tool', 'admin', NOW(), '', NULL, ''),
(2009, '系统管理',   2000, 9, 'sys', NULL, 1, 0, 'M', '0', '0', NULL, 'system', 'admin', NOW(), '', NULL, '');
