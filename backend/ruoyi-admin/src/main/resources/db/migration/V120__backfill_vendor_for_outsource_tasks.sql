-- ============================================================
-- V120：回填外协任务缺失的 vendor 信息
--
-- 背景：feedbackEntry 接口原先用 vendorCode 是否非空判断外协任务，
--       但部分外协任务(qxx_pro_task.workstation_code='VENDOR')的
--       vendor_code 为 NULL，导致被误归为厂内任务，APP 报工页
--       不显示状态和进度。
--       代码层已改为按 workstation_code='VENDOR' 判断外协，
--       本迁移回填历史任务的 vendor_id/vendor_code/outsource_factory_id。
--
-- 改动：对 workstation_code='VENDOR' 且 vendor_code IS NULL 的任务，
--       按 route_id + process_id 关联路线工序表，回填厂商信息。
-- 幂等：重跑无目标行（vendor_code 已非空的不匹配）。
--       裸 JDBC 不走 FactoryIdInterceptor，WHERE 带 factory_id IS NOT NULL。
-- 注意：部分外协工序本身未配厂商（vendor_id=NULL），回填后仍为 NULL，
--       但代码层已用 workstation_code 判断外协，不影响显示。
-- ============================================================

SET NAMES utf8mb4;

UPDATE `qxx_pro_task` t
INNER JOIN `qxx_pro_route_process` rp
  ON t.`route_id` = rp.`route_id`
  AND t.`process_id` = rp.`process_id`
  AND rp.`is_outsource` = '1'
SET t.`vendor_id` = COALESCE(t.`vendor_id`, rp.`vendor_id`),
    t.`vendor_code` = rp.`vendor_code`,
    t.`outsource_factory_id` = COALESCE(t.`outsource_factory_id`, rp.`outsource_factory_id`),
    t.`update_by` = 'flyway',
    t.`update_time` = NOW()
WHERE t.`workstation_code` = 'VENDOR'
  AND t.`vendor_code` IS NULL
  AND rp.`vendor_code` IS NOT NULL
  AND t.`factory_id` IS NOT NULL;
