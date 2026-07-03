-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('节假日设置', 2007, '1', 'holiday', 'mes/cal/holiday/index', 1, 0, 'C', '0', '0', 'mes:cal:holiday:list', '#', 'admin', sysdate(), '', null, '节假日设置菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('节假日设置查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('节假日设置新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('节假日设置修改', @parentId, 2007,  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('节假日设置删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('节假日设置导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:holiday:export',       '#', 'admin', sysdate(), '', null, '');