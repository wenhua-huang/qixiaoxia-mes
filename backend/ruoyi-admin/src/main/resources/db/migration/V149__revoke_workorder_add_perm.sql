-- ============================================================
-- V149: 回收非 admin 角色的「生产工单新增」按钮权限
-- 业务规则：生产工单只能由销售订单「生成生产工单」链路创建
--   （入口 POST /mes/sal/order/toWorkorder，独立权限点 mes:sal:order:workorder），
--   生产工单列表页的手工「新增」入口（mes:pro:workorder:add）仅对超级管理员保留，
--   杜绝同一批活从两条平行入口重复开单。
-- 说明：
--   1. admin(user_id=1) 登录即持 *:*:* 通配权限，@ss.hasPermi 恒放行，
--      不依赖 sys_role_menu 授权行，故 role_id=1 的行保留不动也无影响；
--   2. 不限定 factory_id：历史授权 factory_id 混用 0/1，需全部回收；DELETE 天然幂等；
--   3. menu_id 不硬编码（23002 仅存在于已删除的手工脚本 init_shengxiang.sql），
--      按 perms + menu_type='F' 动态定位，仿 V58/V113 先例；
--   4. F 按钮菜单定义本身保留，管理员日后可在「系统管理-角色」中按需重新授权。
-- ============================================================

DELETE rm FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE m.menu_type = 'F'
  AND m.perms = 'mes:pro:workorder:add'
  AND rm.role_id <> 1;
