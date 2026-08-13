-- ============================================================
-- V125: 销售订单「详情页」隐藏路由菜单
--
-- 背景：销售订单新增独立只读详情页（el-descriptions 展示头 + el-table 展示明细），
--      可查看审核人/审核时间/审核意见。详情页不进侧边栏（visible='1'），
--      仅从列表行「查看」按钮 router.push 进入。
--
-- 路由：挂在 2900（销售管理目录，path='sal'）下，与 2901 订单列表同级，
--      完整路径 /mes/sal/order_detail；component=mes/sal/order/detail。
-- 权限：复用 mes:sal:order:query（能看列表即可看详情，不新增权限点）。
-- 授权：给所有已拥有订单列表菜单(2901)的角色授予，保持 factory_id 隔离。
--
-- 幂等：INSERT ... SELECT ... WHERE NOT EXISTS
-- 日期：2026-08-13
-- ============================================================

SET NAMES utf8mb4;

-- ① 详情页菜单（C 类型，隐藏，挂销售管理目录 2900 下）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2917, '销售订单详情', 2900, 20, 'order_detail', 'mes/sal/order/detail', 1, 0, 'C', '1', '0', 'mes:sal:order:query', 'view', 'admin', sysdate(), '销售订单只读详情页（隐藏路由，列表查看进入）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2917);

-- ② 授权：给所有拥有订单列表(2901)的角色授予详情页菜单，沿用其 factory_id
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT rm.role_id, 2917, rm.factory_id
FROM sys_role_menu rm
WHERE rm.menu_id = 2901
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm2
    WHERE rm2.role_id = rm.role_id AND rm2.menu_id = 2917 AND rm2.factory_id = rm.factory_id
  );
