-- V47: 新增 BATCH_CODE 自动编码规则（批次号生成）
-- 格式: BAT + yyyyMMdd + 3位流水号，每日重置

INSERT INTO sys_auto_code_rule (factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag)
SELECT 1, 'BATCH_CODE', '批次号', '入库时自动生成批次号', 20, 'N', '0', 'L', '1'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_rule WHERE rule_code = 'BATCH_CODE');

SET @rid = (SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'BATCH_CODE' AND factory_id = 1 LIMIT 1);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, fix_character)
SELECT 1, @rid, 1, 'FIXCHAR', 'PREFIX', '前缀', 3, 'BAT'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid AND part_index = 1);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format)
SELECT 1, @rid, 2, 'NOWDATE', 'DATE_PART', '日期', 8, 'yyyyMMdd'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid AND part_index = 2);

INSERT INTO sys_auto_code_part (factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, seria_start_no, seria_step, cycle_flag, cycle_method)
SELECT 1, @rid, 3, 'SERIALNO', 'SERIAL_PART', '流水号', 3, 1, 1, 'Y', 'DAY'
WHERE NOT EXISTS (SELECT 1 FROM sys_auto_code_part WHERE rule_id = @rid AND part_index = 3);
