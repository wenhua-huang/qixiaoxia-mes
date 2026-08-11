-- ═══════════════════════════════════════════════════════════════════════
-- V105 外协加工完成 + 厂商发货
--
-- 背景：原状态机 ...→PROCESSING→RECEIVED 缺少厂商交付确认节点。
--       厂商录完结果后无法标记"加工完成/已发货"，工厂也无从知道该收货。
--       新增 FINISHED（加工完成）和 SHIPPED（已发货）两个状态：
--         PROCESSING → FINISHED（录满自动完成，或厂商手动完成允许短交）
--         FINISHED   → SHIPPED  （厂商确认发货，锁定补录）
--         SHIPPED    → RECEIVED （工厂收货，原要求 PROCESSING 改为 SHIPPED）
--
-- 改动：
--   1. qxx_wm_outsource_order 加 finish_time/finish_by/ship_time/ship_by
--   2. 字典加 FINISHED(sort=4)、SHIPPED(sort=5)，RECEIVED/CLOSED 后移
--   3. 菜单 28807 加工完成、28808 厂商发货，授 admin+厂商角色
--   4. 历史已录满的 PROCESSING 单提升为 SHIPPED，保证可立即收货
--
-- 注：sys_dict_*/sys_menu/sys_role_menu 无 factory_id（Flyway 裸 JDBC，手写幂等）。
-- @author qixiaoxia
-- ═══════════════════════════════════════════════════════════════════════

SET NAMES utf8mb4;

-- 1. order 表加完成/发货字段（幂等 ALTER）
DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_wm_outsource_order', 'finish_time',
    'ALTER TABLE qxx_wm_outsource_order ADD COLUMN finish_time datetime DEFAULT NULL COMMENT ''加工完成时间''');
CALL add_col_if_missing('qxx_wm_outsource_order', 'finish_by',
    'ALTER TABLE qxx_wm_outsource_order ADD COLUMN finish_by varchar(64) DEFAULT NULL COMMENT ''加工完成人''');
CALL add_col_if_missing('qxx_wm_outsource_order', 'ship_time',
    'ALTER TABLE qxx_wm_outsource_order ADD COLUMN ship_time datetime DEFAULT NULL COMMENT ''厂商发货时间''');
CALL add_col_if_missing('qxx_wm_outsource_order', 'ship_by',
    'ALTER TABLE qxx_wm_outsource_order ADD COLUMN ship_by varchar(64) DEFAULT NULL COMMENT ''发货人''');

DROP PROCEDURE IF EXISTS add_col_if_missing;

-- 2. 字典：插入 FINISHED/SHIPPED，并把 RECEIVED/CLOSED 的 sort 后移
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '加工完成', 'FINISHED', 'mes_outsource_status', '', 'warning', 'N', '0', 'admin', NOW(), '厂商已完成加工，待发货'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_status' AND dict_value = 'FINISHED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已发货', 'SHIPPED', 'mes_outsource_status', '', 'primary', 'N', '0', 'admin', NOW(), '厂商已发货，待工厂收货'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_status' AND dict_value = 'SHIPPED');

-- 后移已有状态 sort，给 FINISHED/SHIPPED 腾位置
UPDATE sys_dict_data SET dict_sort = 6 WHERE dict_type = 'mes_outsource_status' AND dict_value = 'RECEIVED' AND dict_sort < 6;
UPDATE sys_dict_data SET dict_sort = 7 WHERE dict_type = 'mes_outsource_status' AND dict_value = 'CLOSED' AND dict_sort < 7;

-- 3. 菜单：加工完成(28807)、厂商发货(28808)
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28807, '加工完成', 28800, 7, 'F', '0', '0', 'mes:wm:outsource:complete', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28807);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28808, '厂商发货', 28800, 8, 'F', '0', '0', 'mes:wm:outsource:ship', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28808);

-- 授权：admin(1, factory_id=0) + 厂商(106, factory_id=1)
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 28807, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=1 AND rm.menu_id=28807 AND rm.factory_id=0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 106, 28807, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=106 AND rm.menu_id=28807 AND rm.factory_id=1);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 28808, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=1 AND rm.menu_id=28808 AND rm.factory_id=0);
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 106, 28808, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=106 AND rm.menu_id=28808 AND rm.factory_id=1);

-- 4. 历史数据：已录满(recpt_total>=issue_total 且有收货行)的旧 PROCESSING 单提升为 SHIPPED
--    未录满的保持 PROCESSING，由厂商走新流程手动完成+发货
UPDATE qxx_wm_outsource_order
SET status = 'SHIPPED',
    finish_time = COALESCE(finish_time, NOW()),
    finish_by   = COALESCE(finish_by, 'system'),
    ship_time   = COALESCE(ship_time, NOW()),
    ship_by     = COALESCE(ship_by, 'system'),
    update_time = NOW()
WHERE status = 'PROCESSING'
  AND recpt_total_qty IS NOT NULL
  AND issue_total_qty IS NOT NULL
  AND recpt_total_qty >= issue_total_qty
  AND EXISTS (SELECT 1 FROM qxx_wm_outsource_recpt_line r WHERE r.order_id = qxx_wm_outsource_order.order_id);
