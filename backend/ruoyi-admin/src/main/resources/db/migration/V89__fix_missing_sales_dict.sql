-- V89__fix_missing_sales_dict.sql
-- 修复 V80 迁移遗漏的两张字典:mes_wm_sales_status、mes_product_sales_type
--
-- 根因:
--   V80 写死 dict_id=206/207、dict_code=328-336,且用 INSERT IGNORE。
--   在已存在这些主键的老库(206=mes_pro_task_status/207=mes_wage_type/328-336=其他字典)
--   上会主键冲突静默丢弃,而 flyway_schema_history 仍记录 V80 success,导致
--   前端 useDict('mes_wm_sales_status') 返回空 → 销售出库页面搜索项/状态列不显示。
--
-- 修复:用 WHERE NOT EXISTS 幂等模式补插,不写死主键。

-- =====================================================
-- 1. mes_wm_sales_status — 销售出库单状态
-- =====================================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '销售出库单状态', 'mes_wm_sales_status', '0', 'admin', NOW(),
       '销售出库单生命周期:草稿/部分过账/已过账/已发货/已关闭/已作废'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_wm_sales_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '草稿', 'DRAFT', 'mes_wm_sales_status', '', 'info', 'Y', '0', 'admin', NOW(), '制单中,可编辑'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_sales_status' AND dict_value='DRAFT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '部分过账', 'PARTIAL_POSTED', 'mes_wm_sales_status', '', 'warning', 'N', '0', 'admin', NOW(), '分批出库中,部分库存已扣减'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_sales_status' AND dict_value='PARTIAL_POSTED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已过账', 'POSTED', 'mes_wm_sales_status', '', 'success', 'N', '0', 'admin', NOW(), '全量出库完成,已扣减库存'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_sales_status' AND dict_value='POSTED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已发货', 'SHIPPED', 'mes_wm_sales_status', '', 'primary', 'N', '0', 'admin', NOW(), '已登记物流发货'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_sales_status' AND dict_value='SHIPPED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已关闭', 'CLOSED', 'mes_wm_sales_status', '', 'info', 'N', '0', 'admin', NOW(), '已关闭,终态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_sales_status' AND dict_value='CLOSED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '已作废', 'CANCELED', 'mes_wm_sales_status', '', 'danger', 'N', '0', 'admin', NOW(), '已作废,终态'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_wm_sales_status' AND dict_value='CANCELED');


-- =====================================================
-- 2. mes_product_sales_type — 销售类型
-- =====================================================

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '销售类型', 'mes_product_sales_type', '0', 'admin', NOW(), '外贸/内贸/现货'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type WHERE dict_type = 'mes_product_sales_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '外贸', 'FOREIGN', 'mes_product_sales_type', '', 'primary', 'Y', '0', 'admin', NOW(), '出口外贸订单'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_product_sales_type' AND dict_value='FOREIGN');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '内贸', 'DOMESTIC', 'mes_product_sales_type', '', 'success', 'N', '0', 'admin', NOW(), '国内销售订单'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_product_sales_type' AND dict_value='DOMESTIC');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '现货', 'SPOT', 'mes_product_sales_type', '', 'warning', 'N', '0', 'admin', NOW(), '现货销售'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type='mes_product_sales_type' AND dict_value='SPOT');
