-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具类型', '3', '1', 'tool-type', 'mes/tm/tool-type/index', 1, 0, 'C', '0', '0', 'mes:tm:tool-type:list', '#', 'admin', sysdate(), '', null, '工装夹具类型菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具类型查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool-type:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具类型新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool-type:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具类型修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool-type:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具类型删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool-type:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('工装夹具类型导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:tm:tool-type:export',       '#', 'admin', sysdate(), '', null, '');
