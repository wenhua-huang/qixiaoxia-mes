-- ============================================================
-- V100：通用外协发货/收货框架
--
-- 背景：V56 预留了 6 张 outsource_issue/recpt 空壳表但零代码。
--       分切外协被迫自包含实现（ProSlittingServiceImpl 三步流程）。
--       本迁移建立通用外协订单模型，分切/印刷/复合等外协业务统一挂载。
--
-- 设计：一张 outsource_order 头表统一管理外协全生命周期，
--       issue_line（发料行）+ recpt_line（收货行）作为子表。
--       V56 的 6 张 detail 表保留（二期按库存记录粒度的扣减明细可用）。
--
-- 幂等：CREATE TABLE IF NOT EXISTS；INSERT WHERE NOT EXISTS；ALTER 查 information_schema。
-- 注：sys_dict_*/sys_menu 系统表无 factory_id；sys_role_menu 复合主键含 factory_id。
-- ============================================================

SET NAMES utf8mb4;

-- ════════════════════════════════════════════
-- 0. V56 的 issue_line/recpt_line 表已存在，补 order_id/quantity/warehouse_*/source_ref 等列
--    用简单 ALTER（列已存在时 MySQL 8 报 duplicate，用 stored procedure 兼容）
-- ════════════════════════════════════════════
-- 注意：以下 ALTER 用 PROCEDURE 包装实现幂等（列存在则跳过）

DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_wm_outsource_issue_line', 'order_id', 'ALTER TABLE qxx_wm_outsource_issue_line ADD COLUMN order_id bigint DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_issue_line', 'quantity', 'ALTER TABLE qxx_wm_outsource_issue_line ADD COLUMN quantity decimal(14,4) DEFAULT 0');
CALL add_col_if_missing('qxx_wm_outsource_issue_line', 'warehouse_code', 'ALTER TABLE qxx_wm_outsource_issue_line ADD COLUMN warehouse_code varchar(64) DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_issue_line', 'warehouse_name', 'ALTER TABLE qxx_wm_outsource_issue_line ADD COLUMN warehouse_name varchar(255) DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_issue_line', 'source_ref_type', 'ALTER TABLE qxx_wm_outsource_issue_line ADD COLUMN source_ref_type varchar(30) DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_issue_line', 'source_ref_id', 'ALTER TABLE qxx_wm_outsource_issue_line ADD COLUMN source_ref_id bigint DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_issue_line', 'ext_attrs', 'ALTER TABLE qxx_wm_outsource_issue_line ADD COLUMN ext_attrs varchar(500) DEFAULT NULL');

CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'order_id', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN order_id bigint DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'quantity', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN quantity decimal(14,4) DEFAULT 0');
CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'warehouse_code', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN warehouse_code varchar(64) DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'warehouse_name', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN warehouse_name varchar(255) DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'ext_attrs', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN ext_attrs varchar(500) DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'source_ref_type', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN source_ref_type varchar(30) DEFAULT NULL');
CALL add_col_if_missing('qxx_wm_outsource_recpt_line', 'source_ref_id', 'ALTER TABLE qxx_wm_outsource_recpt_line ADD COLUMN source_ref_id bigint DEFAULT NULL');

DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_order` (
  `order_id`         bigint        NOT NULL AUTO_INCREMENT  COMMENT '外协订单ID',
  `factory_id`       bigint        NOT NULL                 COMMENT '工厂ID',
  `order_code`       varchar(64)   NOT NULL                 COMMENT '外协单编码(OUTSOURCE_CODE自动生成)',
  -- 关联
  `vendor_id`        bigint        NOT NULL                 COMMENT '外协厂商ID(qxx_md_vendor)',
  `vendor_code`      varchar(64)   DEFAULT NULL             COMMENT '厂商编码',
  `vendor_name`      varchar(255)  DEFAULT NULL             COMMENT '厂商名称',
  `workorder_id`     bigint        DEFAULT NULL             COMMENT '工单ID(可空,独立外协)',
  `workorder_code`   varchar(64)   DEFAULT NULL             COMMENT '工单编码',
  `card_id`          bigint        DEFAULT NULL             COMMENT '流转卡ID(联动OUTSOURCING状态)',
  `route_id`         bigint        DEFAULT NULL             COMMENT '工艺路线ID(判断末工序)',
  `process_id`       bigint        DEFAULT NULL             COMMENT '外协工序ID',
  `process_code`     varchar(64)   DEFAULT NULL             COMMENT '工序编码',
  `process_name`     varchar(255)  DEFAULT NULL             COMMENT '工序名称',
  -- 来源业务（Strategy 适配）
  `source_type`      varchar(20)   DEFAULT 'GENERIC'        COMMENT '来源类型:GENERIC-通用,SLITTING-分切,PRINTING-印刷',
  `source_ref_id`    bigint        DEFAULT NULL             COMMENT '来源业务单ID(如slitting_record.slit_id)',
  -- 状态
  `status`           varchar(20)   DEFAULT 'ISSUED'         COMMENT '状态:ISSUED-已发料,PROCESSING-加工中,RECEIVED-已收货,CLOSED-已关闭',
  `feedback_id`      bigint        DEFAULT NULL             COMMENT '收货时建的报工ID',
  -- 汇总
  `issue_total_qty`  decimal(14,4) DEFAULT 0.0000           COMMENT '发料总数量',
  `recpt_total_qty`  decimal(14,4) DEFAULT 0.0000           COMMENT '收货总数量',
  -- 操作
  `operator`         varchar(64)   DEFAULT NULL             COMMENT '发料操作人',
  `issue_time`       datetime      DEFAULT NULL             COMMENT '发料时间',
  `receive_time`     datetime      DEFAULT NULL             COMMENT '收货时间',
  `remark`           varchar(500)  DEFAULT ''               COMMENT '备注',
  `create_by`        varchar(64)   DEFAULT ''               COMMENT '创建者',
  `create_time`      datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`        varchar(64)   DEFAULT ''               COMMENT '更新者',
  `update_time`      datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  UNIQUE KEY `uk_order_code` (`order_code`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_vendor` (`vendor_id`),
  KEY `idx_workorder` (`workorder_id`),
  KEY `idx_card` (`card_id`),
  KEY `idx_status` (`status`),
  KEY `idx_source` (`source_type`, `source_ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用外协订单头表';

-- ════════════════════════════════════════════
-- 2. 发料行表（发出去的物料）
-- ════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_issue_line` (
  `line_id`          bigint        NOT NULL AUTO_INCREMENT  COMMENT '行ID',
  `factory_id`       bigint        NOT NULL                 COMMENT '工厂ID',
  `order_id`         bigint        NOT NULL                 COMMENT '外协订单ID',
  `item_id`          bigint        NOT NULL                 COMMENT '物料ID',
  `item_code`        varchar(64)   NOT NULL                 COMMENT '物料编码',
  `item_name`        varchar(255)  NOT NULL                 COMMENT '物料名称',
  `specification`    varchar(500)  DEFAULT NULL             COMMENT '规格型号',
  `unit_of_measure`  varchar(64)   NOT NULL                 COMMENT '单位编码',
  `unit_name`        varchar(64)   DEFAULT NULL             COMMENT '单位名称',
  `quantity`         decimal(14,4) NOT NULL                 COMMENT '发料数量',
  `batch_id`         bigint        DEFAULT NULL             COMMENT '批次ID',
  `batch_code`       varchar(64)   DEFAULT NULL             COMMENT '批次编码',
  `warehouse_id`     bigint        DEFAULT NULL             COMMENT '发料仓库ID',
  `warehouse_code`   varchar(64)   DEFAULT NULL             COMMENT '仓库编码',
  `warehouse_name`   varchar(255)  DEFAULT NULL             COMMENT '仓库名称',
  -- 来源对象（如分切的母卷roll_id）
  `source_ref_type`  varchar(30)   DEFAULT NULL             COMMENT '来源对象类型:ROLL-纸卷,STOCK-库存记录',
  `source_ref_id`    bigint        DEFAULT NULL             COMMENT '来源对象ID(如roll_id)',
  `remark`           varchar(500)  DEFAULT ''               COMMENT '备注',
  `create_by`        varchar(64)   DEFAULT ''               COMMENT '创建者',
  `create_time`      datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协发料行表';

-- ════════════════════════════════════════════
-- 3. 收货行表（收回来的产出物）
-- ════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS `qxx_wm_outsource_recpt_line` (
  `line_id`          bigint        NOT NULL AUTO_INCREMENT  COMMENT '行ID',
  `factory_id`       bigint        NOT NULL                 COMMENT '工厂ID',
  `order_id`         bigint        NOT NULL                 COMMENT '外协订单ID',
  `item_id`          bigint        NOT NULL                 COMMENT '物料ID',
  `item_code`        varchar(64)   NOT NULL                 COMMENT '物料编码',
  `item_name`        varchar(255)  NOT NULL                 COMMENT '物料名称',
  `specification`    varchar(500)  DEFAULT NULL             COMMENT '规格型号',
  `unit_of_measure`  varchar(64)   NOT NULL                 COMMENT '单位编码',
  `unit_name`        varchar(64)   DEFAULT NULL             COMMENT '单位名称',
  `quantity`         decimal(14,4) NOT NULL                 COMMENT '收货数量',
  `batch_id`         bigint        DEFAULT NULL             COMMENT '批次ID',
  `batch_code`       varchar(64)   DEFAULT NULL             COMMENT '批次编码',
  `warehouse_id`     bigint        DEFAULT NULL             COMMENT '收货仓库ID',
  `warehouse_code`   varchar(64)   DEFAULT NULL             COMMENT '仓库编码',
  `warehouse_name`   varchar(255)  DEFAULT NULL             COMMENT '仓库名称',
  -- 扩展属性（分切子卷的门幅/克重等，JSON或独立列）
  `ext_attrs`        varchar(500)  DEFAULT NULL             COMMENT '扩展属性JSON(如分切子卷width/gsm)',
  -- 来源对象（如分切的子卷roll_id）
  `source_ref_type`  varchar(30)   DEFAULT NULL             COMMENT '产出对象类型:ROLL-纸卷',
  `source_ref_id`    bigint        DEFAULT NULL             COMMENT '产出对象ID(如子卷roll_id)',
  `remark`           varchar(500)  DEFAULT ''               COMMENT '备注',
  `create_by`        varchar(64)   DEFAULT ''               COMMENT '创建者',
  `create_time`      datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`line_id`),
  KEY `idx_factory_id` (`factory_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外协收货行表';

-- ════════════════════════════════════════════
-- 4. 字典
-- ════════════════════════════════════════════

-- 4.1 外协单状态
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '外协单状态', 'mes_outsource_status', '0', 'admin', NOW(), '通用外协订单生命周期'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_outsource_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '已发料', 'ISSUED', 'mes_outsource_status', '', 'warning', 'N', '0', 'admin', NOW(), '物料已发往厂商'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_status' AND dict_value = 'ISSUED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '加工中', 'PROCESSING', 'mes_outsource_status', '', 'primary', 'N', '0', 'admin', NOW(), '厂商已录入加工结果'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_status' AND dict_value = 'PROCESSING');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已收货', 'RECEIVED', 'mes_outsource_status', '', 'success', 'N', '0', 'admin', NOW(), '我方已收货入库'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_status' AND dict_value = 'RECEIVED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已关闭', 'CLOSED', 'mes_outsource_status', '', 'info', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_status' AND dict_value = 'CLOSED');

