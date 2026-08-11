-- ============================================================
-- V108：修正外协分切单 source_type 历史脏数据
--
-- 背景：OutsourceIssueHelper.issueOutsourceForProcess 在工单开工自动建外协草稿时，
--       source_type 写死 'GENERIC'，未按工序码识别分切(PRC-SLIT)，导致分切外协单
--       在列表里显示"通用"，且录结果时走错 OutsourceResultStrategy（SLITTING 策略不生效）。
--       代码层已改为按 process_code 派生 source_type，本迁移回填历史数据。
--
-- 改动：process_code='PRC-SLIT' 且 source_type='GENERIC' 的外协单 → 'SLITTING'
-- 幂等：重跑无目标行（已 SLITTING 的不匹配）。裸 JDBC 不走拦截器，WHERE 带 factory_id IS NOT NULL。
-- ============================================================

SET NAMES utf8mb4;

UPDATE `qxx_wm_outsource_order`
SET `source_type` = 'SLITTING',
    `update_by`   = 'flyway',
    `update_time` = NOW()
WHERE `process_code` = 'PRC-SLIT'
  AND `source_type`  = 'GENERIC'
  AND `factory_id` IS NOT NULL;
