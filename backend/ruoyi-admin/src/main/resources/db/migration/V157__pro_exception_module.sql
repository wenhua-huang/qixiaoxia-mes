-- ============================================================
-- V157: 生产异常模块（E1–E6 + D2）
--   1. 异常单表 qxx_pro_exception（四类异常同表，专属字段可空列）
--   2. qxx_pro_task 增加异常任务四标志列
--   3. 自动编码规则 PRO_EXCEPTION_CODE：EX + yyyyMMdd + '-' + 3位日流水
--   4. 字典 5 套
--   5. 菜单：异常台账(2314)/隐藏详情(2315) + F 权限 2316-2321 + 角色授权
-- 全部语句幂等（CREATE TABLE IF NOT EXISTS / WHERE NOT EXISTS）。
-- ============================================================

SET NAMES utf8mb4;

-- ══════════ 1. 异常单主表 ══════════
CREATE TABLE IF NOT EXISTS qxx_pro_exception (
    exception_id          bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    factory_id            bigint        NOT NULL COMMENT '工厂ID',
    exception_code        varchar(64)   NOT NULL COMMENT '异常单号(EX20260902-014)',
    exception_type        varchar(16)   NOT NULL COMMENT '异常类型 QUALITY质量数量/MATERIAL缺料/DELAY进度延迟/RETURN客户退货',
    status                varchar(16)   NOT NULL DEFAULT 'OPEN' COMMENT '状态 OPEN待处理/PROCESSING处理中/CLOSED已关闭',
    responsible_party     varchar(16)   NOT NULL DEFAULT 'PENDING' COMMENT '责任方 FACTORY本厂/SUPPLIER供应商/CUSTOMER客户/PENDING待定',

    -- 关联快照（一期全部来自工序任务；target_* 为多态预留，本期恒 TASK）
    workorder_id          bigint        DEFAULT NULL COMMENT '工单ID',
    workorder_code        varchar(64)   DEFAULT NULL COMMENT '工单编码快照',
    workorder_name        varchar(255)  DEFAULT NULL COMMENT '工单名称快照',
    task_id               bigint        DEFAULT NULL COMMENT '工序任务ID',
    task_code             varchar(64)   DEFAULT NULL COMMENT '任务编号快照',
    process_id            bigint        DEFAULT NULL COMMENT '工序ID',
    process_code          varchar(64)   DEFAULT NULL COMMENT '工序编码快照',
    process_name          varchar(128)  DEFAULT NULL COMMENT '工序名称快照',
    target_type           varchar(16)   DEFAULT 'TASK' COMMENT '关联对象类型(预留:二期挂销售订单)',
    target_id             bigint        DEFAULT NULL COMMENT '关联对象ID',
    target_code           varchar(64)   DEFAULT NULL COMMENT '关联对象编号',

    -- 上报信息
    reporter_id           bigint        DEFAULT NULL COMMENT '上报人用户ID',
    reporter_name         varchar(64)   DEFAULT NULL COMMENT '上报人账号快照',
    occur_time            datetime      DEFAULT NULL COMMENT '异常发生时间',
    impact_quantity       decimal(14,2) DEFAULT NULL COMMENT '影响数量',
    description           varchar(1000) DEFAULT NULL COMMENT '异常情况说明',
    scene_images          varchar(1000) DEFAULT NULL COMMENT '现场照片(MinIO URL逗号串)',

    -- 质量数量异常专属
    quality_subclass      varchar(16)   DEFAULT NULL COMMENT '质量小类 BROKEN做坏了/SHORT做少了',
    usable_quantity       decimal(14,2) DEFAULT NULL COMMENT '可使用数量',
    need_rework           char(1)       DEFAULT NULL COMMENT '是否需要返工 Y/N',

    -- 缺料异常专属
    item_id               bigint        DEFAULT NULL COMMENT '缺料物料ID',
    item_code             varchar(64)   DEFAULT NULL COMMENT '缺料物料编码',
    item_name             varchar(255)  DEFAULT NULL COMMENT '缺料物料名称',
    shortage_quantity     decimal(14,2) DEFAULT NULL COMMENT '缺料数量',
    expected_arrival_date date          DEFAULT NULL COMMENT '预计到货日期',

    -- 进度延迟异常专属
    original_plan_time    datetime      DEFAULT NULL COMMENT '原计划完成时间',
    new_expected_time     datetime      DEFAULT NULL COMMENT '新预计完成时间',
    delay_reason          varchar(500)  DEFAULT NULL COMMENT '延迟原因',

    -- 客户退货异常专属
    return_quantity       decimal(14,2) DEFAULT NULL COMMENT '退货数量',
    return_reason         varchar(500)  DEFAULT NULL COMMENT '退货原因',
    customer_accept_rework char(1)      DEFAULT NULL COMMENT '客户是否接受返工 Y/N',

    -- 处理与关闭
    conclusion            varchar(1000) DEFAULT NULL COMMENT '处理结论',
    resolve_type          varchar(16)   DEFAULT NULL COMMENT '出口动作 REWORK返工/REMAKE补做/PURCHASE补料采购/RESCHEDULE顺延/SCRAP报废/CONCESSION让步接收/REFUND退款结单',
    resolve_by            varchar(64)   DEFAULT NULL COMMENT '选出口处理人',
    resolve_time          datetime      DEFAULT NULL COMMENT '选出口时间',
    target_doc_type       varchar(32)   DEFAULT NULL COMMENT '处理产生单据类型 TASK/PUR_ORDER',
    target_doc_id         bigint        DEFAULT NULL COMMENT '处理产生单据ID',
    target_doc_code       varchar(64)   DEFAULT NULL COMMENT '处理产生单据编号',
    scrap_quantity        decimal(14,2) DEFAULT NULL COMMENT '报废数量',
    close_by              varchar(64)   DEFAULT NULL COMMENT '关闭人',
    close_time            datetime      DEFAULT NULL COMMENT '关闭时间',

    create_by             varchar(64)   DEFAULT NULL COMMENT '创建者',
    create_time           datetime      DEFAULT NULL COMMENT '创建时间',
    update_by             varchar(64)   DEFAULT NULL COMMENT '更新者',
    update_time           datetime      DEFAULT NULL COMMENT '更新时间',
    remark                varchar(500)  DEFAULT NULL COMMENT '备注',

    PRIMARY KEY (exception_id),
    UNIQUE KEY uk_factory_code (factory_id, exception_code),
    KEY idx_wo_status (workorder_id, status),
    KEY idx_task (task_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产异常单';

-- ══════════ 2. qxx_pro_task 异常任务标志列（PROCEDURE 守卫，幂等，同 V142） ══════════
DROP PROCEDURE IF EXISTS add_col_if_missing;
CREATE PROCEDURE add_col_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=tbl AND column_name=col) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

DROP PROCEDURE IF EXISTS add_index_if_missing;
CREATE PROCEDURE add_index_if_missing(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=tbl AND index_name=idx) THEN
        SET @s = ddl; PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END;

CALL add_col_if_missing('qxx_pro_task', 'is_exception',
    'ALTER TABLE qxx_pro_task ADD COLUMN is_exception CHAR(1) DEFAULT ''N'' COMMENT ''是否异常(返工/补做)任务 Y/N''');
CALL add_col_if_missing('qxx_pro_task', 'exception_id',
    'ALTER TABLE qxx_pro_task ADD COLUMN exception_id BIGINT NULL COMMENT ''来源异常单ID''');
CALL add_col_if_missing('qxx_pro_task', 'exception_code',
    'ALTER TABLE qxx_pro_task ADD COLUMN exception_code VARCHAR(64) NULL COMMENT ''来源异常单号''');
CALL add_col_if_missing('qxx_pro_task', 'origin_task_id',
    'ALTER TABLE qxx_pro_task ADD COLUMN origin_task_id BIGINT NULL COMMENT ''原工序任务ID(-E序号计数用)''');
CALL add_index_if_missing('qxx_pro_task', 'idx_exception',
    'CREATE INDEX idx_exception ON qxx_pro_task(exception_id)');
CALL add_index_if_missing('qxx_pro_task', 'idx_origin',
    'CREATE INDEX idx_origin ON qxx_pro_task(origin_task_id)');

DROP PROCEDURE IF EXISTS add_col_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;

-- ══════════ 3. 自动编码规则 PRO_EXCEPTION_CODE ══════════
-- EX + yyyyMMdd + '-' + 3位流水(按日循环) → EX20260902-014；不整体补长，靠流水段补零
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time)
SELECT 1, 'PRO_EXCEPTION_CODE', '生产异常单编码', '格式:EX20260902-014', 20, 'N', '0', 'L', '1', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code='PRO_EXCEPTION_CODE' AND factory_id=1);
SET @rid_ex = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code='PRO_EXCEPTION_CODE' AND factory_id=1 LIMIT 1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_ex, 1, 'FIXCHAR', 'PREFIX_EX', '固定前缀EX', 2, 'EX'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_ex AND part_index=1);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_ex, 2, 'NOWDATE', 'DATE_PART', '日期(yyyyMMdd)', 8, 'yyyyMMdd'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_ex AND part_index=2);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_ex, 3, 'FIXCHAR', 'SEP_PART', '分隔符-', 1, '-'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_ex AND part_index=3);
INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_ex, 4, 'SERIALNO', 'SERIAL_PART', '流水号(3位按日)', 3, 1, 1, '1', 'DAY'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id=@rid_ex AND part_index=4);

