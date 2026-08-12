-- ============================================================
-- V107：修正外协工序(is_outsource=1)被当成厂内工序的历史脏数据
--
-- 背景：V102 把外发判定从 process_type 迁到 route_process.is_outsource 后，
--       排产(ScheduleServiceImpl)和领料(generateIssueDocuments)未同步读 is_outsource，
--       导致外协工序被：(1) 分配厂内工作站  (2) 生成厂内领料单（与外协发料重复）。
--       本迁移修正已产生的脏数据。代码层防新增已在同批改动修复。
--
-- 改动：
--   1. 外协工序任务：清厂内 workstation，回填 vendor 信息，workstation_code='VENDOR'
--   2. 外协工序的 DRAFT 领料单（未发料）：连同行、生成日志作废删除
--
-- 幂等：UPDATE 无副作用（已 VENDOR 的不变）；DELETE 用状态+关联条件，重跑无目标行。
--       裸 JDBC 不走拦截器，但本迁移全是 UPDATE/DELETE（按 is_outsource/route 关联，无需 factory_id 注入）。
-- ============================================================

SET NAMES utf8mb4;

-- ════════════════════════════════════════════
-- 1. 外协工序任务：清厂内工作站，回填 vendor
--    条件：route_process.is_outsource='1' 且任务被分配了真实厂内工作站(workstation_id<>0)
--    回填 vendor 从 route_process 取（同 routeId+processId）
-- ════════════════════════════════════════════
UPDATE qxx_pro_task t
JOIN qxx_pro_route_process rp
  ON rp.route_id = t.route_id AND rp.process_id = t.process_id
SET
    t.workstation_id   = 0,
    t.workstation_code = 'VENDOR',
    t.workstation_name = COALESCE(rp.vendor_name, '外协'),
    t.vendor_id        = rp.vendor_id,
    t.vendor_code      = rp.vendor_code,
    t.outsource_factory_id = rp.outsource_factory_id,
    t.update_time      = NOW(),
    t.update_by        = 'system'
WHERE rp.is_outsource = '1'
  AND t.workstation_id <> 0;

-- ════════════════════════════════════════════
-- 2. 作废删除：外协工序的 DRAFT 厂内领料单
--    仅删 DRAFT（未发料，无库存影响）；已 ISSUED/POSTED 的保留（发料已成事实，人工处理）。
--    关联：issue_header.workorder_id + task.process_id → route_process.is_outsource='1'
-- ════════════════════════════════════════════

-- 2a. 删生成日志（先删，避免孤儿日志）
DELETE lg
FROM qxx_pro_doc_generation_log lg
JOIN qxx_wm_issue_header ih ON ih.issue_id = lg.doc_id
JOIN qxx_pro_task tk ON tk.task_id = ih.task_id
JOIN qxx_pro_route_process rp ON rp.route_id = tk.route_id AND rp.process_id = tk.process_id
WHERE lg.doc_type = 'ISSUE'
  AND rp.is_outsource = '1'
  AND ih.status = 'DRAFT';

-- 2b. 删领料行
DELETE il
FROM qxx_wm_issue_line il
JOIN qxx_wm_issue_header ih ON ih.issue_id = il.issue_id
JOIN qxx_pro_task tk ON tk.task_id = ih.task_id
JOIN qxx_pro_route_process rp ON rp.route_id = tk.route_id AND rp.process_id = tk.process_id
WHERE rp.is_outsource = '1'
  AND ih.status = 'DRAFT';

-- 2c. 删领料头
DELETE ih
FROM qxx_wm_issue_header ih
JOIN qxx_pro_task tk ON tk.task_id = ih.task_id
JOIN qxx_pro_route_process rp ON rp.route_id = tk.route_id AND rp.process_id = tk.process_id
WHERE rp.is_outsource = '1'
  AND ih.status = 'DRAFT';
