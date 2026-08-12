-- ============================================================
-- V92：纸张分切工序功能
--
-- 背景：分切是纸袋制造的关键工序（母卷->子卷），当前系统仅在工序类型枚举层
--       预留了 SLITTING，但无任何业务逻辑。本迁移落地分切功能的 DB 层：
--       1. 补回 V91 遗漏的 SLIT 追溯类型 + 新增 ROLL 节点类型
--       2. 新增分切状态字典
--       3. roll_detail 加 slit_batch_no 字段（同一次分切的子卷共享）
--       4. 新增分切记录头表 qxx_pro_slitting_record
--       5. 自动编码规则：SLITTING_CODE / ROLL_CODE
--       6. 菜单：生产管理 -> 分切作业
--
-- 幂等：ALTER 前查 information_schema；INSERT 用 WHERE NOT EXISTS 防重。
-- 注：sys_dict_type/sys_dict_data/sys_menu 为系统表，无 factory_id 列；
--     sys_role_menu 主键含 factory_id（NOT NULL），Flyway 裸 JDBC 必须显式写。
-- ============================================================

SET NAMES utf8mb4;

-- ════════════════════════════════════════════
-- 1. 字典：补 SLIT 追溯类型 + ROLL 节点类型
-- ════════════════════════════════════════════

-- 1.1 追溯事件类型：补 SLIT（V91 遗漏，设计稿 mes-pro.sql 本有此值）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 13, '分切', 'SLIT', 'mes_material_trace_type', '', 'primary', 'N', '0', 'admin', NOW(), '母卷分切成子卷'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'SLIT');

-- 1.2 追溯节点类型：补 ROLL（纸卷节点，分切追溯需要）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 10, '纸卷', 'ROLL', 'mes_material_trace_node_type', '', 'primary', 'N', '0', 'admin', NOW(), '纸卷明细节点'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'ROLL');

-- ════════════════════════════════════════════
-- 2. 字典：分切记录状态
-- ════════════════════════════════════════════

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '分切记录状态', 'mes_pro_slitting_status', '0', 'admin', NOW(), '分切作业记录状态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_pro_slitting_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '已执行', 'EXECUTED', 'mes_pro_slitting_status', '', 'success', 'Y', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_slitting_status' AND dict_value = 'EXECUTED');

-- ════════════════════════════════════════════
-- 3. roll_detail 加 slit_batch_no 字段
-- ════════════════════════════════════════════

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_wm_roll_detail' AND column_name = 'slit_batch_no');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_wm_roll_detail ADD COLUMN slit_batch_no VARCHAR(64) DEFAULT NULL COMMENT ''分切批次号(同一次分切产生的子卷共享,便于按次查询)''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ════════════════════════════════════════════
-- 4. 新表：分切记录头表
-- ════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS qxx_pro_slitting_record (
  slit_id            bigint(20)      not null auto_increment    comment '分切记录ID',
  factory_id         bigint(20)      not null                   comment '工厂ID(关联qxx_md_factory)',
  slit_batch_no      varchar(64)     not null                   comment '分切批次号(唯一,如SL20260729001)',
  -- 关联报工
  feedback_id        bigint(20)      default null               comment '关联报工记录ID(关联qxx_pro_feedback)',
  -- 工单/工序/流转卡
  workorder_id       bigint(20)      default null               comment '生产工单ID(关联qxx_pro_workorder)',
  workorder_code     varchar(64)     default null               comment '生产工单编码',
  process_id         bigint(20)      default null               comment '工序ID(关联qxx_pro_process)',
  process_code       varchar(64)     default null               comment '工序编码',
  process_name       varchar(255)    default null               comment '工序名称',
  card_id            bigint(20)      default null               comment '流转卡ID(关联qxx_pro_card)',
  -- 母卷信息
  parent_roll_id     bigint(20)      not null                   comment '母卷ID(关联qxx_wm_roll_detail)',
  parent_roll_code   varchar(64)     default null               comment '母卷号',
  parent_item_id     bigint(20)      default null               comment '母卷物料ID',
  parent_item_code   varchar(64)     default null               comment '母卷物料编码',
  parent_item_name   varchar(255)    default null               comment '母卷物料名称',
  parent_width       varchar(20)     default null               comment '母卷门幅(mm)',
  parent_weight      decimal(14,4)   default 0.0000             comment '母卷分切前重量(吨)',
  -- 子卷汇总
  child_count        int(11)         default 0                  comment '子卷数量',
  child_total_weight decimal(14,4)   default 0.0000             comment '子卷总重量(吨)',
  -- 纸边/损耗
  edge_item_id       bigint(20)      default null               comment '纸边物料ID',
  edge_item_code     varchar(64)     default null               comment '纸边物料编码',
  edge_item_name     varchar(255)    default null               comment '纸边物料名称',
  edge_weight        decimal(14,4)   default 0.0000             comment '纸边重量(kg)',
  -- 重量校验
  loss_weight        decimal(14,4)   default 0.0000             comment '损耗重量=母卷-子卷-纸边(吨)',
  loss_rate          decimal(8,4)    default 0.0000             comment '损耗率(%)',
  -- 操作
  operator           varchar(64)     default null               comment '操作人',
  workstation_id     bigint(20)      default null               comment '工作站ID(分切设备)',
  slit_time          datetime        default null               comment '分切时间',
  status             varchar(20)     default 'EXECUTED'         comment '状态:EXECUTED-已执行',
  remark             varchar(500)    default ''                 comment '备注',
  create_by          varchar(64)     default ''                 comment '创建者',
  create_time        datetime        default current_timestamp  comment '创建时间',
  update_by          varchar(64)     default ''                 comment '更新者',
  update_time        datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (slit_id),
  unique key uk_slit_batch_no (slit_batch_no),
  key idx_factory_id (factory_id),
  key idx_parent_roll (parent_roll_id),
  key idx_workorder (workorder_id),
  key idx_feedback (feedback_id)
) engine=innodb auto_increment=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci comment = '分切作业记录表';