-- ══════════ 4. 字典（系统表无 factory_id） ══════════
-- 4.1 异常类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '生产异常类型', 'mes_pro_exception_type', '0', 'admin', NOW(), '质量数量/缺料/延迟/退货'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_pro_exception_type');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '质量数量', 'QUALITY', 'mes_pro_exception_type', '', 'danger', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_type' AND dict_value='QUALITY');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '缺料', 'MATERIAL', 'mes_pro_exception_type', '', 'warning', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_type' AND dict_value='MATERIAL');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '进度延迟', 'DELAY', 'mes_pro_exception_type', '', 'primary', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_type' AND dict_value='DELAY');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '客户退货', 'RETURN', 'mes_pro_exception_type', '', 'info', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_type' AND dict_value='RETURN');

-- 4.2 责任方
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '生产异常责任方', 'mes_pro_exception_party', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_pro_exception_party');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '本厂工序', 'FACTORY', 'mes_pro_exception_party', '', 'danger', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_party' AND dict_value='FACTORY');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '供应商', 'SUPPLIER', 'mes_pro_exception_party', '', 'warning', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_party' AND dict_value='SUPPLIER');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '客户', 'CUSTOMER', 'mes_pro_exception_party', '', 'primary', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_party' AND dict_value='CUSTOMER');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 9, '待定', 'PENDING', 'mes_pro_exception_party', '', 'info', 'Y', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_party' AND dict_value='PENDING');

