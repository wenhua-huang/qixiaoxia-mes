-- 菜单 SQL
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班次', 2007, '1', 'shift', 'mes/cal/shift/index', 1, 0, 'C', '0', '0', 'mes:cal:shift:list', '#', 'admin', sysdate(), '', null, '计划班次菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班次查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:query',        '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班次新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:add',          '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班次修改', @parentId, 2007,  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:edit',         '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班次删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:remove',       '#', 'admin', sysdate(), '', null, '');

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('计划班次导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:shift:export',       '#', 'admin', sysdate(), '', null, '');