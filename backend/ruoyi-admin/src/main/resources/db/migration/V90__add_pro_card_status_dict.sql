-- ============================================================
-- V90：流转卡状态字典
-- 用途：前端用 useDict 替换硬编码 Record<string,string> 映射（frontend/AGENTS.md 禁止硬编码枚举映射）
-- 对应 qxx_pro_card.status：ACTIVE(流转中) / COMPLETED(已完工) / SCRAPPED(已报废)
-- ============================================================

SET NAMES utf8mb4;

-- 1. 字典类型（幂等：不存在才插入；sys_dict_* 是系统表，无 factory_id 列）
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '流转卡状态', 'mes_pro_card_status', '0', 'admin', NOW(), '生产流转卡状态：流转中/已完工/已报废'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_pro_card_status');

-- 2. 字典数据（幂等：每条 WHERE NOT EXISTS 防重）
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '流转中', 'ACTIVE', 'mes_pro_card_status', '', '', 'Y', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_card_status' AND dict_value = 'ACTIVE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已完工', 'COMPLETED', 'mes_pro_card_status', '', 'success', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_card_status' AND dict_value = 'COMPLETED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已报废', 'SCRAPPED', 'mes_pro_card_status', '', 'danger', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_card_status' AND dict_value = 'SCRAPPED');