-- 4.3 异常单状态
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '生产异常单状态', 'mes_pro_exception_status', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_pro_exception_status');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待处理', 'OPEN', 'mes_pro_exception_status', '', 'danger', 'Y', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_status' AND dict_value='OPEN');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '处理中', 'PROCESSING', 'mes_pro_exception_status', '', 'warning', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_status' AND dict_value='PROCESSING');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已关闭', 'CLOSED', 'mes_pro_exception_status', '', 'success', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_status' AND dict_value='CLOSED');

-- 4.4 出口动作
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '生产异常出口动作', 'mes_pro_exception_resolve', '0', 'admin', NOW(), '4 回流 + 3 终结'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_pro_exception_resolve');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '开返工任务', 'REWORK', 'mes_pro_exception_resolve', '', 'primary', 'N', '0', 'admin', NOW(), '回流:建异常任务,处理中'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_resolve' AND dict_value='REWORK');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '开补做任务', 'REMAKE', 'mes_pro_exception_resolve', '', 'primary', 'N', '0', 'admin', NOW(), '回流:建异常任务,处理中'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_resolve' AND dict_value='REMAKE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '开补料采购', 'PURCHASE', 'mes_pro_exception_resolve', '', 'primary', 'N', '0', 'admin', NOW(), '回流:建DRAFT采购单,处理中'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_resolve' AND dict_value='PURCHASE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '顺延改期', 'RESCHEDULE', 'mes_pro_exception_resolve', '', 'warning', 'N', '0', 'admin', NOW(), '回流:改原任务计划完成时间,处理中'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_resolve' AND dict_value='RESCHEDULE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '报废', 'SCRAP', 'mes_pro_exception_resolve', '', 'info', 'N', '0', 'admin', NOW(), '终结:记报废数量直接关闭'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_resolve' AND dict_value='SCRAP');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '让步接收', 'CONCESSION', 'mes_pro_exception_resolve', '', 'success', 'N', '0', 'admin', NOW(), '终结:直接关闭'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_resolve' AND dict_value='CONCESSION');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '退款结单', 'REFUND', 'mes_pro_exception_resolve', '', 'info', 'N', '0', 'admin', NOW(), '终结:无财务实体,留痕关闭'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_resolve' AND dict_value='REFUND');

