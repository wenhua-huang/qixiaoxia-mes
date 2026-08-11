-- ═══════════════════════════════════════════════════════════════════════
-- V104 外协厂商签收 + 分批补录支撑
--
-- 背景：原状态机 DRAFT→ISSUED→PROCESSING→RECEIVED 把"外协签收物料"和
--       "外协加工完交付"压缩成 recordResult 一个动作，责任转移节点缺失。
--       新增 VENDOR_RCVD（厂商已签收）状态：我方发料后，厂商在 app 确认
--       收到物料，物料保管责任转移到厂商；之后厂商才可录加工结果。
--
-- 改动：
--   1. qxx_wm_outsource_order 加 vendor_receive_time / vendor_receiver
--   2. 字典 mes_outsource_status 加 VENDOR_RCVD（插在 ISSUED 与 PROCESSING 之间）
--   3. 菜单加 28806 厂商签收权限，授给厂商角色 106
--
-- 注：sys_dict_*/sys_menu/sys_role_menu 无 factory_id（Flyway 裸 JDBC，手写幂等）。
--     ALTER 用 PROCEDURE 包装实现幂等（列已存在则跳过）。
-- @author qixiaoxia
-- ═══════════════════════════════════════════════════════════════════════

SET NAMES utf8mb4;

-- 1. order 表加签收字段
DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_wm_outsource_order', 'vendor_receive_time',
    'ALTER TABLE qxx_wm_outsource_order ADD COLUMN vendor_receive_time datetime DEFAULT NULL COMMENT ''厂商签收时间''');
CALL add_col_if_missing('qxx_wm_outsource_order', 'vendor_receiver',
    'ALTER TABLE qxx_wm_outsource_order ADD COLUMN vendor_receiver varchar(64) DEFAULT NULL COMMENT ''厂商签收人''');

DROP PROCEDURE IF EXISTS add_col_if_missing;

-- 2. 字典：插入 VENDOR_RCVD，并把 PROCESSING/RECEIVED/CLOSED 的 sort 后移
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '厂商已签收', 'VENDOR_RCVD', 'mes_outsource_status', '', 'info', 'N', '0', 'admin', NOW(), '厂商已确认收到发料物料，可开始加工'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_status' AND dict_value = 'VENDOR_RCVD');

-- 后移已有状态的 sort，给 VENDOR_RCVD 腾出位置 2
UPDATE sys_dict_data SET dict_sort = 3 WHERE dict_type = 'mes_outsource_status' AND dict_value = 'PROCESSING' AND dict_sort < 3;
UPDATE sys_dict_data SET dict_sort = 4 WHERE dict_type = 'mes_outsource_status' AND dict_value = 'RECEIVED' AND dict_sort < 4;
UPDATE sys_dict_data SET dict_sort = 5 WHERE dict_type = 'mes_outsource_status' AND dict_value = 'CLOSED' AND dict_sort < 5;

-- 3. 菜单：厂商签收权限（28805 已被执行发料占用，用 28806）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28806, '厂商签收', 28800, 6, 'F', '0', '0', 'mes:wm:outsource:vendorReceive', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28806);

-- 4. 角色授权：admin(1, factory_id=0) + 厂商(106, factory_id=1)；产线(11)不授（签收是厂商操作）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 28806, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = 28806 AND rm.factory_id = 0);

INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 106, 28806, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 106 AND rm.menu_id = 28806 AND rm.factory_id = 1);
