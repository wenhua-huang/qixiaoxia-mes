-- ============================================================
-- 企小侠MES — 编码规则种子数据（Phase 3+4 补充）
-- 用途：为工序、排产、报工、流转卡、领料、退料、物料补充自动编码规则
-- 执行：mysql -h localhost -u root -p mes --default-character-set=utf8mb4 < seed_autocode_rules.sql
-- 注意：使用自增ID，不硬编码 rule_id/part_id，兼容多次执行
-- ============================================================

SET NAMES utf8mb4;

-- ═══════════════════════════════════════
-- 编码规则定义
-- ═══════════════════════════════════════

-- 工序编码：PRC + yyyyMMdd + 3位流水号
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'PROCESS_CODE',  '生产工序编码',  '格式:PRC20260620001',    15, '1', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_PRC',    '固定前缀PRC',     3, NULL,      NULL, 'PRC', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL,  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL,      NULL, NULL,  1,    1,    '1',   'DAY', 'admin', NOW());

-- 排产任务编码：TASK + yyyyMMdd + 3位流水号
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'TASK_CODE',     '排产任务编码',  '格式:TASK20260620001',   17, '1', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_TASK',   '固定前缀TASK',    4, NULL,      NULL, 'TASK', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL,  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL,      NULL, NULL,  1,    1,    '1',   'DAY', 'admin', NOW());

-- 报工记录编码：FB + yyyyMMdd + 3位流水号
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'FEEDBACK_CODE', '报工记录编码',  '格式:FB20260620001',     16, '1', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_FB',     '固定前缀FB',      2, NULL,      NULL, 'FB',  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL,  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL,      NULL, NULL,  1,    1,    '1',   'DAY', 'admin', NOW());

-- 流转卡编码：CRD + yyyyMMdd + 3位流水号
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'CARD_CODE',     '流转卡编码',   '格式:CRD20260620001',    17, '1', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_CRD',    '固定前缀CRD',     3, NULL,      NULL, 'CRD', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL,  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL,      NULL, NULL,  1,    1,    '1',   'DAY', 'admin', NOW());

-- 生产领料单编码：ISS + yyyyMMdd + 3位流水号
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'ISSUE_CODE',    '生产领料单编码','格式:ISS20260620001',    17, '1', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_ISS',    '固定前缀ISS',     3, NULL,      NULL, 'ISS', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL,  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL,      NULL, NULL,  1,    1,    '1',   'DAY', 'admin', NOW());

-- 生产退料单编码：RTI + yyyyMMdd + 3位流水号
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'RT_ISSUE_CODE', '生产退料单编码','格式:RTI20260620001',    17, '1', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_RTI',    '固定前缀RTI',     3, NULL,      NULL, 'RTI', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)',  8, 'yyyyMMdd', NULL, NULL,  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',     3, NULL,      NULL, NULL,  1,    1,    '1',   'DAY', 'admin', NOW());

-- ═══════════════════════════════════════
-- 物料编码按产品类型区分（5条规则）
-- ═══════════════════════════════════════

-- ITEM_RAW_CODE：RAW + 日期 + 流水
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'ITEM_RAW_CODE', '物料编码(原料)', 'RAW+日期+流水号', 20, 'N', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, date_format, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX',     '固定前缀RAW',     3, 'RAW',  NULL,      NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',  '日期(yyyyMMdd)',  8, NULL,   'yyyyMMdd', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)',    3, NULL,   NULL,      1,    1,    '1',   'DAY', 'admin', NOW());

-- ITEM_SEMI_CODE：SEMI + 日期 + 流水
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'ITEM_SEMI_CODE', '物料编码(半成品)', 'SEMI+日期+流水号', 20, 'N', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, date_format, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX',     '固定前缀SEMI',    4, 'SEMI', NULL,      NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',  '日期(yyyyMMdd)',  8, NULL,   'yyyyMMdd', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)',    3, NULL,   NULL,      1,    1,    '1',   'DAY', 'admin', NOW());

-- ITEM_FINISHED_CODE：FIN + 日期 + 流水
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'ITEM_FINISHED_CODE', '物料编码(成品)', 'FIN+日期+流水号', 20, 'N', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, date_format, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX',     '固定前缀FIN',     3, 'FIN',  NULL,      NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',  '日期(yyyyMMdd)',  8, NULL,   'yyyyMMdd', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)',    3, NULL,   NULL,      1,    1,    '1',   'DAY', 'admin', NOW());

-- ITEM_AUXILIARY_CODE：AUX + 日期 + 流水
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'ITEM_AUXILIARY_CODE', '物料编码(辅料)', 'AUX+日期+流水号', 20, 'N', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, date_format, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX',     '固定前缀AUX',     3, 'AUX',  NULL,      NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',  '日期(yyyyMMdd)',  8, NULL,   'yyyyMMdd', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)',    3, NULL,   NULL,      1,    1,    '1',   'DAY', 'admin', NOW());

-- ITEM_PACK_CODE：PACK + 日期 + 流水
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'ITEM_PACK_CODE', '物料编码(包材)', 'PACK+日期+流水号', 20, 'N', '0', 'L', '1', 'admin', NOW());
SET @rid = LAST_INSERT_ID();
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, date_format, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX',     '固定前缀PACK',    4, 'PACK', NULL,      NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',  '日期(yyyyMMdd)',  8, NULL,   'yyyyMMdd', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)',    3, NULL,   NULL,      1,    1,    '1',   'DAY', 'admin', NOW());
