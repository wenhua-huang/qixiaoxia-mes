-- ============================================================
-- 甘特图排产功能 — 菜单/权限种子数据
-- 日期：2026-06-27
-- ============================================================

-- 1. 甘特图排产 (menu_id=2311, parent=2003 生产管理)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2311, '甘特图排产', 2003, 4, 'gantt', 'mes/pro/gantt/index', 1, 0, 'C', '0', '0', 'mes:pro:gantt:query', 'chart', 'admin', sysdate());

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23111, '排产查询', 2311, 1, 'F', '0', '0', 'mes:pro:gantt:query',  'admin', sysdate()),
(23112, '排产计算', 2311, 2, 'F', '0', '0', 'mes:pro:gantt:schedule','admin', sysdate()),
(23113, '排产编辑', 2311, 3, 'F', '0', '0', 'mes:pro:gantt:edit',    'admin', sysdate());

-- 2. 换型时间配置 (menu_id=2312, parent=2003 生产管理)
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2312, '换型时间', 2003, 5, 'changeover', 'mes/pro/changeover/index', 1, 0, 'C', '0', '0', 'mes:pro:changeover:list', 'tool', 'admin', sysdate());

INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(23121, '换型查询', 2312, 1, 'F', '0', '0', 'mes:pro:changeover:query', 'admin', sysdate()),
(23122, '换型新增', 2312, 2, 'F', '0', '0', 'mes:pro:changeover:add',   'admin', sysdate()),
(23123, '换型修改', 2312, 3, 'F', '0', '0', 'mes:pro:changeover:edit',  'admin', sysdate()),
(23124, '换型删除', 2312, 4, 'F', '0', '0', 'mes:pro:changeover:remove','admin', sysdate()),
(23125, '换型导出', 2312, 5, 'F', '0', '0', 'mes:pro:changeover:export','admin', sysdate());

-- 3. 授予 admin 角色 (role_id=1) 新菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id IN (2311, 23111, 23112, 23113, 2312, 23121, 23122, 23123, 23124, 23125);

-- 4. 授予生产角色 (role_id=11, sx_production) 新菜单权限
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 11, menu_id FROM sys_menu WHERE menu_id IN (2311, 23111, 23112, 23113, 2312, 23121);
