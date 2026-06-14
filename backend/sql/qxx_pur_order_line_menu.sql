-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单行', '3', '1', 'line', 'mes/pur/order-line/index', 1, 0, 'C', '0', '0', 'mes:pur:order-line:list', '#', 'admin', sysdate(), '', null, '采购订单行菜单');

-- 按钮父菜单ID
SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单行查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单行新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单行修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单行删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('采购订单行导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'mes:pur:order-line:export',       '#', 'admin', sysdate(), '', null, '');