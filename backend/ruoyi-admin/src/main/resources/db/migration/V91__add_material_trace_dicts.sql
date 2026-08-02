-- ============================================================
-- V91：物料追溯字典（trace_type 追溯事件类型 + node_type 节点类型）
-- 用途：前端 materialtrace 页用 useDict 替换硬编码 Record<string,string> 映射
--       （frontend/AGENTS.md 禁止硬编码枚举映射）
-- 幂等：ALTER 前查 information_schema；INSERT 用 WHERE NOT EXISTS 防重。
-- 注：sys_dict_type/sys_dict_data 为 RuoYi 系统级表，无 factory_id 列。
-- ============================================================

SET NAMES utf8mb4;

-- ============ 1. 追溯事件类型 mes_material_trace_type ============
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '追溯事件类型', 'mes_material_trace_type', '0', 'admin', NOW(), '物料追溯流转事件类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_material_trace_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '投料消耗', 'ISSUE', 'mes_material_trace_type', '', 'warning', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'ISSUE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '生产产出', 'PRODUCE', 'mes_material_trace_type', '', 'success', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'PRODUCE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '工序加工', 'PROCESS', 'mes_material_trace_type', '', 'info', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'PROCESS');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '外协加工', 'OUTSOURCE_PROCESS', 'mes_material_trace_type', '', 'warning', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'OUTSOURCE_PROCESS');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '合并', 'MERGE', 'mes_material_trace_type', '', 'info', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'MERGE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '调整', 'ADJUST', 'mes_material_trace_type', '', 'danger', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'ADJUST');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '采购入库', 'RECEIPT', 'mes_material_trace_type', '', '', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'RECEIPT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 8, '外协发料', 'OUTSOURCE_ISSUE', 'mes_material_trace_type', '', 'warning', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'OUTSOURCE_ISSUE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 9, '外协入库', 'OUTSOURCE_RECPT', 'mes_material_trace_type', '', 'success', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'OUTSOURCE_RECPT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 10, '生产退料', 'RETURN', 'mes_material_trace_type', '', 'danger', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'RETURN');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 11, '销售出库', 'SALES_OUT', 'mes_material_trace_type', '', 'danger', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'SALES_OUT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 12, '产出入库', 'PRODUCE_STOCKIN', 'mes_material_trace_type', '', 'success', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_type' AND dict_value = 'PRODUCE_STOCKIN');

-- ============ 2. 追溯节点类型 mes_material_trace_node_type ============
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '追溯节点类型', 'mes_material_trace_node_type', '0', 'admin', NOW(), '物料追溯链路节点类型'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_material_trace_node_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '流转卡', 'CARD', 'mes_material_trace_node_type', '', 'warning', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'CARD');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '库存记录', 'MATERIAL_STOCK', 'mes_material_trace_node_type', '', '', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'MATERIAL_STOCK');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '报工记录', 'FEEDBACK', 'mes_material_trace_node_type', '', 'success', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'FEEDBACK');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '采购订单', 'PUR_ORDER', 'mes_material_trace_node_type', '', 'info', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'PUR_ORDER');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '供应商', 'VENDOR', 'mes_material_trace_node_type', '', 'info', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'VENDOR');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '批次', 'BATCH', 'mes_material_trace_node_type', '', '', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'BATCH');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 7, '工单', 'WORKORDER', 'mes_material_trace_node_type', '', 'warning', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'WORKORDER');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 8, '销售出库', 'SALES_OUT', 'mes_material_trace_node_type', '', 'danger', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'SALES_OUT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 9, '未知', 'NONE', 'mes_material_trace_node_type', '', 'info', 'N', '0', 'admin', NOW(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_material_trace_node_type' AND dict_value = 'NONE');
