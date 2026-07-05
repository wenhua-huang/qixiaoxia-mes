-- ============================================================
-- V40: 清理编码规则重复数据 & 补全缺失的8个编码规则
-- 问题: seed_autocode_rules.sql 被多次执行，11个规则各产生了9份副本
-- 修复: 删除重复(保留最小rule_id) + 补全缺失规则PUR_ORDER_CODE等
-- ============================================================

-- 1. 删除重复的 auto_code_parts (rule_id 230-317 是重复项)
DELETE FROM sys_auto_code_part WHERE rule_id BETWEEN 230 AND 317;

-- 2. 删除重复的 auto_code_rules
DELETE FROM sys_auto_code_rule WHERE rule_id BETWEEN 230 AND 317;

-- 3. 补全缺失的8个编码规则 (INSERT IGNORE 幂等，兼容多次部署)
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(1, 'PUR_ORDER_CODE',  '采购订单编码',   '采购订单号自动生成',                20, 'N', '0', 'L', '1', 'admin', NOW()),
(1, 'TOOL_TYPE_CODE', '工装夹具类型编码', '格式:TLT001',                      10, '1', '0', 'L', '1', 'admin', NOW()),
(1, 'TOOL_CODE',      '工装夹具编码',    '格式:TL001',                       10, '1', '0', 'L', '1', 'admin', NOW()),
(1, 'CAL_PLAN_CODE',  '排班计划编码',    NULL,                               19, 'Y', NULL, 'L', '1', 'admin', NOW()),
(1, 'CAL_TEAM_CODE',  '班组编码',        NULL,                                8, 'Y', NULL, 'L', '1', 'admin', NOW()),
(1, 'ROUTE_CODE',     '工艺路线编码',    NULL,                               12, '1', '0', 'L', '1', 'admin', NOW()),
(1, 'PRO_CARD_CODE',  '流转卡编码',      '格式:CRD20260620001',              17, '1', '0', 'L', '1', 'admin', NOW()),
(1, 'SKU_CODE',       'SKU变体编码',     'SKU变体编码 格式:{父编码}-V{n}',    64, 'N', '0', 'L', '1', 'admin', NOW());

-- 4. 补全各规则的分段数据 (每个规则先取ID再插分段)
-- PUR_ORDER_CODE
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'PUR_ORDER_CODE' LIMIT 1);
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX',      '前缀',       2, NULL,         'PO', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',   '日期',       8, 'yyyyMMdd',  NULL,  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号',     3, NULL,         NULL,  1,    1,    'Y',  'DAY', 'admin', NOW());

-- TOOL_TYPE_CODE
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'TOOL_TYPE_CODE' LIMIT 1);
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_TLT', '固定前缀TLT', 3, 'TLT', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'SERIALNO', 'SERIAL_4',   '流水号4位',   4, NULL,  1,    1,    '0',  NULL, 'admin', NOW());

-- TOOL_CODE
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'TOOL_CODE' LIMIT 1);
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_TL', '固定前缀TL',  2, 'TL', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'SERIALNO', 'SERIAL_5',  '流水号5位',   5, NULL,  1,    1,    '0',  NULL, 'admin', NOW());

-- CAL_PLAN_CODE
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'CAL_PLAN_CODE' LIMIT 1);
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX',   NULL, 4, NULL,         'PLAN', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE',     NULL, 8, 'yyyyMMdd',  NULL,   NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL',   NULL, 4, NULL,         NULL,   1,    1,    NULL, NULL, 'admin', NOW());

-- CAL_TEAM_CODE
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'CAL_TEAM_CODE' LIMIT 1);
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX', NULL, 4, 'TEAM', NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'SERIALNO', 'SERIAL', NULL, 4, NULL,   1,    1,    NULL, NULL, 'admin', NOW());

-- ROUTE_CODE
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'ROUTE_CODE' LIMIT 1);
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  NULL, NULL, 3, 'RT-', NULL, NULL, '0', NULL, 'admin', NOW()),
(1, @rid, 2, 'SERIALNO', NULL, NULL, 3, NULL,  1,    1,    '0', NULL, 'admin', NOW());

-- PRO_CARD_CODE
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'PRO_CARD_CODE' LIMIT 1);
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'FIXCHAR',  'PREFIX_CRD',  '固定前缀CRD',   3, NULL,         'CRD', 1, 1, '1', 'DAY', 'admin', NOW()),
(1, @rid, 2, 'NOWDATE',  'DATE_PART',   '日期yyyyMMdd', 8, 'yyyyMMdd',  NULL,  NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号3位',     3, NULL,         NULL,  1, 1, '1', 'DAY', 'admin', NOW());

-- SKU_CODE
SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'SKU_CODE' LIMIT 1);
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(1, @rid, 1, 'INPUTCHAR', NULL, NULL, 30, NULL, NULL, NULL, NULL, 'admin', NOW()),
(1, @rid, 2, 'SERIALNO',  NULL, NULL,  3, 1,    1,    '1',  'OTHER', 'admin', NOW());
