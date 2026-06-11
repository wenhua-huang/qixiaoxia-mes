-- ============================================================
-- 企小侠文化纸盒MES系统 — 圣享工厂初始化数据
-- 版本: v1.0
-- 日期: 2026-06-10
-- 数据库: MySQL 8.0+, 字符集 utf8mb4
-- 说明: 部门/岗位/角色/用户/MES菜单 全量初始化
-- 依赖: 需先执行 ry_20260417.sql(系统表) + mes-md.sql(工厂表)
-- ============================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- Part 0: 清理（支持重复执行，先删后建）
-- ============================================================
DELETE FROM sys_role_menu WHERE menu_id >= 2000;
DELETE FROM sys_menu WHERE menu_id >= 2000;

-- ============================================================
-- Part 1: 工厂数据
-- ============================================================

-- 如果工厂已存在则忽略
INSERT IGNORE INTO qxx_md_factory (factory_id, factory_code, factory_name, short_name, enable_flag, create_by, create_time, update_by, update_time)
VALUES (1, 'SX', '圣享工厂', '圣享', '1', 'admin', sysdate(), '', sysdate());

-- ============================================================
-- Part 1.5: 计量单位初始化（qxx_md_unit_measure）
-- 说明：圣享工厂纸袋生产所需计量单位，主单位 primary_unit 为空
-- ============================================================

DELETE FROM qxx_md_unit_measure WHERE factory_id = 1;

-- ### 1.5.1 主单位（primary_unit 为空）###
INSERT INTO qxx_md_unit_measure (unit_id, factory_id, unit_code, unit_name, primary_unit, conversion_rate, enable_flag, remark, create_by, create_time) VALUES
(200, 1, 'PCS',  '个',   NULL, 1.000000, 'Y', '计数单位，用于成品/纸箱/塑料盖等', 'admin', sysdate()),
(201, 1, 'KG',   '千克', NULL, 1.000000, 'Y', '重量主单位，用于纸张/原料称重', 'admin', sysdate()),
(202, 1, 'M',    '米',   NULL, 1.000000, 'Y', '长度单位，用于纸张门幅/米数', 'admin', sysdate()),
(203, 1, 'ROLL', '卷',   NULL, 1.000000, 'Y', '纸张计数单位，一卷纸的完整包装', 'admin', sysdate()),
(204, 1, 'PAIR', '对',   NULL, 1.000000, 'Y', '成对计数单位，用于绳子/织带/指甲扣', 'admin', sysdate()),
(205, 1, 'BARREL','桶',  NULL, 1.000000, 'Y', '容量单位，用于油墨/胶水', 'admin', sysdate()),
(206, 1, 'SET',  '套',   NULL, 1.000000, 'Y', '成套单位，用于工具/模具', 'admin', sysdate());

-- ### 1.5.2 辅助单位（primary_unit 指向主单位编码，conversion_rate = 1本单位 = N主单位）###
INSERT INTO qxx_md_unit_measure (unit_id, factory_id, unit_code, unit_name, primary_unit, conversion_rate, enable_flag, remark, create_by, create_time) VALUES
(210, 1, 'BOX', '箱',   'PCS', 250.000000, 'Y', '包装单位，1箱=250个（按实际装箱规格调整）', 'admin', sysdate()),
(211, 1, 'TON', '吨',   'KG',  1000.000000,'Y', '重量单位，1吨=1000千克', 'admin', sysdate()),
(212, 1, 'G',   '克',   'KG',  0.001000,  'Y', '重量单位，1克=0.001千克', 'admin', sysdate());

-- ============================================================
-- Part 2: 部门（sys_dept）— 删除 RuoYi 默认，重建圣享架构
-- ============================================================

-- 先删除外键关联数据
DELETE FROM sys_user_post WHERE user_id IN (SELECT user_id FROM sys_user WHERE dept_id IN (100,101,102,103,104,105,106,107,108,109));
DELETE FROM sys_role_dept WHERE dept_id IN (100,101,102,103,104,105,106,107,108,109);
DELETE FROM sys_user WHERE dept_id IN (100,101,102,103,104,105,106,107,108,109) AND user_id NOT IN (1, 2);
-- 更新 admin 和 ry 的部门（admin→生产部, ry 保留但不关联部门）
UPDATE sys_user SET dept_id = NULL WHERE user_id = 2;
-- 删除旧部门
DELETE FROM sys_dept WHERE dept_id >= 100;

-- 重建圣享组织架构
-- 根节点
INSERT INTO sys_dept (dept_id, factory_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time)
VALUES (100, 1, 0, '0', '圣享实业', 0, '吴兵', '', '', '0', '0', 'admin', sysdate());

