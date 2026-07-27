-- =====================================================
-- V85: 修复采购退货单按钮菜单 parent_id 为 NULL 的问题
-- =====================================================
-- 背景：
--   V74/V78 迁移中 SELECT @rtVendorMenuId 使用 perms='mes:wm:rt_vendor:list'，
--   但 DB 实际存的权限是 'mes:wm:rtvendor:list'（无下划线），
--   导致 3 条按钮菜单（confirm/post/fromPurOrder）parent_id 为 NULL，
--   进而触发 SysMenuServiceImpl.getChildList() 的 NPE（getParentId().longValue()）。
--
-- 修复：将这 3 条菜单的 parent_id 指向正确的父菜单（mes:wm:rtvendor:list）。
-- 幂等：WHERE parent_id IS NULL 确保重复执行不影响已修复数据。

-- 找到采购退货单父菜单 ID（perms 无下划线版本）
SELECT @rtVendorMenuId := menu_id FROM sys_menu
WHERE perms = 'mes:wm:rtvendor:list' AND menu_type = 'C' LIMIT 1;

-- 仅当找到父菜单且子菜单 parent_id 为 NULL 时修复
-- 注意：@rtVendorMenuId 可能为 NULL（找不到父菜单），此时不更新，避免把 parent_id 设回 NULL
UPDATE sys_menu
SET parent_id = @rtVendorMenuId
WHERE perms IN ('mes:wm:rt_vendor:confirm', 'mes:wm:rt_vendor:post', 'mes:wm:rt_vendor:fromPurOrder')
  AND menu_type = 'F'
  AND parent_id IS NULL
  AND @rtVendorMenuId IS NOT NULL;
