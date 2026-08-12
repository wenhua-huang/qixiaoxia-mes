-- ============================================================
-- V98：分切外协闭环 —— sys_user.vendor_id 隔离 + 外协状态机
--
-- 背景：原纸分切以外协为主（母卷发给分切厂商 → 厂商切完 → 子卷收回）。
--       V92/V94 的厂内分切（INTERNAL，一步完成领料+建卷+报工）保留；
--       本迁移扩展 OUTSOURCE 模式，三步：
--         我方建单(ISSUED+母卷OUTSOURCED) → 厂商录结果(SLITTING+建子卷) → 我方收货(RECEIVED+入库)
--
-- 隔离方案：sys_user.vendor_id 绑定厂商账号。
--   - 我方员工 vendor_id=NULL，看全部外协单
--   - 厂商员工 vendor_id=厂商ID，Service 层 WHERE vendor_id 过滤，只看自己的
--   - 同 factory_id（不引入独立工厂/outsource_factory_id）
--
-- 幂等：ALTER 前查 information_schema；INSERT 用 WHERE NOT EXISTS。
-- 注意：sys_role_menu 主键含 factory_id（NOT NULL），Flyway 裸 JDBC 必须显式写。
-- ============================================================

SET NAMES utf8mb4;

-- ════════════════════════════════════════════
-- 1. sys_user 加 vendor_id
-- ════════════════════════════════════════════
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'vendor_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN vendor_id bigint(20) DEFAULT NULL COMMENT ''关联厂商ID(外协厂商员工账号绑定qxx_md_vendor;我方员工为NULL)'' AFTER factory_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND index_name = 'idx_vendor_id');
SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE sys_user ADD INDEX idx_vendor_id (vendor_id)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ════════════════════════════════════════════
-- 2. qxx_pro_slitting_record 加 slit_mode + vendor_*
-- ════════════════════════════════════════════
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'slit_mode');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN slit_mode varchar(20) DEFAULT ''INTERNAL'' COMMENT ''分切模式:INTERNAL-厂内,OUTSOURCE-外协'' AFTER status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'vendor_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN vendor_id bigint(20) DEFAULT NULL COMMENT ''外协厂商ID(关联qxx_md_vendor,OUTSOURCE模式必填)'' AFTER slit_mode',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'vendor_code');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN vendor_code varchar(64) DEFAULT NULL COMMENT ''外协厂商编码'' AFTER vendor_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'vendor_name');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN vendor_name varchar(255) DEFAULT NULL COMMENT ''外协厂商名称'' AFTER vendor_code',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- parent_roll_id 在 V92 建表时是 NOT NULL，但外协建单时母卷尚未创建（发料时才建），需放宽
SET @is_nullable = (SELECT IS_NULLABLE FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'parent_roll_id');
SET @sql = IF(@is_nullable = 'NO',
    'ALTER TABLE qxx_pro_slitting_record MODIFY COLUMN parent_roll_id bigint(20) DEFAULT NULL COMMENT ''母卷ID(外协发料时回填)''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 外协状态默认值改为 PENDING（新建外协单待发料）；已有厂内数据保持 EXECUTED 不变（slit_mode=INTERNAL 时 status 仍为 EXECUTED）
-- 注意：不改列默认值，避免影响厂内；状态由代码控制。

-- ════════════════════════════════════════════
-- 3. 字典：分切模式 + 外协状态值 + 纸卷状态
-- ════════════════════════════════════════════

-- 3.1 分切模式
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '分切模式', 'mes_pro_slitting_mode', '0', 'admin', NOW(), '厂内/外协分切模式'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_pro_slitting_mode');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '厂内分切', 'INTERNAL', 'mes_pro_slitting_mode', '', 'primary', 'Y', '0', 'admin', NOW(), '本厂自行分切'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_slitting_mode' AND dict_value = 'INTERNAL');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '外协分切', 'OUTSOURCE', 'mes_pro_slitting_mode', '', 'warning', 'N', '0', 'admin', NOW(), '外发给分切厂商'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_slitting_mode' AND dict_value = 'OUTSOURCE');

-- 3.2 分切状态补外协三态（EXECUTED 已在 V92 存在）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已发料', 'ISSUED', 'mes_pro_slitting_status', '', 'warning', 'N', '0', 'admin', NOW(), '母卷已发给外协厂商'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_slitting_status' AND dict_value = 'ISSUED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '分切中', 'SLITTING', 'mes_pro_slitting_status', '', 'primary', 'N', '0', 'admin', NOW(), '厂商已录结果待收货'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_slitting_status' AND dict_value = 'SLITTING');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已收货', 'RECEIVED', 'mes_pro_slitting_status', '', 'success', 'N', '0', 'admin', NOW(), '子卷已收回入库'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_slitting_status' AND dict_value = 'RECEIVED');

-- 3.3 纸卷状态字典（roll_detail.status）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '纸卷状态', 'mes_wm_roll_status', '0', 'admin', NOW(), '纸卷明细状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_wm_roll_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '在库', 'IN_STOCK', 'mes_wm_roll_status', '', 'success', 'Y', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_roll_status' AND dict_value = 'IN_STOCK');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已消耗', 'CONSUMED', 'mes_wm_roll_status', '', 'info', 'N', '0', 'admin', NOW(), '领料/分切消耗'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_roll_status' AND dict_value = 'CONSUMED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已外发', 'OUTSOURCED', 'mes_wm_roll_status', '', 'warning', 'N', '0', 'admin', NOW(), '母卷已发给外协厂商'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_roll_status' AND dict_value = 'OUTSOURCED');

