-- V63__ensure_product_recpt_menu.sql
-- 补建「产品入库」父级菜单 (menu_id=2204)
-- 背景:V61 假设 menu_id=2204 已存在,直接做 UPDATE。
-- 但生产环境从未初始化过该记录(旧版工单完工入库写 item_recpt,productrecpt 菜单被隐藏且表空),
-- 结果 V61 的 UPDATE 命中 0 行,只有 5 个子按钮 F 权限进库,父级 C 菜单缺失,侧边栏看不到入口。
-- 本迁移幂等补建 menu_id=2204,对齐前端页面路径 mes/wm/productrecpt/index。

-- ============================================================
-- 幂等插入产品入库父菜单 (parent_id=2002 仓库管理)
-- perms 对齐后端 @PreAuthorize: mes:wm:product_recpt:list (有下划线)
-- path 对齐前端路由: /wm/product_recpt (kebab-case,与采购入库 item_recpt 保持一致)
-- component 对齐前端页面: mes/wm/productrecpt/index (物理目录无下划线)
-- ============================================================
INSERT INTO sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time
)
SELECT
    2204, '产品入库', 2002, 3, 'product_recpt', 'mes/wm/productrecpt/index',
    1, 0, 'C', '0', '0', 'mes:wm:product_recpt:list', 'form',
    'admin', sysdate()
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2204);

-- 若已存在(测试环境或历史脏数据),强制对齐字段防止漂移
UPDATE sys_menu
SET menu_name = '产品入库',
    parent_id = 2002,
    path = 'product_recpt',
    component = 'mes/wm/productrecpt/index',
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'mes:wm:product_recpt:list',
    icon = 'form'
WHERE menu_id = 2204;
