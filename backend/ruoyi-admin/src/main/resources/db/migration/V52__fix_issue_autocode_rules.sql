-- ============================================================
-- 补全生产领料单/退料单自动编码规则（ISSUE_CODE / RT_ISSUE_CODE）
-- 背景：V15 定义了种子但实际库缺失（疑似 V40 清理误删），
--       导致 WmIssueHeaderServiceImpl.insertWmIssueHeader 调用 genSerialCode("ISSUE_CODE") 报"规则不存在"
-- 幂等：INSERT IGNORE + 规则已存在则跳过；分段用子查询关联 rule_id，避免 LAST_INSERT_ID 串号
-- ============================================================

SET NAMES utf8mb4;

-- ══════════════════════════════════════════════
-- 1. 生产领料单编码 ISSUE_CODE：ISS + yyyyMMdd + 3位流水
-- ══════════════════════════════════════════════
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time)
VALUES (1, 'ISSUE_CODE', '生产领料单编码', '格式:ISS20260620001', 17, '1', '0', 'L', '1', 'admin', NOW());

-- 分段（仅在规则存在且分段未建时插入）
INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time)
SELECT 1, r.rule_id, 1, 'FIXCHAR', 'PREFIX_ISS', '固定前缀ISS', 3, NULL, NULL, 'ISS', NULL, NULL, NULL, NULL, 'admin', NOW()
FROM sys_auto_code_rule r WHERE r.rule_code = 'ISSUE_CODE'
  AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id = r.rule_id AND p.part_index = 1);

INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time)
SELECT 1, r.rule_id, 2, 'NOWDATE', 'DATE_PART', '日期(yyyyMMdd)', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, 'admin', NOW()
FROM sys_auto_code_rule r WHERE r.rule_code = 'ISSUE_CODE'
  AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id = r.rule_id AND p.part_index = 2);

INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time)
SELECT 1, r.rule_id, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)', 3, NULL, NULL, NULL, 1, 1, '1', 'DAY', 'admin', NOW()
FROM sys_auto_code_rule r WHERE r.rule_code = 'ISSUE_CODE'
  AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id = r.rule_id AND p.part_index = 3);

-- ══════════════════════════════════════════════
-- 2. 生产退料单编码 RT_ISSUE_CODE：RTI + yyyyMMdd + 3位流水
-- ══════════════════════════════════════════════
INSERT IGNORE INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time)
VALUES (1, 'RT_ISSUE_CODE', '生产退料单编码', '格式:RTI20260620001', 17, '1', '0', 'L', '1', 'admin', NOW());

INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time)
SELECT 1, r.rule_id, 1, 'FIXCHAR', 'PREFIX_RTI', '固定前缀RTI', 3, NULL, NULL, 'RTI', NULL, NULL, NULL, NULL, 'admin', NOW()
FROM sys_auto_code_rule r WHERE r.rule_code = 'RT_ISSUE_CODE'
  AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id = r.rule_id AND p.part_index = 1);

INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time)
SELECT 1, r.rule_id, 2, 'NOWDATE', 'DATE_PART', '日期(yyyyMMdd)', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL, 'admin', NOW()
FROM sys_auto_code_rule r WHERE r.rule_code = 'RT_ISSUE_CODE'
  AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id = r.rule_id AND p.part_index = 2);

INSERT IGNORE INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time)
SELECT 1, r.rule_id, 3, 'SERIALNO', 'SERIAL_PART', '流水号(3位)', 3, NULL, NULL, NULL, 1, 1, '1', 'DAY', 'admin', NOW()
FROM sys_auto_code_rule r WHERE r.rule_code = 'RT_ISSUE_CODE'
  AND NOT EXISTS (SELECT 1 FROM sys_auto_code_part p WHERE p.rule_id = r.rule_id AND p.part_index = 3);
