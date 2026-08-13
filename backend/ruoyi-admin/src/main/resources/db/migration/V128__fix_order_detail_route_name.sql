-- ============================================================
-- V128: 修复销售/采购订单详情页路由 name 冲突
--
-- 背景：V125(销售详情 2917) 与 V126(采购详情 2921) 的 path 均为 order_detail，
--      且都未配置 route_name。后端 SysMenuServiceImpl.getRouteName() 在
--      route_name 为空时用 StringUtils.capitalize(path) 生成路由名，
--      导致两个详情页的 Vue Router name 都是 "Order_detail"。
--
--      Vue Router 4 注册同名路由时，后注册的会把先注册的整条路由记录移除
--      （path 一起移除）。菜单 order_num 上采购(2006)在销售(2900)之前，
--      故销售详情注册在后、顶掉了采购详情 → 采购详情页 404。
--
-- 修复：给两个详情页分别设置唯一 route_name，与现有 SalOrder(销售列表)
--      命名风格保持一致（大驼峰）。前端跳转均用 path，无按 name 引用，改名安全。
--
-- 幂等：UPDATE ... WHERE（可重复执行）
-- 字符集：utf8mb4
-- 日期：2026-08-13
-- ============================================================

SET NAMES utf8mb4;

-- 销售订单详情 → SalOrderDetail
UPDATE sys_menu SET route_name = 'SalOrderDetail', update_by = 'admin', update_time = sysdate()
WHERE menu_id = 2917 AND (route_name IS NULL OR route_name = '');

-- 采购订单详情 → PurOrderDetail
UPDATE sys_menu SET route_name = 'PurOrderDetail', update_by = 'admin', update_time = sysdate()
WHERE menu_id = 2921 AND (route_name IS NULL OR route_name = '');