-- ════════════════════════════════════════════
-- 5. 自动编码规则：SLITTING_CODE / ROLL_CODE
-- ════════════════════════════════════════════

-- 5.1 分切批次号 SLITTING_CODE（SL + yyyyMMdd + 3位流水，每日重置）
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'SLITTING_CODE', '分切批次号', '格式:SL20260729001', 15, 'N', '0', 'L', '1'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code = 'SLITTING_CODE');

SET @rid_slit = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'SLITTING_CODE' AND factory_id = 1 LIMIT 1);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_slit, 1, 'FIXCHAR', 'PREFIX_SL', '前缀SL', 2, 'SL'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_slit AND part_index = 1);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_slit, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_slit AND part_index = 2);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_slit, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_slit AND part_index = 3);

-- 5.2 子卷号 ROLL_CODE（R + yyyyMMdd + 3位流水，每日重置）
INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'ROLL_CODE', '纸卷号', '分切子卷自动生成卷号', 14, 'N', '0', 'L', '1'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code = 'ROLL_CODE');

SET @rid_roll = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'ROLL_CODE' AND factory_id = 1 LIMIT 1);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid_roll, 1, 'FIXCHAR', 'PREFIX_R', '前缀R', 1, 'R'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_roll AND part_index = 1);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid_roll, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_roll AND part_index = 2);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid_roll, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid_roll AND part_index = 3);

-- ════════════════════════════════════════════
-- 6. 菜单：生产管理 -> 分切作业
-- ════════════════════════════════════════════

-- 6.1 C-menu（parent_id=2003 生产管理，menu_id=2310）
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
SELECT 2310, '分切作业', 2003, 10, 'slitting', 'mes/pro/slitting/index', 1, 0, 'C', '0', '0', 'mes:pro:slitting:list', 'scissors', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2310);

-- 6.2 F-menu 子按钮权限
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23101, '分切查询', 2310, 1, 'F', '0', '0', 'mes:pro:slitting:query', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23101);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23102, '分切执行', 2310, 2, 'F', '0', '0', 'mes:pro:slitting:add', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23102);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23103, '分切详情', 2310, 3, 'F', '0', '0', 'mes:pro:slitting:edit', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23103);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23104, '分切删除', 2310, 4, 'F', '0', '0', 'mes:pro:slitting:remove', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23104);

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, menu_type, visible, status, perms, create_by, create_time)
SELECT 23105, '分切导出', 2310, 5, 'F', '0', '0', 'mes:pro:slitting:export', 'admin', sysdate()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 23105);

-- 6.3 角色授权（sys_role_menu 主键含 factory_id，Flyway 裸 JDBC 必须显式写 factory_id=0）
INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 1, menu_id, 0 FROM sys_menu WHERE menu_id IN (
    2310, 23101, 23102, 23103, 23104, 23105
) AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
);

INSERT INTO sys_role_menu (role_id, menu_id, factory_id)
SELECT 11, menu_id, 0 FROM sys_menu WHERE menu_id IN (
    2310, 23101, 23102, 23103, 23104, 23105
) AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 11 AND rm.menu_id = sys_menu.menu_id AND rm.factory_id = 0
);
