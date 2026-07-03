-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('班组设置', 2007, '1', 'team', 'mes/cal/team/index', 1, 0, 'C', '0', '0', 'mes:cal:team:list', '#', 'admin', sysdate(), '', null, '班组菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('班组查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('班组新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('班组修改', @parentId, 2007,  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('班组删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('班组导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:team:export',       '#', 'admin', sysdate(), '', null, '');