-- 一级部门
INSERT INTO sys_dept (dept_id, factory_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time) VALUES
(101, 1, 100, '0,100', '仓库部', 1, '姚晓翔', '', '', '0', '0', 'admin', sysdate()),
(102, 1, 100, '0,100', '生产部', 2, '吴兵', '', '', '0', '0', 'admin', sysdate()),
(103, 1, 100, '0,100', '外发部', 3, '鞠阳', '', '', '0', '0', 'admin', sysdate()),
(104, 1, 100, '0,100', '行政部', 4, '陈慧琳', '', '', '0', '0', 'admin', sysdate()),
(105, 1, 100, '0,100', '外贸部', 5, '陈仁林', '', '', '0', '0', 'admin', sysdate()),
(106, 1, 100, '0,100', '内贸部', 6, '陈丽丽', '', '', '0', '0', 'admin', sysdate()),
(107, 1, 100, '0,100', '现货部', 7, '', '', '', '0', '0', 'admin', sysdate());

-- 更新 admin 用户到生产部
UPDATE sys_user SET dept_id = 102 WHERE user_id = 1;

-- ============================================================
-- Part 3: 岗位（sys_post）— 删除 RuoYi 默认，重建圣享岗位
-- ============================================================

DELETE FROM sys_user_post WHERE post_id >= 1;
DELETE FROM sys_post WHERE post_id >= 1;

INSERT INTO sys_post (post_id, post_code, post_name, post_sort, status, create_by, create_time) VALUES
(1, 'warehouse_supervisor', '仓库主管', 1, '0', 'admin', sysdate()),
(2, 'factory_supervisor',   '工厂主管', 2, '0', 'admin', sysdate()),
(3, 'outsource_supervisor', '生产外发主管', 3, '0', 'admin', sysdate()),
(4, 'admin_finance',        '行政/财务', 4, '0', 'admin', sysdate()),
(5, 'sales_foreign',        '外贸业务员', 5, '0', 'admin', sysdate()),
(6, 'sales_domestic',       '内贸业务员', 6, '0', 'admin', sysdate()),
(7, 'operator',             '操作工', 7, '0', 'admin', sysdate());

-- ============================================================
-- Part 4: 角色（sys_role）— 保留 admin/common，新增圣享业务角色
-- ============================================================

INSERT IGNORE INTO sys_role (role_id, factory_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time) VALUES
(10, 1, '仓库角色',     'sx_warehouse',  3, '3', 1, 1, '0', '0', 'admin', sysdate()),
(11, 1, '生产角色',     'sx_production', 4, '3', 1, 1, '0', '0', 'admin', sysdate()),
(12, 1, '外发角色',     'sx_outsource',  5, '3', 1, 1, '0', '0', 'admin', sysdate()),
(13, 1, '行政角色',     'sx_admin',      6, '3', 1, 1, '0', '0', 'admin', sysdate()),
(14, 1, '业务员角色',   'sx_sales',      7, '3', 1, 1, '0', '0', 'admin', sysdate());

-- ============================================================
-- Part 5: 用户（sys_user）
-- ============================================================
-- 密码均为 123456, bcrypt: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2