-- 4.2 外协来源类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '外协来源类型', 'mes_outsource_type', '0', 'admin', NOW(), '外协业务来源'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_outsource_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '通用', 'GENERIC', 'mes_outsource_type', '', '', 'Y', '0', 'admin', NOW(), '普通外协发料收货'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_type' AND dict_value = 'GENERIC');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '分切', 'SLITTING', 'mes_outsource_type', '', 'warning', 'N', '0', 'admin', NOW(), '外协分切(母卷→子卷)'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_type' AND dict_value = 'SLITTING');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '印刷', 'PRINTING', 'mes_outsource_type', '', 'primary', 'N', '0', 'admin', NOW(), '外协印刷'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_outsource_type' AND dict_value = 'PRINTING');

-- ════════════════════════════════════════════
-- 5. 自动编码规则：OUTSOURCE_CODE（OS + yyyyMMdd + 3位流水）
-- ════════════════════════════════════════════
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'OUTSOURCE_CODE', '外协单号', '格式:OS20260803001', 15, 'N', '0', 'L', '1'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code = 'OUTSOURCE_CODE');

SET @rid_os = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'OUTSOURCE_CODE' AND factory_id = 1 LIMIT 1);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_os, 1, 'FIXCHAR', 'PREFIX_OS', '前缀OS', 2, 'OS'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_os AND part_index = 1);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_os, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_os AND part_index = 2);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_os, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_os AND part_index = 3);

-- ════════════════════════════════════════════
-- 6. 菜单：仓库管理 → 外协管理（menu_id 从 28800 段取，避开现有）
-- ════════════════════════════════════════════
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 28800, '外协管理', 2002, 20, 'outsource', 'mes/wm/outsource/index', 1, 0, 'C', '0', '0', 'mes:wm:outsource:list', 'shopping', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28800);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28801, '外协查询', 28800, 1, 'F', '0', '0', 'mes:wm:outsource:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28801);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28802, '外协发货', 28800, 2, 'F', '0', '0', 'mes:wm:outsource:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28802);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28803, '外协收货', 28800, 3, 'F', '0', '0', 'mes:wm:outsource:receive', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28803);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 28804, '厂商录结果', 28800, 4, 'F', '0', '0', 'mes:wm:outsource:result', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 28804);

-- ════════════════════════════════════════════
-- 7. 角色授权（admin=1 factory_id=0；产线=11 factory_id=1；厂商=106 factory_id=1）
-- ════════════════════════════════════════════
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu WHERE menu_id IN (28800, 28801, 28802, 28803, 28804)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0);

INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, menu_id, 1 FROM sys_menu WHERE menu_id IN (28800, 28801, 28802, 28803)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 11 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 1);

-- 厂商角色(106)：只看列表+录结果+查询（不能发货/收货）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 106, menu_id, 1 FROM sys_menu WHERE menu_id IN (28800, 28801, 28804)
AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 106 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 1);
