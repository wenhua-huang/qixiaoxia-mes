-- V154__pro_task_release_perm.sql
-- 质检拦截放行按钮权限：默认不授任何角色（超管 *:*:* 天然拥有），后续在角色管理自行分配。
-- sys_menu 无 factory_id，此处不写 factory_id；NOT EXISTS 幂等，不插任何角色授权。
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '质检拦截放行', m.menu_id, 20, '', NULL, 1, 0, 'F', '0', '0',
       'mes:pro:task:release', '#', 'admin', now(), '跟单检验不合格时放行道工序报工'
FROM sys_menu m
WHERE m.perms = 'mes:pro:task:list' AND m.menu_type = 'C'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:pro:task:release');
