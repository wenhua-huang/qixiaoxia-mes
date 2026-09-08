-- ============================================================
-- V147：把存量「自动分配」(workstation_code='AUTO') 的在制任务改派到具体机台
--
-- 背景：自动排产按 工作站.process_id = 工序id 匹配机台，匹配不到就写
--       workstation_id=0/code='AUTO'/name='自动分配'。大量任务因此未落实机台
--       （印刷因主数据存在重复工序 PRC-001(200)/PRC-PRINT(201)、机台挂在 201 上；
--        分切为配机台前的历史任务；贴绳工序无机台主数据）。
--       代码层已：① 自动排产未匹配机台改置 PENDING「待指派机台」；
--                 ② 新增按同名工序兜底匹配机台；③ 未指派机台禁止下发。
--       本迁移修复存量数据。
--
-- 步骤：
--   1) 能按 process_id 直接匹配机台的（如分切）→ 改派到该机台（同工序取最小机台id）
--   2) 按同名工序兜底匹配（如印刷 200→同名工序 201 挂的印刷机）
--   3) 仍无机台的（如贴绳）→ 置 PENDING「待指派机台」，等待人工指派
--   已完成/取消的历史任务不动。
-- 幂等：重跑无目标行（改派后 workstation_code 不再是 AUTO）。
--       裸 JDBC 不走 FactoryIdInterceptor，WHERE 带 factory_id IS NOT NULL。
-- ============================================================

SET NAMES utf8mb4;

-- 1) 直接按 process_id 匹配机台
UPDATE `qxx_pro_task` t
INNER JOIN (
  SELECT `process_id`, `factory_id`, MIN(`workstation_id`) AS ws_id
  FROM `qxx_md_workstation`
  WHERE `enable_flag` = '1' AND `process_id` IS NOT NULL
  GROUP BY `process_id`, `factory_id`
) m ON m.`process_id` = t.`process_id` AND m.`factory_id` = t.`factory_id`
INNER JOIN `qxx_md_workstation` w ON w.`workstation_id` = m.ws_id
SET t.`workstation_id`   = w.`workstation_id`,
    t.`workstation_code` = w.`workstation_code`,
    t.`workstation_name` = w.`workstation_name`,
    t.`update_by` = 'flyway',
    t.`update_time` = NOW()
WHERE t.`workstation_code` = 'AUTO'
  AND t.`status` NOT IN ('COMPLETED', 'CANCEL')
  AND t.`factory_id` IS NOT NULL;

-- 2) 同名工序兜底：任务工序与机台所挂工序同名但 id 不同（重复工序主数据）
UPDATE `qxx_pro_task` t
INNER JOIN `qxx_pro_process` tp ON tp.`process_id` = t.`process_id`
INNER JOIN `qxx_pro_process` sp
  ON sp.`process_name` = tp.`process_name`
  AND sp.`enable_flag` = '1'
  AND sp.`process_id` <> tp.`process_id`
INNER JOIN (
  SELECT `process_id`, `factory_id`, MIN(`workstation_id`) AS ws_id
  FROM `qxx_md_workstation`
  WHERE `enable_flag` = '1' AND `process_id` IS NOT NULL
  GROUP BY `process_id`, `factory_id`
) m ON m.`process_id` = sp.`process_id` AND m.`factory_id` = t.`factory_id`
INNER JOIN `qxx_md_workstation` w ON w.`workstation_id` = m.ws_id
SET t.`workstation_id`   = w.`workstation_id`,
    t.`workstation_code` = w.`workstation_code`,
    t.`workstation_name` = w.`workstation_name`,
    t.`update_by` = 'flyway',
    t.`update_time` = NOW()
WHERE t.`workstation_code` = 'AUTO'
  AND t.`status` NOT IN ('COMPLETED', 'CANCEL')
  AND t.`factory_id` IS NOT NULL;

-- 3) 仍无机台的在制任务 → 置「待指派机台」，由排产员人工指派后才能下发
UPDATE `qxx_pro_task`
SET `workstation_code` = 'PENDING',
    `workstation_name` = '待指派机台',
    `update_by` = 'flyway',
    `update_time` = NOW()
WHERE `workstation_code` = 'AUTO'
  AND `status` NOT IN ('COMPLETED', 'CANCEL')
  AND `factory_id` IS NOT NULL;