-- ════════════════════════════════════════════
-- 4. 权限：厂商录结果按钮（menu_id=23134）
-- ════════════════════════════════════════════
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23134, '厂商录结果', 2313, 6, 'F', '0', '0', 'mes:pro:slitting:result', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23134);

-- 超管授权（factory_id=0 是若依超管角色的惯例值）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, 23134, 0 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 23134 AND factory_id = 0);
-- 生产角色 11 授权（factory_id=1 主工厂）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, 23134, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 11 AND menu_id = 23134 AND factory_id = 1);

-- ════════════════════════════════════════════
-- 5. 厂商角色（role_id=106, role_key=vendor, 仅本人数据）
--    注：role_id=12 已被"外发角色 sx_outsource"占用（我方管外协的内部角色），不能复用。
-- ════════════════════════════════════════════
INSERT INTO sys_role (role_id, factory_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT 106, 1, '外协厂商', 'vendor', 20, '5', 1, 1, '0', '0', 'admin', NOW(), '外协厂商员工角色：只能看自己厂商的分切任务并录结果'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 106 AND factory_id = 1);

-- 厂商角色授权：分切作业 C-menu + 查询 + 厂商录结果
-- （不授 add/edit/remove —— 厂商不能建单/改单/删单，只能录结果和查看）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 106, m.menu_id, 1 FROM sys_menu m
WHERE m.menu_id IN (2313, 23131, 23134)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 106 AND rm.menu_id = m.menu_id AND rm.factory_id = 1);

-- ════════════════════════════════════════════
-- 6. 5个厂商员工账号（factory_id=1, vendor_id 绑定 208-212）
--    初始密码：vendor123（BCrypt），登录后自行修改
-- ════════════════════════════════════════════
-- BCrypt('vendor123')
SET @vendor_pwd = '$2b$10$Pn2PH79l3HNx/f0GD4vyKOO82PoJaDMf.pvDsWVhZwyquFh/utcua';

-- 万隆 208
INSERT INTO sys_user (user_id, factory_id, vendor_id, dept_id, user_name, nick_name, email, phonenumber, sex, password, status, del_flag, create_by, create_time, remark)
SELECT 208, 1, 208, NULL, 'wanlong', '万隆分切', '', '', '0', @vendor_pwd, '0', '0', 'admin', NOW(), '外协厂商账号-万隆'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'wanlong' AND del_flag = '0');
INSERT INTO sys_user_role (user_id, role_id, factory_id) SELECT 208, 106, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 208 AND role_id = 106 AND factory_id = 1);

-- 吉荣 209
INSERT INTO sys_user (user_id, factory_id, vendor_id, dept_id, user_name, nick_name, email, phonenumber, sex, password, status, del_flag, create_by, create_time, remark)
SELECT 209, 1, 209, NULL, 'jirong', '吉荣分切', '', '', '0', @vendor_pwd, '0', '0', 'admin', NOW(), '外协厂商账号-吉荣'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'jirong' AND del_flag = '0');
INSERT INTO sys_user_role (user_id, role_id, factory_id) SELECT 209, 106, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 209 AND role_id = 106 AND factory_id = 1);

-- 浩卓 210
INSERT INTO sys_user (user_id, factory_id, vendor_id, dept_id, user_name, nick_name, email, phonenumber, sex, password, status, del_flag, create_by, create_time, remark)
SELECT 210, 1, 210, NULL, 'haozhuo', '浩卓分切', '', '', '0', @vendor_pwd, '0', '0', 'admin', NOW(), '外协厂商账号-浩卓'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'haozhuo' AND del_flag = '0');
INSERT INTO sys_user_role (user_id, role_id, factory_id) SELECT 210, 106, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 210 AND role_id = 106 AND factory_id = 1);

-- 欧诺 211
INSERT INTO sys_user (user_id, factory_id, vendor_id, dept_id, user_name, nick_name, email, phonenumber, sex, password, status, del_flag, create_by, create_time, remark)
SELECT 211, 1, 211, NULL, 'ounuo', '欧诺分切', '', '', '0', @vendor_pwd, '0', '0', 'admin', NOW(), '外协厂商账号-欧诺'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'ounuo' AND del_flag = '0');
INSERT INTO sys_user_role (user_id, role_id, factory_id) SELECT 211, 106, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 211 AND role_id = 106 AND factory_id = 1);

-- 圣皓 212
INSERT INTO sys_user (user_id, factory_id, vendor_id, dept_id, user_name, nick_name, email, phonenumber, sex, password, status, del_flag, create_by, create_time, remark)
SELECT 212, 1, 212, NULL, 'shenghao', '圣皓分切', '', '', '0', @vendor_pwd, '0', '0', 'admin', NOW(), '外协厂商账号-圣皓'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = 'shenghao' AND del_flag = '0');
INSERT INTO sys_user_role (user_id, role_id, factory_id) SELECT 212, 106, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_user_role WHERE user_id = 212 AND role_id = 106 AND factory_id = 1);
