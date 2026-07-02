-- ============================================================
-- MES 菜单种子数据（INSERT IGNORE，幂等安全，每次部署均可执行）
-- 说明：ry_20260417.sql 只包含 RuoYi 标准菜单，MES 菜单在此补充
-- ⚠️ 新增 MES 子模块菜单后，务必同步更新此文件
-- ============================================================
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, visible, status, menu_type, perms, icon, create_by, create_time) VALUES
-- ── 顶层：MES管理 ──
(2000, 'MES管理', 0, 5, 'mes', NULL, 0, 0, 'M', NULL, 'system', 'admin', NOW()),

-- ── MES 二级模块 ──
(2001, '主数据管理', 2000, 1, 'md', NULL, 0, 0, 'M', NULL, 'table', 'admin', NOW()),
(2002, '仓库管理',   2000, 2, 'wm', NULL, 0, 0, 'M', NULL, 'tree', 'admin', NOW()),
(2003, '生产管理',   2000, 3, 'pro', NULL, 0, 0, 'M', NULL, 'component', 'admin', NOW()),
(2004, '质量管理',   2000, 4, 'qc', NULL, 0, 0, 'M', NULL, 'monitor', 'admin', NOW()),
(2005, '设备管理',   2000, 5, 'dv', NULL, 0, 0, 'M', NULL, 'tool', 'admin', NOW()),
(2006, '采购管理',   2000, 6, 'pur', NULL, 0, 0, 'M', NULL, 'shopping', 'admin', NOW()),
(2007, '日历管理',   2000, 7, 'cal', NULL, 0, 0, 'M', NULL, 'date', 'admin', NOW()),
(2008, '工装管理',   2000, 8, 'tm', NULL, 0, 0, 'M', NULL, 'nested', 'admin', NOW());
