-- =====================================================================
-- V97: 物料分类动态扩展属性 - 注册按钮权限
-- 说明: V95/V96 引入了分类扩展属性配置(MdItemTypeController: attrBind/createAttrAndBind)
--       与属性字典查询(MdAttrDefController: list)，但未在 sys_menu 注册对应权限，
--       导致非超管用户看不到"扩展属性"按钮、接口 403。本迁移补齐按钮权限(F 类型)并授权 admin。
-- =====================================================================

-- 1. 分类"扩展属性"配置按钮（挂在 物料分类 menu_id=2102 下）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '分类扩展属性', 2102, 5, '#', '', 1, 0, 'F', '0', '0', 'mes:md:itemtype:attr', '#', 'admin', NOW(), '', NULL, '物料分类扩展属性配置'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:md:itemtype:attr' AND menu_type = 'F');

-- 2. 属性字典查询权限（配置弹窗"添加已有属性"下拉需要，隐式字典只读查询）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT '属性字典查询', 2102, 6, '#', '', 1, 0, 'F', '0', '0', 'mes:md:attrDef:list', '#', 'admin', NOW(), '', NULL, '物料扩展属性字典查询(配置弹窗用)'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'mes:md:attrDef:list' AND menu_type = 'F');

-- 3. 授权给超级管理员角色(role_id=1)
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu
WHERE (perms = 'mes:md:itemtype:attr' OR perms = 'mes:md:attrDef:list')
  AND menu_type = 'F'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm
      WHERE rm.menu_id = sys_menu.menu_id AND rm.role_id = 1 AND rm.factory_id = 0
  );