-- 4.5 质量异常小类
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '质量异常小类', 'mes_pro_exception_quality_sub', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type='mes_pro_exception_quality_sub');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '做坏了', 'BROKEN', 'mes_pro_exception_quality_sub', '', 'danger', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_quality_sub' AND dict_value='BROKEN');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '做少了', 'SHORT', 'mes_pro_exception_quality_sub', '', 'warning', 'N', '0', 'admin', NOW(), ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_pro_exception_quality_sub' AND dict_value='SHORT');

-- ══════════ 5. 菜单与权限（挂 2003 生产管理） ══════════
-- 5.1 台账页 + 隐藏详情页
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2314, '异常台账', 2003, 10, 'exception', 'mes/pro/exception/index', 1, 0, 'C', '0', '0', 'mes:pro:exception:list', 'warning', 'admin', NOW(), '生产异常单台账(D2)'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2314);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 2315, '异常单详情', 2003, 11, 'exception_detail', 'mes/pro/exception/detail', 1, 0, 'C', '1', '0', 'mes:pro:exception:query', 'view', 'admin', NOW(), '生产异常单详情(隐藏路由,台账行进入)'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2315);

-- 5.2 F 按钮权限（parent=2314）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2316, '异常单查询', 2314, 1, 'F', '0', '0', 'mes:pro:exception:query', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2316);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2317, '异常上报', 2314, 2, 'F', '0', '0', 'mes:pro:exception:add', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2317);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2318, '异常单补全', 2314, 3, 'F', '0', '0', 'mes:pro:exception:edit', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2318);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2319, '异常处理', 2314, 4, 'F', '0', '0', 'mes:pro:exception:handle', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2319);
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 2321, '异常单导出', 2314, 5, 'F', '0', '0', 'mes:pro:exception:export', 'admin', NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2321);

-- 5.3 角色授权（幂等，沿用角色 factory_id）
-- 台账/详情/查询/补全/处理/导出：授给现持有排产任务(2302)的角色
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT rm.role_id, m.menu_id, rm.factory_id
FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id IN (2314, 2315, 2316, 2318, 2319, 2321)
WHERE rm.menu_id = 2302
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu x
      WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id AND x.factory_id = rm.factory_id
  );

-- 异常上报(手机端)：授给现持有生产报工(2303)的角色
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT rm.role_id, 2317, rm.factory_id
FROM sys_role_menu rm
WHERE rm.menu_id = 2303
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu x
      WHERE x.role_id = rm.role_id AND x.menu_id = 2317 AND x.factory_id = rm.factory_id
  );
