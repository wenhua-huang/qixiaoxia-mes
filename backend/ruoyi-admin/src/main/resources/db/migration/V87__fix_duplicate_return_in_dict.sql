-- =====================================================
-- V87: 删除事务类型字典中 RETURN_IN 的重复定义
-- =====================================================
-- 背景：
--   sys_dict_data 中 mes_wm_transaction_type 的 RETURN_IN 被定义了两次：
--     dict_code=323 标签"退料入库"（有 remark，正确）
--     dict_code=375 标签"退货入库"（无 remark，重复脏数据）
--   dict-tag 组件对同一 value 渲染时，把匹配到的两条字典都显示，
--   导致库存事务页面 RETURN_IN 类型显示"退料入库 / 退货入库"两个标签。
--
-- 用途确认：
--   代码中 RETURN_IN 仅用于退料入库（WmRtIssueServiceImpl.doExecuteReturn，
--   source_doc_type=RTISSUE）。供应商退货用的是独立的 ITEM_RTV 类型，不复用 RETURN_IN。
--   因此"退货入库"标签是纯重复脏数据，删除安全。
--
-- 幂等：DELETE 不存在时影响 0 行，重复执行安全。

-- 用 remark 为空定位重复脏数据（正确的"退料入库"那条有 remark 说明，重复的没有），
-- 不依赖 dict_code（跨环境 dict_code 可能不同），幂等。
DELETE FROM sys_dict_data
WHERE dict_type = 'mes_wm_transaction_type'
  AND dict_value = 'RETURN_IN'
  AND (remark IS NULL OR remark = '');
