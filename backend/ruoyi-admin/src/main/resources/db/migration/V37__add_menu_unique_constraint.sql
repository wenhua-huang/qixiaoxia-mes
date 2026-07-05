-- ============================================================
-- V37: 添加 sys_menu 唯一约束，彻底防止重复菜单
-- 问题：sys_menu 只有 PRIMARY KEY(menu_id)，INSERT IGNORE 无法防重
-- 修复：先清重复 → 统一 perms='' 为 NULL → 加唯一约束
-- ============================================================

-- 1. 统一空 perms 为 NULL（'' 和 NULL 在业务上等价）
UPDATE sys_menu SET perms = NULL WHERE perms = '';

-- 2. 删除重复菜单（保留 menu_id 最小的）
DELETE t1 FROM sys_menu t1
INNER JOIN sys_menu t2
ON t1.parent_id = t2.parent_id
   AND ((t1.perms = t2.perms) OR (t1.perms IS NULL AND t2.perms IS NULL))
WHERE t1.menu_id > t2.menu_id;

-- 3. 添加唯一约束 — 同一父菜单下权限标识唯一
ALTER TABLE sys_menu ADD UNIQUE KEY uk_parent_perms (parent_id, perms);
