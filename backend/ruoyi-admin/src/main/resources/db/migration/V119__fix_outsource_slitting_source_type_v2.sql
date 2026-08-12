-- ============================================================
-- V119：修正外协分切单 source_type 历史脏数据（V108 补修）
--
-- 背景：V108 的 WHERE 用了 process_code='PRC-SLIT'，但种子数据 V16 里
--       分切工序 process_code 实际为 'SLITTING'，导致 V108 空跑 0 行，
--       分切外协单仍被标成 GENERIC，SlittingResultStrategy 不生效。
--       代码层已把 PROCESS_CODE_SLIT 统一改为 ProConstants.PROCESS_CODE_SLITTING
--       （"SLITTING"），本迁移回填历史数据。
--
-- 改动：process_code='SLITTING' 且 source_type='GENERIC' 的外协单 → 'SLITTING'
-- 幂等：重跑无目标行（已 SLITTING 的不匹配）。
--       裸 JDBC 不走 FactoryIdInterceptor，WHERE 带 factory_id IS NOT NULL。
-- 注意：仅修正来源类型标记，不追溯建子卷/分切记录（已按通用流程收货入账，
--       补建子卷无对应库存事务会造成账实不符）。
-- ============================================================

SET NAMES utf8mb4;

UPDATE `qxx_wm_outsource_order`
SET `source_type` = 'SLITTING',
    `update_by`   = 'flyway',
    `update_time` = NOW()
WHERE `process_code` = 'SLITTING'
  AND `source_type`  = 'GENERIC'
  AND `factory_id` IS NOT NULL;
