-- ============================================================
-- 企小侠MES — 编码规则种子数据（Phase 3+4 补充）
-- 用途：为工序、排产、报工、流转卡、领料、退料补充自动编码规则
-- 执行：mysql -h localhost -u root -p mes < seed_autocode_rules.sql
-- ============================================================

SET NAMES utf8mb4;

-- ═══════════════════════════════════════
-- 编码规则定义（rule_id 从 204 开始，避免与 mes_sys_seed.sql 201-203 冲突）
-- ═══════════════════════════════════════

-- 工序编码：PRC + yyyyMMdd + 3位流水号
INSERT IGNORE INTO sys_auto_code_rule (rule_id, factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(204, 1, 'PROCESS_CODE',  '生产工序编码',  '格式:PRC20260620001',    15, '1', '0', 'L', '1', 'admin', NOW()),
(205, 1, 'TASK_CODE',     '排产任务编码',  '格式:TASK20260620001',   17, '1', '0', 'L', '1', 'admin', NOW()),
(206, 1, 'FEEDBACK_CODE', '报工记录编码',  '格式:FB20260620001',     16, '1', '0', 'L', '1', 'admin', NOW()),
(207, 1, 'CARD_CODE',     '流转卡编码',   '格式:CRD20260620001',    17, '1', '0', 'L', '1', 'admin', NOW()),
(208, 1, 'ISSUE_CODE',    '生产领料单编码','格式:ISS20260620001',    17, '1', '0', 'L', '1', 'admin', NOW()),
(209, 1, 'RT_ISSUE_CODE', '生产退料单编码','格式:RTI20260620001',    17, '1', '0', 'L', '1', 'admin', NOW());

-- ═══════════════════════════════════════
-- 编码分段定义（part_id 从 310 开始，避免与 mes_sys_seed.sql 301-309 冲突）
-- ═══════════════════════════════════════

-- ---- 工序编码 PROCESS_CODE：PRC + yyyyMMdd + 3位流水号 ----
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(310, 1, 204, 1, 'FIXCHAR',  'PREFIX_PRC',    '固定前缀PRC',     3, NULL, NULL, 'PRC', NULL, NULL, NULL, NULL,    'admin', NOW()),
(311, 1, 204, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(312, 1, 204, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());

-- ---- 排产任务编码 TASK_CODE：TASK + yyyyMMdd + 3位流水号 ----
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(313, 1, 205, 1, 'FIXCHAR',  'PREFIX_TASK',   '固定前缀TASK',    4, NULL, NULL, 'TASK', NULL, NULL, NULL, NULL,   'admin', NOW()),
(314, 1, 205, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(315, 1, 205, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());

-- ---- 报工编码 FEEDBACK_CODE：FB + yyyyMMdd + 3位流水号 ----
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(316, 1, 206, 1, 'FIXCHAR',  'PREFIX_FB',     '固定前缀FB',      2, NULL, NULL, 'FB', NULL, NULL, NULL, NULL,    'admin', NOW()),
(317, 1, 206, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(318, 1, 206, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());

-- ---- 流转卡编码 CARD_CODE：CRD + yyyyMMdd + 3位流水号 ----
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(319, 1, 207, 1, 'FIXCHAR',  'PREFIX_CRD',    '固定前缀CRD',     3, NULL, NULL, 'CRD', NULL, NULL, NULL, NULL,    'admin', NOW()),
(320, 1, 207, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(321, 1, 207, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());

-- ---- 领料单编码 ISSUE_CODE：ISS + yyyyMMdd + 3位流水号 ----
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(322, 1, 208, 1, 'FIXCHAR',  'PREFIX_ISS',    '固定前缀ISS',     3, NULL, NULL, 'ISS', NULL, NULL, NULL, NULL,    'admin', NOW()),
(323, 1, 208, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(324, 1, 208, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());

-- ---- 退料单编码 RT_ISSUE_CODE：RTI + yyyyMMdd + 3位流水号 ----
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(325, 1, 209, 1, 'FIXCHAR',  'PREFIX_RTI',    '固定前缀RTI',     3, NULL, NULL, 'RTI', NULL, NULL, NULL, NULL,    'admin', NOW()),
(326, 1, 209, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(327, 1, 209, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());
