-- ============================================================
-- V126: 采购订单「详情页」+「明细导出」菜单/权限
--
-- 背景：采购订单新增"导出详情为 PDF/Excel"功能（对外采购单 / 对内对账），
--      以及独立只读详情页（列表查看进入）。对齐销售订单 V123/V125 范式。
--
-- 含：① 明细导出按钮权限 mes:pur:order:exportDetail（F 类型，挂在采购订单列表菜单下）
--    ② admin 授权
--    ③ 详情页隐藏路由菜单（C 类型，挂采购管理目录 2006 下，复用 query 权限）
--    ④ 给所有拥有采购订单列表菜单的角色授予详情页菜单（保持 factory_id 隔离）
--
-- 注意：采购订单列表菜单 menu_id 为自增（V22/V34），不同环境值不同，
--      故按钮 parent_id 用 SELECT 动态取；详情页 parent_id=2006（采购管理目录，seed 硬编码稳定）。
--
-- 幂等：INSERT ... SELECT ... WHERE NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-08-13
-- ============================================================

SET NAMES utf8mb4;

-- 动态取采购订单列表菜单ID（自增，不同环境不同）
SELECT @purOrderMenuId := menu_id FROM sys_menu
WHERE perms = 'mes:pur:order:list' AND menu_type = 'C' LIMIT 1;

-- ① 采购订单明细导出按钮（F 类型，挂在采购订单列表菜单下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2920, '采购订单明细导出', @purOrderMenuId, 8, 'F', '0', '0', 'mes:pur:order:exportDetail', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2920);

-- ② admin 角色授权（sys_role_menu 主键含 factory_id，全局为 0）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 2920, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2920 AND factory_id = 0);

-- ③ 详情页菜单（C 类型，隐藏，挂采购管理目录 2006 下）
--    完整路由 /mes/pur/order_detail；component=mes/pur/order/detail
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2921, '采购订单详情', 2006, 20, 'order_detail', 'mes/pur/order/detail', 1, 0, 'C', '1', '0', 'mes:pur:order:query', 'view', 'admin', sysdate(), '采购订单只读详情页（隐藏路由，列表查看进入）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2921);

-- ④ 授权：给所有拥有采购订单列表菜单的角色授予详情页菜单，沿用其 factory_id
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT rm.role_id, 2921, rm.factory_id
FROM sys_role_menu rm
WHERE rm.menu_id = @purOrderMenuId
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm2
    WHERE rm2.role_id = rm.role_id AND rm2.menu_id = 2921 AND rm2.factory_id = rm.factory_id
  );
