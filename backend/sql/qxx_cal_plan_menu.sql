-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('排班计划', '3', '1', 'plan', 'mes/plan/index', 1, 0, 'C', '0', '0', 'mes:cal:plan:list', '#', 'admin', sysdate(), '', null, '排班计划菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('排班计划查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('排班计划新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('排班计划修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('排班计划删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('排班计划导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:cal:plan:export',       '#', 'admin', sysdate(), '', null, '');