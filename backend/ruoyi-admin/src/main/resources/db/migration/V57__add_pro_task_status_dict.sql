-- 生产任务状态字典 mes_pro_task_status
-- 供排产/报工页面通过 useDict('mes_pro_task_status') + dict-tag 翻译枚举值，
-- 取代前端硬编码 statusMap 映射（CLAUDE.md 字典规范）。
-- 状态值与 ProConstants.TASK_STATUS_* 对齐：PREPARE/NORMAL/PRODUCING/COMPLETED/PAUSED/CANCEL
-- 注：sys_dict_type/sys_dict_data 为 RuoYi 系统级表，无 factory_id。

-- 1. 字典类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '任务状态', 'mes_pro_task_status', '0', 'system', NOW(), '生产排产任务状态'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_pro_task_status'
);

-- 2. 字典数据
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待排产', 'PREPARE', 'mes_pro_task_status', '', 'info', 'N', '0', 'system', NOW(), '未排产'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_task_status' AND dict_value = 'PREPARE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '正常', 'NORMAL', 'mes_pro_task_status', '', 'success', 'Y', '0', 'system', NOW(), '已排产待生产'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_task_status' AND dict_value = 'NORMAL');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '生产中', 'PRODUCING', 'mes_pro_task_status', '', 'warning', 'N', '0', 'system', NOW(), '生产中'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_task_status' AND dict_value = 'PRODUCING');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已完成', 'COMPLETED', 'mes_pro_task_status', '', 'success', 'N', '0', 'system', NOW(), '已完成'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_task_status' AND dict_value = 'COMPLETED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '暂停', 'PAUSED', 'mes_pro_task_status', '', 'danger', 'N', '0', 'system', NOW(), '暂停'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_task_status' AND dict_value = 'PAUSED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '取消', 'CANCEL', 'mes_pro_task_status', '', 'info', 'N', '0', 'system', NOW(), '取消'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_pro_task_status' AND dict_value = 'CANCEL');
