-- ============================================================
-- V33: 清理生产环境错误菜单数据
-- 问题：旧版 SQL 使用 parent_id='3'（系统工具）导致 MES 菜单挂在错误父节点，
--       且多次部署产生重复记录
-- 修复：删除错误位置的 MES 菜单及重复记录
--       V22-V32 的 INSERT IGNORE 会在迁移中重新插入到正确 parent_id
-- ============================================================

-- Step 1: 找出要删除的 MES 菜单 ID（parent_id=3 下的）
SET @purge_ids = (SELECT GROUP_CONCAT(menu_id) FROM (
    SELECT menu_id FROM sys_menu WHERE parent_id = 3 AND perms LIKE 'mes:%'
) AS tmp);

-- Step 2: 先删除这些菜单的子按钮权限
DELETE FROM sys_menu WHERE FIND_IN_SET(parent_id, @purge_ids) > 0;

-- Step 3: 再删除这些 MES 菜单本身
DELETE FROM sys_menu WHERE parent_id = 3 AND perms LIKE 'mes:%';

-- Step 4: 删除仓库管理(parent_id=2002)下的重复菜单子按钮（第二次部署产生，menu_id 23269~23282）
DELETE FROM sys_menu WHERE parent_id BETWEEN 23269 AND 23282;

-- Step 5: 删除重复的仓库管理菜单
DELETE FROM sys_menu WHERE menu_id BETWEEN 23269 AND 23282;
