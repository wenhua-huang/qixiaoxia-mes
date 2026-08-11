-- ═══════════════════════════════════════════════════════════════════════
-- V103 外协草稿状态 + 执行发料权限
--
-- 背景：外协工序自动发料改为草稿模式（开工时建 DRAFT 不扣料，用户确认发料行后
--       executeOutsource 执行扣料 DRAFT→ISSUED）。需新增 DRAFT 字典值 + execute 权限按钮。
--
-- 注：sys_dict_*/sys_menu/sys_role_menu 系统表无 factory_id（Flyway 裸 JDBC，手写幂等）。
-- @author qixiaoxia
-- ═══════════════════════════════════════════════════════════════════════

-- 1. 外协状态字典新增 DRAFT（草稿，排到最前）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 0, '草稿', 'DRAFT', 'mes_outsource_status', '', 'info', 'N', '0', 'admin', NOW(), '草稿单，发料行可改，执行发料后才扣库存'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_status' AND dict_value = 'DRAFT');

-- 2. 执行发料权限按钮（挂在外协管理菜单下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28805, '外协执行发料', 28800, 5, 'F', '0', '0', 'mes:wm:outsource:execute', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28805);

-- 3. 角色授权（admin=1 factory_id=0；产线=11 factory_id=1；厂商=106 不授执行发料）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 28805, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = 28805 AND rm.factory_id = 0);

INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, 28805, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 11 AND rm.menu_id = 28805 AND rm.factory_id = 1);
