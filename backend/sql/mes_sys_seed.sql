-- ============================================================
-- 企小侠文化纸盒MES系统 — mes-sys 种子数据
-- 用途：预置常用编码规则及分段，系统初始化后可立即使用
-- 重要：导入前确保客户端字符集为 utf8mb4（mysql --default-character-set=utf8mb4）
-- ============================================================

SET NAMES utf8mb4;

-- 预置编码规则（使用 INSERT IGNORE 兼容重复执行）
INSERT IGNORE INTO sys_auto_code_rule (rule_id, factory_id, rule_code, rule_name, rule_desc, max_length, is_padded, padded_char, padded_method, enable_flag, create_by, create_time) VALUES
(201, 1, 'ORDER_NO',      '销售订单号',  '格式:SO20260611001',    15, '1', '0', 'L', '1', 'admin', NOW()),
(202, 1, 'WORKORDER_NO',  '生产工单号',  '格式:WO20260611001',    15, '1', '0', 'L', '1', 'admin', NOW()),
(203, 1, 'RECEIPT_NO',    '收货单号',   '格式:RCVT20260611001',  17, '1', '0', 'L', '1', 'admin', NOW()),
(204, 1, 'PUR_ORDER_CODE', '采购订单号',  '格式:PO202606110001',   16, '1', '0', 'L', '1', 'admin', NOW());

-- 销售订单号分段：SO + yyyyMMdd + 3位流水号（按日循环）
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(301, 1, 201, 1, 'FIXCHAR',  'PREFIX_SO',     '固定前缀SO',    2, NULL, NULL, 'SO', NULL, NULL, NULL, NULL,    'admin', NOW()),
(302, 1, 201, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(303, 1, 201, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',    3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());

-- 生产工单号分段：WO + yyyyMMdd + 3位流水号（按日循环）
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(304, 1, 202, 1, 'FIXCHAR',  'PREFIX_WO',     '固定前缀WO',    2, NULL, NULL, 'WO', NULL, NULL, NULL, NULL,    'admin', NOW()),
(305, 1, 202, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(306, 1, 202, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',    3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());

-- 收货单号分段：RCVT + yyyyMMdd + 3位流水号（按日循环）
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(307, 1, 203, 1, 'FIXCHAR',  'PREFIX_RCVT',   '固定前缀RCVT',  4, NULL, NULL, 'RCVT', NULL, NULL, NULL, NULL,  'admin', NOW()),
(308, 1, 203, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(309, 1, 203, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(3位)',    3, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());

-- 采购订单号分段：PO + yyyyMMdd + 4位流水号（按日循环）
INSERT IGNORE INTO sys_auto_code_part (part_id, factory_id, rule_id, part_index, part_type, part_code, part_name, part_length, date_format, input_character, fix_character, seria_start_no, seria_step, cycle_flag, cycle_method, create_by, create_time) VALUES
(310, 1, 204, 1, 'FIXCHAR',  'PREFIX_PO',     '固定前缀PO',    2, NULL, NULL, 'PO', NULL, NULL, NULL, NULL,    'admin', NOW()),
(311, 1, 204, 2, 'NOWDATE',  'DATE_PART',     '日期(yyyyMMdd)', 8, 'yyyyMMdd', NULL, NULL, NULL, NULL, NULL, NULL,  'admin', NOW()),
(312, 1, 204, 3, 'SERIALNO', 'SERIAL_PART',   '流水号(4位)',    4, NULL, NULL, NULL, 1,    1,    '1',   'DAY',  'admin', NOW());
