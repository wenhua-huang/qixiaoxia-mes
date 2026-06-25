-- ============================================================
-- 工单变更管理 菜单种子数据
-- 用途：为 workorderchange 页面注册路由
-- 权限：复用 mes:pro:workorder:* 权限（无需新增按钮）
-- 执行方式：mysql -h localhost -P 3307 -u root -pqxx123456 mes < seed_pro_workorderchange_menu.sql
-- ============================================================

-- 工单变更管理 C-menu（页面菜单，parent=2003 生产管理）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2305, '工单变更', 2003, 6, 'workorderchange', 'mes/pro/workorderchange/index', 1, 0, 'C', '0', '0', 'mes:pro:workorder:list', '#', 'admin', sysdate());

-- 说明：工单变更管理复用生产工单的现有权限按钮（query/add/edit/remove/export），无需新增 F-menu。
-- ProWorkorderChangeController 中 @PreAuthorize 已声明使用 mes:pro:workorder:* 权限。
