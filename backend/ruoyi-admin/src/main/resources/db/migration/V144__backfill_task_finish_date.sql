-- V144: 回填历史已完工任务的 finish_date
--
-- 背景：completeTask / 报工自动完工此前只写 task.status=COMPLETED，未写 finish_date，
--      导致生产报表按 t.finish_date BETWEEN 统计完工量/延期时全部落空。
--      代码已修复（完工时一并 setFinishDate），此处用 update_time 兜底回填历史数据。
-- 幂等：只更新 finish_date IS NULL 的完工任务，重跑安全。纯 UPDATE，无需 factory_id 注入。

SET NAMES utf8mb4;

UPDATE qxx_pro_task
SET finish_date = update_time
WHERE status = 'COMPLETED'
  AND finish_date IS NULL
  AND update_time IS NOT NULL;
