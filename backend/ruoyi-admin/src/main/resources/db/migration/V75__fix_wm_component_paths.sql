-- =====================================================
-- V75: 修复仓库管理菜单的 component 路径
-- 背景：V36 将 path（URL 路径）从旧格式更新为下划线格式，
--       但 component 字段未同步更新，导致前端 loadView()
--       无法匹配到实际文件，页面显示"模块缺失"（404）
-- 幂等策略：WHERE component = 'old_path' 确保只更新一次
-- 字符集：utf8mb4
-- 日期：2026-07-16
-- =====================================================

SET NAMES utf8mb4;

-- V36 更新了 path 但遗留了 component 的 9 个菜单：
-- location → storage_location, area → storage_area,
-- takingplan → stock_taking_plan, rtvendor → rt_vendor,
-- rtsales → rt_sales, rolldetail → roll_detail,
-- miscrecpt → misc_recpt, miscissue → misc_issue,
-- itemrecpt → item_recpt

-- 注意：前端实际目录结构使用下划线（如 rt_vendor/、storage_location/），
-- 所以 component 必须与目录名完全一致，否则 loadView 匹配失败。

UPDATE sys_menu SET
    component = 'mes/wm/storage_location/index'
WHERE parent_id = 2002 AND component = 'mes/wm/location/index';

UPDATE sys_menu SET
    component = 'mes/wm/storage_area/index'
WHERE parent_id = 2002 AND component = 'mes/wm/area/index';

UPDATE sys_menu SET
    component = 'mes/wm/stock_taking_plan/index'
WHERE parent_id = 2002 AND component = 'mes/wm/takingplan/index';

UPDATE sys_menu SET
    component = 'mes/wm/rt_vendor/index'
WHERE parent_id = 2002 AND component = 'mes/wm/rtvendor/index';

UPDATE sys_menu SET
    component = 'mes/wm/rt_sales/index'
WHERE parent_id = 2002 AND component = 'mes/wm/rtsales/index';

UPDATE sys_menu SET
    component = 'mes/wm/roll_detail/index'
WHERE parent_id = 2002 AND component = 'mes/wm/rolldetail/index';

UPDATE sys_menu SET
    component = 'mes/wm/misc_recpt/index'
WHERE parent_id = 2002 AND component = 'mes/wm/miscrecpt/index';

UPDATE sys_menu SET
    component = 'mes/wm/misc_issue/index'
WHERE parent_id = 2002 AND component = 'mes/wm/miscissue/index';

UPDATE sys_menu SET
    component = 'mes/wm/item_recpt/index'
WHERE parent_id = 2002 AND component = 'mes/wm/itemrecpt/index';