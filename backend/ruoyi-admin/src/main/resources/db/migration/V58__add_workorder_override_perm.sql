-- ============================================================
-- V58: 新增「豁免开工」按钮权限 mes:pro:workorder:override
-- 用途：开工四步检查中排产检查 FAIL 时，授权此权限的角色可 force=true 豁免开工
--       （物料齐套为硬约束，不可豁免；仅排产可豁免，豁免记入工单变更记录）
-- parent_id 动态取工单 C-menu（perms=mes:pro:workorder:list），避免硬编码 menu_id
-- 幂等：NOT EXISTS 防重；sys_menu 无 factory_id（系统表，全局共享）
-- ============================================================

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT '豁免开工',
       (SELECT menu_id FROM sys_menu WHERE perms = 'mes:pro:workorder:list' AND menu_type = 'C' LIMIT 1),
       20, '', NULL, 1, 0, 'F', '0', '0', 'mes:pro:workorder:override', '#', 'admin', sysdate()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE perms = 'mes:pro:workorder:override'
);
