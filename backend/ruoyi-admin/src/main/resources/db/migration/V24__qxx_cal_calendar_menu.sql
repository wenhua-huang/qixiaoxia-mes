-- 排班日历菜单
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('排班日历', 2007, '1', 'calendar', 'mes/cal/calendar/index', 1, 0, 'C', '0', '0', 'mes:cal:calendar:list', '#', 'admin', sysdate(), '', null, '排班日历菜单');

SELECT @parentId := LAST_INSERT_ID();

INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('排班日历查询', @parentId, '1', '#', '', 1, 0, 'F', '0', '0', 'mes:cal:calendar:query', '#', 'admin', sysdate(), '', null, '');
