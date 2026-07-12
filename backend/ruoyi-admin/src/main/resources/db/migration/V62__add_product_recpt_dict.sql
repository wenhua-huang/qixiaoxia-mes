-- V62__add_product_recpt_dict.sql
-- 补充产品入库事务类型字典值（配套 TransactionTypeEnum.PRODUCT_RECPT）
-- 库存事务 qxx_wm_transaction.transaction_type='PRODUCT_RECPT' 时，前端 dict-tag 正确显示
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time)
SELECT 19, '产品入库', 'PRODUCT_RECPT', 'mes_wm_transaction_type', '', 'success', 'N', '0', 'admin', sysdate()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data WHERE dict_type = 'mes_wm_transaction_type' AND dict_value = 'PRODUCT_RECPT'
);
