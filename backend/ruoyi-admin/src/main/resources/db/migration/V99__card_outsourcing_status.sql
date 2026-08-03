-- ============================================================
-- V99：流转卡状态补「外协中」
-- 用途：外协分切发料时流转卡状态置 OUTSOURCING，收货时恢复 ACTIVE/COMPLETED
-- 让流转卡列表和生产看板能体现"正在外协分切"的异步进行中态
-- ============================================================

SET NAMES utf8mb4;

-- slitting_record 加 route_id 列（收货建报工判断末工序用，幂等）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qxx_pro_slitting_record' AND column_name = 'route_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE qxx_pro_slitting_record ADD COLUMN route_id bigint(20) DEFAULT NULL COMMENT ''工艺路线ID(关联qxx_pro_route)'' AFTER workorder_code',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 字典数据：mes_pro_card_status 补 OUTSOURCING（幂等）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '外协中', 'OUTSOURCING', 'mes_pro_card_status', '', 'warning', 'N', '0', 'admin', NOW(), '外协工序进行中(母卷已发出待收货)'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_card_status' AND dict_value = 'OUTSOURCING');