INSERT IGNORE INTO sys_user (user_id, factory_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, password, status, del_flag, create_by, create_time) VALUES
(10, 1, 101, 'yaoxiaoxiang', '姚晓翔', '00', '', '', '1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate()),
(11, 1, 102, 'wubing',       '吴兵',   '00', '', '', '1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate()),
(12, 1, 103, 'juyang',       '鞠阳',   '00', '', '', '1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate()),
(13, 1, 104, 'chenhuilin',   '陈慧琳', '00', '', '', '1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate()),
(14, 1, 105, 'chenrenlin',   '陈仁林', '00', '', '', '1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate()),
(15, 1, 106, 'chenlili',     '陈丽丽', '00', '', '', '1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', sysdate());

-- ============================================================
-- Part 6: 用户-角色关联
-- ============================================================

INSERT IGNORE INTO sys_user_role (user_id, role_id, factory_id) VALUES
(1,  '1',  1),   -- admin → 超级管理员（已在 ry_20260417.sql 中存在则跳过）
(10, '10', 1),   -- 姚晓翔 → 仓库角色
(11, '11', 1),   -- 吴兵 → 生产角色
(12, '12', 1),   -- 鞠阳 → 外发角色
(13, '13', 1),   -- 陈慧琳 → 行政角色
(14, '14', 1),   -- 陈仁林 → 业务员角色
(15, '14', 1);   -- 陈丽丽 → 业务员角色

-- ============================================================
-- Part 7: 用户-岗位关联
-- ============================================================

INSERT INTO sys_user_post (user_id, post_id, factory_id) VALUES
(10, 1, 1),   -- 姚晓翔 → 仓库主管
(11, 2, 1),   -- 吴兵 → 工厂主管
(12, 3, 1),   -- 鞠阳 → 生产外发主管
(13, 4, 1),   -- 陈慧琳 → 行政/财务
(14, 5, 1),   -- 陈仁林 → 外贸业务员
(15, 6, 1);   -- 陈丽丽 → 内贸业务员

-- ============================================================
-- Part 8: MES 菜单（sys_menu）
-- ============================================================
-- menu_id: 2000+ 区间，避免与 RuoYi 默认菜单(1-4, 100-117, 500-501, 1000-1060)冲突
-- 权限标识: mes:<模块>:<功能>:list

-- ### 8.1 一级目录：MES管理 ###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2000, 'MES管理', 0, 5, 'mes', NULL, 1, 0, 'M', '0', '0', '', 'guide', 'admin', sysdate());

-- ### 8.2 MES 模块目录（2001-2008）###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2001, '基础数据', 2000, 1, 'md',  NULL, 1, 0, 'M', '0', '0', '', 'table',    'admin', sysdate()),
(2002, '仓储管理', 2000, 2, 'wm',  NULL, 1, 0, 'M', '0', '0', '', 'component', 'admin', sysdate()),
(2003, '生产管理', 2000, 3, 'pro', NULL, 1, 0, 'M', '0', '0', '', 'example',  'admin', sysdate()),
(2004, '质量管理', 2000, 4, 'qc',  NULL, 1, 0, 'M', '0', '0', '', 'checkbox', 'admin', sysdate()),
(2005, '设备管理', 2000, 5, 'dv',  NULL, 1, 0, 'M', '0', '0', '', 'tool',     'admin', sysdate()),
(2006, '采购管理', 2000, 6, 'pur', NULL, 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate()),
(2007, '排班管理', 2000, 7, 'cal', NULL, 1, 0, 'M', '0', '0', '', 'date',     'admin', sysdate()),
(2008, '工装管理', 2000, 8, 'tm',  NULL, 1, 0, 'M', '0', '0', '', 'nested',   'admin', sysdate());

-- ### 8.3 基础数据(md) 子菜单（2101-2108）###
-- 注：工厂无CRUD页面，由开发人员直接SQL操作；BOM已集成到物料主数据对话框中，无独立页面
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2101, '物料主数据',   2001, 1, 'item',        'mes/md/item/index',        1, 0, 'C', '0', '0', 'mes:md:item:list',        '#', 'admin', sysdate()),
(2102, '物料分类',     2001, 2, 'itemtype',    'mes/md/itemtype/index',    1, 0, 'C', '0', '0', 'mes:md:itemtype:list',    '#', 'admin', sysdate()),
(2104, '供应商管理',   2001, 3, 'vendor',      'mes/md/vendor/index',      1, 0, 'C', '0', '0', 'mes:md:vendor:list',      '#', 'admin', sysdate()),
(2105, '客户管理',     2001, 4, 'client',      'mes/md/client/index',      1, 0, 'C', '0', '0', 'mes:md:client:list',      '#', 'admin', sysdate()),
(2106, '计量单位',     2001, 5, 'unitmeasure', 'mes/md/unitmeasure/index', 1, 0, 'C', '0', '0', 'mes:md:unitmeasure:list', '#', 'admin', sysdate()),
(2107, '车间管理',     2001, 6, 'workshop',    'mes/md/workshop/index',    1, 0, 'C', '0', '0', 'mes:md:workshop:list',    '#', 'admin', sysdate()),
(2108, '工作站管理',   2001, 7, 'workstation', 'mes/md/workstation/index', 1, 0, 'C', '0', '0', 'mes:md:workstation:list', '#', 'admin', sysdate());

-- ### 8.4 仓储管理(wm) 子菜单（2200-2206）###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2200, '仓库定义',   2002, 1, 'warehouse',    'mes/wm/warehouse/index',    1, 0, 'C', '0', '0', 'mes:wm:warehouse:list',    '#', 'admin', sysdate()),
(2201, '库存查询',   2002, 2, 'stock',        'mes/wm/stock/index',        1, 0, 'C', '0', '0', 'mes:wm:stock:list',        '#', 'admin', sysdate()),
(2202, '物料入库',   2002, 3, 'itemrecpt',    'mes/wm/itemrecpt/index',    1, 0, 'C', '0', '0', 'mes:wm:itemrecpt:list',    '#', 'admin', sysdate()),
(2203, '生产领料',   2002, 4, 'issue',        'mes/wm/issue/index',        1, 0, 'C', '0', '0', 'mes:wm:issue:list',        '#', 'admin', sysdate()),
(2204, '产品入库',   2002, 5, 'productrecpt', 'mes/wm/productrecpt/index', 1, 0, 'C', '0', '0', 'mes:wm:productrecpt:list', '#', 'admin', sysdate()),
(2205, '销售出库',   2002, 6, 'sales',        'mes/wm/sales/index',        1, 0, 'C', '0', '0', 'mes:wm:sales:list',        '#', 'admin', sysdate()),
(2206, '库存盘点',   2002, 7, 'taking',       'mes/wm/taking/index',       1, 0, 'C', '0', '0', 'mes:wm:taking:list',       '#', 'admin', sysdate());

-- ### 8.5 生产管理(pro) 子菜单（2300-2304）###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2300, '生产工单', 2003, 1, 'workorder', 'mes/pro/workorder/index', 1, 0, 'C', '0', '0', 'mes:pro:workorder:list', '#', 'admin', sysdate()),
(2301, '工艺路线', 2003, 2, 'route',     'mes/pro/route/index',     1, 0, 'C', '0', '0', 'mes:pro:route:list',     '#', 'admin', sysdate()),
(2302, '排产任务', 2003, 3, 'task',      'mes/pro/task/index',      1, 0, 'C', '0', '0', 'mes:pro:task:list',      '#', 'admin', sysdate()),
(2303, '生产报工', 2003, 4, 'feedback',  'mes/pro/feedback/index',  1, 0, 'C', '0', '0', 'mes:pro:feedback:list',  '#', 'admin', sysdate()),
(2304, '流转卡',   2003, 5, 'card',      'mes/pro/card/index',      1, 0, 'C', '0', '0', 'mes:pro:card:list',      '#', 'admin', sysdate());

-- ### 8.6 质量管理(qc) 子菜单（2400-2404）###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2400, '来料检验', 2004, 1, 'iqc',      'mes/qc/iqc/index',      1, 0, 'C', '0', '0', 'mes:qc:iqc:list',      '#', 'admin', sysdate()),
(2401, '过程检验', 2004, 2, 'ipqc',     'mes/qc/ipqc/index',     1, 0, 'C', '0', '0', 'mes:qc:ipqc:list',     '#', 'admin', sysdate()),
(2402, '出货检验', 2004, 3, 'oqc',      'mes/qc/oqc/index',      1, 0, 'C', '0', '0', 'mes:qc:oqc:list',      '#', 'admin', sysdate()),
(2403, '缺陷管理', 2004, 4, 'defect',   'mes/qc/defect/index',   1, 0, 'C', '0', '0', 'mes:qc:defect:list',   '#', 'admin', sysdate()),
(2404, '检验模板', 2004, 5, 'template', 'mes/qc/template/index', 1, 0, 'C', '0', '0', 'mes:qc:template:list', '#', 'admin', sysdate());

-- ### 8.7 设备管理(dv) 子菜单（2505 设备类型 + 2500-2502）###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2505, '设备类型', 2005, 1, 'machinerytype', 'mes/dv/machinerytype/index', 1, 0, 'C', '0', '0', 'mes:dv:machinerytype:list', '#', 'admin', sysdate()),
(2500, '设备台账', 2005, 2, 'machinery',     'mes/dv/machinery/index',     1, 0, 'C', '0', '0', 'mes:dv:machinery:list',     '#', 'admin', sysdate()),
(2501, '点检计划', 2005, 3, 'checkplan',     'mes/dv/checkplan/index',     1, 0, 'C', '0', '0', 'mes:dv:checkplan:list',     '#', 'admin', sysdate()),
(2502, '维修记录', 2005, 4, 'repair',         'mes/dv/repair/index',        1, 0, 'C', '0', '0', 'mes:dv:repair:list',         '#', 'admin', sysdate());

-- ### 8.8 采购管理(pur) 子菜单（2600）###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2600, '采购订单', 2006, 1, 'order', 'mes/pur/order/index', 1, 0, 'C', '0', '0', 'mes:pur:order:list', '#', 'admin', sysdate());

-- ### 8.9 排班管理(cal) 子菜单（2700-2701）###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2700, '班组定义', 2007, 1, 'team', 'mes/cal/team/index', 1, 0, 'C', '0', '0', 'mes:cal:team:list', '#', 'admin', sysdate()),
(2701, '排班计划', 2007, 2, 'plan', 'mes/cal/plan/index', 1, 0, 'C', '0', '0', 'mes:cal:plan:list', '#', 'admin', sysdate());

-- ### 8.10 工装管理(tm) 子菜单（2800）###
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2800, '工装台账', 2008, 1, 'tool', 'mes/tm/tool/index', 1, 0, 'C', '0', '0', 'mes:tm:tool:list', '#', 'admin', sysdate());

-- ============================================================
-- Part 9: MES 菜单按钮（F 类型）— 每个 C 菜单下 5 个标准按钮
-- 按钮 ID 规则: parent_menu_id * 10 + seq(1=query,2=add,3=edit,4=remove,5=export)
-- ============================================================

-- 9.1 基础数据(md) 按钮
-- 注：工厂无CRUD，按钮已移除（原21001-21005），只保留list/listAll接口
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
-- 物料主数据 2101
(21011, '物料查询', 2101, 1, 'F', '0', '0', 'mes:md:item:query',  'admin', sysdate()),
(21012, '物料新增', 2101, 2, 'F', '0', '0', 'mes:md:item:add',    'admin', sysdate()),
(21013, '物料修改', 2101, 3, 'F', '0', '0', 'mes:md:item:edit',   'admin', sysdate()),
(21014, '物料删除', 2101, 4, 'F', '0', '0', 'mes:md:item:remove', 'admin', sysdate()),
(21015, '物料导出', 2101, 5, 'F', '0', '0', 'mes:md:item:export', 'admin', sysdate()),
-- 物料分类 2102
(21021, '分类查询', 2102, 1, 'F', '0', '0', 'mes:md:itemtype:query',  'admin', sysdate()),
(21022, '分类新增', 2102, 2, 'F', '0', '0', 'mes:md:itemtype:add',    'admin', sysdate()),
(21023, '分类修改', 2102, 3, 'F', '0', '0', 'mes:md:itemtype:edit',   'admin', sysdate()),
(21024, '分类删除', 2102, 4, 'F', '0', '0', 'mes:md:itemtype:remove', 'admin', sysdate()),
-- 供应商管理 2104
(21041, '供应商查询', 2104, 1, 'F', '0', '0', 'mes:md:vendor:query',  'admin', sysdate()),
(21042, '供应商新增', 2104, 2, 'F', '0', '0', 'mes:md:vendor:add',    'admin', sysdate()),
(21043, '供应商修改', 2104, 3, 'F', '0', '0', 'mes:md:vendor:edit',   'admin', sysdate()),
(21044, '供应商删除', 2104, 4, 'F', '0', '0', 'mes:md:vendor:remove', 'admin', sysdate()),
(21045, '供应商导出', 2104, 5, 'F', '0', '0', 'mes:md:vendor:export', 'admin', sysdate()),
-- 客户管理 2105
(21051, '客户查询', 2105, 1, 'F', '0', '0', 'mes:md:client:query',  'admin', sysdate()),
(21052, '客户新增', 2105, 2, 'F', '0', '0', 'mes:md:client:add',    'admin', sysdate()),
(21053, '客户修改', 2105, 3, 'F', '0', '0', 'mes:md:client:edit',   'admin', sysdate()),
(21054, '客户删除', 2105, 4, 'F', '0', '0', 'mes:md:client:remove', 'admin', sysdate()),
(21055, '客户导出', 2105, 5, 'F', '0', '0', 'mes:md:client:export', 'admin', sysdate()),
-- 计量单位 2106
(21061, '单位查询', 2106, 1, 'F', '0', '0', 'mes:md:unitmeasure:query',  'admin', sysdate()),
(21062, '单位新增', 2106, 2, 'F', '0', '0', 'mes:md:unitmeasure:add',    'admin', sysdate()),
(21063, '单位修改', 2106, 3, 'F', '0', '0', 'mes:md:unitmeasure:edit',   'admin', sysdate()),
(21064, '单位删除', 2106, 4, 'F', '0', '0', 'mes:md:unitmeasure:remove', 'admin', sysdate()),
(21065, '单位导出', 2106, 5, 'F', '0', '0', 'mes:md:unitmeasure:export', 'admin', sysdate()),
-- 车间管理 2107
(21071, '车间查询', 2107, 1, 'F', '0', '0', 'mes:md:workshop:query',  'admin', sysdate()),
(21072, '车间新增', 2107, 2, 'F', '0', '0', 'mes:md:workshop:add',    'admin', sysdate()),
(21073, '车间修改', 2107, 3, 'F', '0', '0', 'mes:md:workshop:edit',   'admin', sysdate()),
(21074, '车间删除', 2107, 4, 'F', '0', '0', 'mes:md:workshop:remove', 'admin', sysdate()),
-- 工作站管理 2108
(21081, '工作站查询', 2108, 1, 'F', '0', '0', 'mes:md:workstation:query',  'admin', sysdate()),
(21082, '工作站新增', 2108, 2, 'F', '0', '0', 'mes:md:workstation:add',    'admin', sysdate()),
(21083, '工作站修改', 2108, 3, 'F', '0', '0', 'mes:md:workstation:edit',   'admin', sysdate()),
(21084, '工作站删除', 2108, 4, 'F', '0', '0', 'mes:md:workstation:remove', 'admin', sysdate());

-- 9.2 仓储管理(wm) 按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
-- 仓库定义 2200
(22001, '仓库查询', 2200, 1, 'F', '0', '0', 'mes:wm:warehouse:query',  'admin', sysdate()),
(22002, '仓库新增', 2200, 2, 'F', '0', '0', 'mes:wm:warehouse:add',    'admin', sysdate()),
(22003, '仓库修改', 2200, 3, 'F', '0', '0', 'mes:wm:warehouse:edit',   'admin', sysdate()),
(22004, '仓库删除', 2200, 4, 'F', '0', '0', 'mes:wm:warehouse:remove', 'admin', sysdate()),
-- 库存查询 2201
(22011, '库存查询', 2201, 1, 'F', '0', '0', 'mes:wm:stock:query',  'admin', sysdate()),
(22012, '库存导出', 2201, 2, 'F', '0', '0', 'mes:wm:stock:export', 'admin', sysdate()),
-- 物料入库 2202
(22021, '入库查询', 2202, 1, 'F', '0', '0', 'mes:wm:itemrecpt:query',  'admin', sysdate()),
(22022, '入库新增', 2202, 2, 'F', '0', '0', 'mes:wm:itemrecpt:add',    'admin', sysdate()),
(22023, '入库修改', 2202, 3, 'F', '0', '0', 'mes:wm:itemrecpt:edit',   'admin', sysdate()),
(22024, '入库删除', 2202, 4, 'F', '0', '0', 'mes:wm:itemrecpt:remove', 'admin', sysdate()),
-- 生产领料 2203
(22031, '领料查询', 2203, 1, 'F', '0', '0', 'mes:wm:issue:query',  'admin', sysdate()),
(22032, '领料新增', 2203, 2, 'F', '0', '0', 'mes:wm:issue:add',    'admin', sysdate()),
(22033, '领料修改', 2203, 3, 'F', '0', '0', 'mes:wm:issue:edit',   'admin', sysdate()),
(22034, '领料删除', 2203, 4, 'F', '0', '0', 'mes:wm:issue:remove', 'admin', sysdate()),
-- 产品入库 2204
(22041, '产品入库查询', 2204, 1, 'F', '0', '0', 'mes:wm:productrecpt:query',  'admin', sysdate()),
(22042, '产品入库新增', 2204, 2, 'F', '0', '0', 'mes:wm:productrecpt:add',    'admin', sysdate()),
(22043, '产品入库修改', 2204, 3, 'F', '0', '0', 'mes:wm:productrecpt:edit',   'admin', sysdate()),
(22044, '产品入库删除', 2204, 4, 'F', '0', '0', 'mes:wm:productrecpt:remove', 'admin', sysdate()),
-- 销售出库 2205
(22051, '销售出库查询', 2205, 1, 'F', '0', '0', 'mes:wm:sales:query',  'admin', sysdate()),
(22052, '销售出库新增', 2205, 2, 'F', '0', '0', 'mes:wm:sales:add',    'admin', sysdate()),
(22053, '销售出库修改', 2205, 3, 'F', '0', '0', 'mes:wm:sales:edit',   'admin', sysdate()),
(22054, '销售出库删除', 2205, 4, 'F', '0', '0', 'mes:wm:sales:remove', 'admin', sysdate()),
-- 库存盘点 2206
(22061, '盘点查询', 2206, 1, 'F', '0', '0', 'mes:wm:taking:query',  'admin', sysdate()),
(22062, '盘点新增', 2206, 2, 'F', '0', '0', 'mes:wm:taking:add',    'admin', sysdate()),
(22063, '盘点修改', 2206, 3, 'F', '0', '0', 'mes:wm:taking:edit',   'admin', sysdate()),
(22064, '盘点删除', 2206, 4, 'F', '0', '0', 'mes:wm:taking:remove', 'admin', sysdate());

-- 9.3 生产管理(pro) 按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
-- 生产工单 2300
(23001, '工单查询', 2300, 1, 'F', '0', '0', 'mes:pro:workorder:query',  'admin', sysdate()),
(23002, '工单新增', 2300, 2, 'F', '0', '0', 'mes:pro:workorder:add',    'admin', sysdate()),
(23003, '工单修改', 2300, 3, 'F', '0', '0', 'mes:pro:workorder:edit',   'admin', sysdate()),
(23004, '工单删除', 2300, 4, 'F', '0', '0', 'mes:pro:workorder:remove', 'admin', sysdate()),
(23005, '工单导出', 2300, 5, 'F', '0', '0', 'mes:pro:workorder:export', 'admin', sysdate()),
-- 工艺路线 2301
(23011, '路线查询', 2301, 1, 'F', '0', '0', 'mes:pro:route:query',  'admin', sysdate()),
(23012, '路线新增', 2301, 2, 'F', '0', '0', 'mes:pro:route:add',    'admin', sysdate()),
(23013, '路线修改', 2301, 3, 'F', '0', '0', 'mes:pro:route:edit',   'admin', sysdate()),
(23014, '路线删除', 2301, 4, 'F', '0', '0', 'mes:pro:route:remove', 'admin', sysdate()),
-- 排产任务 2302
(23021, '排产查询', 2302, 1, 'F', '0', '0', 'mes:pro:task:query',  'admin', sysdate()),
(23022, '排产新增', 2302, 2, 'F', '0', '0', 'mes:pro:task:add',    'admin', sysdate()),
(23023, '排产修改', 2302, 3, 'F', '0', '0', 'mes:pro:task:edit',   'admin', sysdate()),
(23024, '排产删除', 2302, 4, 'F', '0', '0', 'mes:pro:task:remove', 'admin', sysdate()),
-- 生产报工 2303
(23031, '报工查询', 2303, 1, 'F', '0', '0', 'mes:pro:feedback:query',  'admin', sysdate()),
(23032, '报工新增', 2303, 2, 'F', '0', '0', 'mes:pro:feedback:add',    'admin', sysdate()),
(23033, '报工修改', 2303, 3, 'F', '0', '0', 'mes:pro:feedback:edit',   'admin', sysdate()),
(23034, '报工删除', 2303, 4, 'F', '0', '0', 'mes:pro:feedback:remove', 'admin', sysdate()),
-- 流转卡 2304
(23041, '流转卡查询', 2304, 1, 'F', '0', '0', 'mes:pro:card:query',  'admin', sysdate()),
(23042, '流转卡新增', 2304, 2, 'F', '0', '0', 'mes:pro:card:add',    'admin', sysdate()),
(23043, '流转卡修改', 2304, 3, 'F', '0', '0', 'mes:pro:card:edit',   'admin', sysdate()),
(23044, '流转卡删除', 2304, 4, 'F', '0', '0', 'mes:pro:card:remove', 'admin', sysdate());

-- 9.4 质量管理(qc) 按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
-- 来料检验 2400
(24001, 'IQC查询', 2400, 1, 'F', '0', '0', 'mes:qc:iqc:query',  'admin', sysdate()),
(24002, 'IQC新增', 2400, 2, 'F', '0', '0', 'mes:qc:iqc:add',    'admin', sysdate()),
(24003, 'IQC修改', 2400, 3, 'F', '0', '0', 'mes:qc:iqc:edit',   'admin', sysdate()),
(24004, 'IQC删除', 2400, 4, 'F', '0', '0', 'mes:qc:iqc:remove', 'admin', sysdate()),
-- 过程检验 2401
(24011, 'IPQC查询', 2401, 1, 'F', '0', '0', 'mes:qc:ipqc:query',  'admin', sysdate()),
(24012, 'IPQC新增', 2401, 2, 'F', '0', '0', 'mes:qc:ipqc:add',    'admin', sysdate()),
(24013, 'IPQC修改', 2401, 3, 'F', '0', '0', 'mes:qc:ipqc:edit',   'admin', sysdate()),
(24014, 'IPQC删除', 2401, 4, 'F', '0', '0', 'mes:qc:ipqc:remove', 'admin', sysdate()),
-- 出货检验 2402
(24021, 'OQC查询', 2402, 1, 'F', '0', '0', 'mes:qc:oqc:query',  'admin', sysdate()),
(24022, 'OQC新增', 2402, 2, 'F', '0', '0', 'mes:qc:oqc:add',    'admin', sysdate()),
(24023, 'OQC修改', 2402, 3, 'F', '0', '0', 'mes:qc:oqc:edit',   'admin', sysdate()),
(24024, 'OQC删除', 2402, 4, 'F', '0', '0', 'mes:qc:oqc:remove', 'admin', sysdate()),
-- 缺陷管理 2403
(24031, '缺陷查询', 2403, 1, 'F', '0', '0', 'mes:qc:defect:query',  'admin', sysdate()),
(24032, '缺陷新增', 2403, 2, 'F', '0', '0', 'mes:qc:defect:add',    'admin', sysdate()),
(24033, '缺陷修改', 2403, 3, 'F', '0', '0', 'mes:qc:defect:edit',   'admin', sysdate()),
(24034, '缺陷删除', 2403, 4, 'F', '0', '0', 'mes:qc:defect:remove', 'admin', sysdate()),
-- 检验模板 2404
(24041, '模板查询', 2404, 1, 'F', '0', '0', 'mes:qc:template:query',  'admin', sysdate()),
(24042, '模板新增', 2404, 2, 'F', '0', '0', 'mes:qc:template:add',    'admin', sysdate()),
(24043, '模板修改', 2404, 3, 'F', '0', '0', 'mes:qc:template:edit',   'admin', sysdate()),
(24044, '模板删除', 2404, 4, 'F', '0', '0', 'mes:qc:template:remove', 'admin', sysdate());

-- 9.5 设备管理(dv) 按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
-- 设备类型 2505
(25051, '设备类型查询', 2505, 1, 'F', '0', '0', 'mes:dv:machinerytype:query',  'admin', sysdate()),
(25052, '设备类型新增', 2505, 2, 'F', '0', '0', 'mes:dv:machinerytype:add',    'admin', sysdate()),
(25053, '设备类型修改', 2505, 3, 'F', '0', '0', 'mes:dv:machinerytype:edit',   'admin', sysdate()),
(25054, '设备类型删除', 2505, 4, 'F', '0', '0', 'mes:dv:machinerytype:remove', 'admin', sysdate()),
-- 设备台账 2500
(25001, '设备查询', 2500, 1, 'F', '0', '0', 'mes:dv:machinery:query',  'admin', sysdate()),
(25002, '设备新增', 2500, 2, 'F', '0', '0', 'mes:dv:machinery:add',    'admin', sysdate()),
(25003, '设备修改', 2500, 3, 'F', '0', '0', 'mes:dv:machinery:edit',   'admin', sysdate()),
(25004, '设备删除', 2500, 4, 'F', '0', '0', 'mes:dv:machinery:remove', 'admin', sysdate()),
-- 点检计划 2501
(25011, '点检查询', 2501, 1, 'F', '0', '0', 'mes:dv:checkplan:query',  'admin', sysdate()),
(25012, '点检新增', 2501, 2, 'F', '0', '0', 'mes:dv:checkplan:add',    'admin', sysdate()),
(25013, '点检修改', 2501, 3, 'F', '0', '0', 'mes:dv:checkplan:edit',   'admin', sysdate()),
(25014, '点检删除', 2501, 4, 'F', '0', '0', 'mes:dv:checkplan:remove', 'admin', sysdate()),
-- 维修记录 2502
(25021, '维修查询', 2502, 1, 'F', '0', '0', 'mes:dv:repair:query',  'admin', sysdate()),
(25022, '维修新增', 2502, 2, 'F', '0', '0', 'mes:dv:repair:add',    'admin', sysdate()),
(25023, '维修修改', 2502, 3, 'F', '0', '0', 'mes:dv:repair:edit',   'admin', sysdate()),
(25024, '维修删除', 2502, 4, 'F', '0', '0', 'mes:dv:repair:remove', 'admin', sysdate());

-- 9.6 采购管理(pur) 按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(26001, '采购查询', 2600, 1, 'F', '0', '0', 'mes:pur:order:query',  'admin', sysdate()),
(26002, '采购新增', 2600, 2, 'F', '0', '0', 'mes:pur:order:add',    'admin', sysdate()),
(26003, '采购修改', 2600, 3, 'F', '0', '0', 'mes:pur:order:edit',   'admin', sysdate()),
(26004, '采购删除', 2600, 4, 'F', '0', '0', 'mes:pur:order:remove', 'admin', sysdate()),
(26005, '采购导出', 2600, 5, 'F', '0', '0', 'mes:pur:order:export', 'admin', sysdate());

-- 9.7 排班管理(cal) 按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(27001, '班组查询', 2700, 1, 'F', '0', '0', 'mes:cal:team:query',  'admin', sysdate()),
(27002, '班组新增', 2700, 2, 'F', '0', '0', 'mes:cal:team:add',    'admin', sysdate()),
(27003, '班组修改', 2700, 3, 'F', '0', '0', 'mes:cal:team:edit',   'admin', sysdate()),
(27004, '班组删除', 2700, 4, 'F', '0', '0', 'mes:cal:team:remove', 'admin', sysdate()),
(27011, '排班查询', 2701, 1, 'F', '0', '0', 'mes:cal:plan:query',  'admin', sysdate()),
(27012, '排班新增', 2701, 2, 'F', '0', '0', 'mes:cal:plan:add',    'admin', sysdate()),
(27013, '排班修改', 2701, 3, 'F', '0', '0', 'mes:cal:plan:edit',   'admin', sysdate()),
(27014, '排班删除', 2701, 4, 'F', '0', '0', 'mes:cal:plan:remove', 'admin', sysdate());

-- 9.8 工装管理(tm) 按钮
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time) VALUES
(28001, '工装查询', 2800, 1, 'F', '0', '0', 'mes:tm:tool:query',  'admin', sysdate()),
(28002, '工装新增', 2800, 2, 'F', '0', '0', 'mes:tm:tool:add',    'admin', sysdate()),
(28003, '工装修改', 2800, 3, 'F', '0', '0', 'mes:tm:tool:edit',   'admin', sysdate()),
(28004, '工装删除', 2800, 4, 'F', '0', '0', 'mes:tm:tool:remove', 'admin', sysdate());

-- ============================================================
-- Part 10: 角色-菜单关联（sys_role_menu）
-- admin(role_id=1) 已有 *:*:* 通配权限，无需添加
-- 普通角色(role_id=2) 保留原有 RuoYi 菜单，不加 MES 菜单
-- ============================================================

-- ### 10.1 仓库角色(sx_warehouse, role_id=10) → WM 仓储管理全部 ###
-- 注意: 下面的子查询获取所有 2002(Wm目录) 及其子孙菜单的 menu_id
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '10', menu_id, 1 FROM sys_menu WHERE menu_id IN (2000, 2002, 2200, 2201, 2202, 2203, 2204, 2205, 2206);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '10', menu_id, 1 FROM sys_menu WHERE parent_id IN (2200, 2201, 2202, 2203, 2204, 2205, 2206);

-- ### 10.2 生产角色(sx_production, role_id=11) → MD基础数据 + PRO生产管理 ###
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '11', menu_id, 1 FROM sys_menu WHERE menu_id IN (2000, 2001, 2003);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '11', menu_id, 1 FROM sys_menu WHERE parent_id IN (2001, 2003);

-- MD 所有子菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '11', menu_id, 1 FROM sys_menu WHERE menu_id IN (2101, 2102, 2104, 2105, 2106, 2107, 2108);

-- MD 所有按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '11', menu_id, 1 FROM sys_menu WHERE parent_id IN (2101, 2102, 2104, 2105, 2106, 2107, 2108);

-- PRO 所有子菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '11', menu_id, 1 FROM sys_menu WHERE menu_id IN (2300, 2301, 2302, 2303, 2304);

-- PRO 所有按钮
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '11', menu_id, 1 FROM sys_menu WHERE parent_id IN (2300, 2301, 2302, 2303, 2304);

-- ### 10.3 外发角色(sx_outsource, role_id=12) → MD供应商 + PRO报工/工单 + WM出库 ###
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '12', menu_id, 1 FROM sys_menu WHERE menu_id IN (2000, 2001, 2003, 2002);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '12', menu_id, 1 FROM sys_menu WHERE menu_id IN (2104, 2300, 2303, 2304, 2205);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '12', menu_id, 1 FROM sys_menu WHERE parent_id IN (2104, 2300, 2303, 2304, 2205);

-- ### 10.4 行政角色(sx_admin, role_id=13) → 系统管理（RuoYi默认）###
-- 系统管理(menu_id=1) 及其子菜单已在 role=2(common) 中，这里不再重复添加

-- ### 10.5 业务员角色(sx_sales, role_id=14) → MD客户 + WM销售出库 + WM库存查询 ###
INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '14', menu_id, 1 FROM sys_menu WHERE menu_id IN (2000, 2001, 2002);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '14', menu_id, 1 FROM sys_menu WHERE menu_id IN (2105, 2201, 2205);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT '14', menu_id, 1 FROM sys_menu WHERE parent_id IN (2105, 2201, 2205);
