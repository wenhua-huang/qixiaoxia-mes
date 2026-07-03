-- ============================================================
-- V35: 设备管理菜单 (parent_id=2005)
-- ============================================================

-- 设备类型
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('设备类型', 2005, 1, 'machinerytype', 'mes/dv/machinerytype/index', 1, 0, 'C', '0', '0', 'mes:dv:machinerytype:list', '#', 'admin', sysdate(), '', NULL, '设备类型菜单');
SET @dv_type_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2005 AND perms = 'mes:dv:machinerytype:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('设备类型查询', @dv_type_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('设备类型新增', @dv_type_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('设备类型修改', @dv_type_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('设备类型删除', @dv_type_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinerytype:remove', '#', 'admin', sysdate(), '', NULL, '');

-- 设备档案
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('设备档案', 2005, 2, 'machinery', 'mes/dv/machinery/index', 1, 0, 'C', '0', '0', 'mes:dv:machinery:list', '#', 'admin', sysdate(), '', NULL, '设备档案菜单');
SET @dv_machinery_id = (SELECT MAX(menu_id) FROM sys_menu WHERE parent_id = 2005 AND perms = 'mes:dv:machinery:list');
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES ('设备档案查询', @dv_machinery_id, 1, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinery:query', '#', 'admin', sysdate(), '', NULL, ''),
       ('设备档案新增', @dv_machinery_id, 2, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinery:add', '#', 'admin', sysdate(), '', NULL, ''),
       ('设备档案修改', @dv_machinery_id, 3, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinery:edit', '#', 'admin', sysdate(), '', NULL, ''),
       ('设备档案删除', @dv_machinery_id, 4, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinery:remove', '#', 'admin', sysdate(), '', NULL, ''),
       ('设备档案导出', @dv_machinery_id, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:dv:machinery:export', '#', 'admin', sysdate(), '', NULL, '');
