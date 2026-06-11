-- ============================================
-- 生产环境菜单修复（删除废弃菜单，补全 md 模块）
-- 用法: mysql -uroot -p mes < backend/sql/fix_prod_menu.sql
-- ============================================
DELETE FROM sys_role_menu WHERE menu_id IN (2100,21001,21002,21003,21004,21005);
DELETE FROM sys_menu WHERE menu_id IN (2100,21001,21002,21003,21004,21005);
DELETE FROM sys_role_menu WHERE menu_id IN (2103,21031,21032,21033,21034);
DELETE FROM sys_menu WHERE menu_id IN (2103,21031,21032,21033,21034);

INSERT IGNORE INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
VALUES (2001,'基础数据',2000,1,'md',NULL,1,0,'M','0','0','','table','admin',NOW());

INSERT IGNORE INTO sys_menu (menu_id,menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time) VALUES
(2101,'物料主数据',2001,1,'item','mes/md/item/index',1,0,'C','0','0','mes:md:item:list','#','admin',NOW()),
(2102,'物料分类',2001,2,'itemtype','mes/md/itemtype/index',1,0,'C','0','0','mes:md:itemtype:list','#','admin',NOW()),
(2104,'供应商管理',2001,3,'vendor','mes/md/vendor/index',1,0,'C','0','0','mes:md:vendor:list','#','admin',NOW()),
(2105,'客户管理',2001,4,'client','mes/md/client/index',1,0,'C','0','0','mes:md:client:list','#','admin',NOW()),
(2106,'计量单位',2001,5,'unitmeasure','mes/md/unitmeasure/index',1,0,'C','0','0','mes:md:unitmeasure:list','#','admin',NOW()),
(2107,'车间管理',2001,6,'workshop','mes/md/workshop/index',1,0,'C','0','0','mes:md:workshop:list','#','admin',NOW()),
(2108,'工作站管理',2001,7,'workstation','mes/md/workstation/index',1,0,'C','0','0','mes:md:workstation:list','#','admin',NOW());

INSERT IGNORE INTO sys_menu (menu_id,menu_name,parent_id,order_num,menu_type,visible,status,perms,create_by,create_time) VALUES
(21011,'物料查询',2101,1,'F','0','0','mes:md:item:query','admin',NOW()),
(21012,'物料新增',2101,2,'F','0','0','mes:md:item:add','admin',NOW()),
(21013,'物料修改',2101,3,'F','0','0','mes:md:item:edit','admin',NOW()),
(21014,'物料删除',2101,4,'F','0','0','mes:md:item:remove','admin',NOW()),
(21015,'物料导出',2101,5,'F','0','0','mes:md:item:export','admin',NOW()),
(21021,'分类查询',2102,1,'F','0','0','mes:md:itemtype:query','admin',NOW()),
(21022,'分类新增',2102,2,'F','0','0','mes:md:itemtype:add','admin',NOW()),
(21023,'分类修改',2102,3,'F','0','0','mes:md:itemtype:edit','admin',NOW()),
(21024,'分类删除',2102,4,'F','0','0','mes:md:itemtype:remove','admin',NOW()),
(21041,'供应商查询',2104,1,'F','0','0','mes:md:vendor:query','admin',NOW()),
(21042,'供应商新增',2104,2,'F','0','0','mes:md:vendor:add','admin',NOW()),
(21043,'供应商修改',2104,3,'F','0','0','mes:md:vendor:edit','admin',NOW()),
(21044,'供应商删除',2104,4,'F','0','0','mes:md:vendor:remove','admin',NOW()),
(21045,'供应商导出',2104,5,'F','0','0','mes:md:vendor:export','admin',NOW()),
(21051,'客户查询',2105,1,'F','0','0','mes:md:client:query','admin',NOW()),
(21052,'客户新增',2105,2,'F','0','0','mes:md:client:add','admin',NOW()),
(21053,'客户修改',2105,3,'F','0','0','mes:md:client:edit','admin',NOW()),
(21054,'客户删除',2105,4,'F','0','0','mes:md:client:remove','admin',NOW()),
(21055,'客户导出',2105,5,'F','0','0','mes:md:client:export','admin',NOW()),
(21071,'车间查询',2107,1,'F','0','0','mes:md:workshop:query','admin',NOW()),
(21072,'车间新增',2107,2,'F','0','0','mes:md:workshop:add','admin',NOW()),
(21073,'车间修改',2107,3,'F','0','0','mes:md:workshop:edit','admin',NOW()),
(21074,'车间删除',2107,4,'F','0','0','mes:md:workshop:remove','admin',NOW()),
(21081,'工作站查询',2108,1,'F','0','0','mes:md:workstation:query','admin',NOW()),
(21082,'工作站新增',2108,2,'F','0','0','mes:md:workstation:add','admin',NOW()),
(21083,'工作站修改',2108,3,'F','0','0','mes:md:workstation:edit','admin',NOW()),
(21084,'工作站删除',2108,4,'F','0','0','mes:md:workstation:remove','admin',NOW());

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id BETWEEN 2101 AND 21084 AND parent_id >= 2000;
