-- V141: 工单进度跟踪与统计 —— 班组快照/索引/菜单/配置

-- 1. 报工：补报工人 user_id + 班组快照
ALTER TABLE qxx_pro_feedback
  ADD COLUMN user_id   BIGINT       NULL COMMENT '报工人用户ID(关联sys_user)' AFTER user_name,
  ADD COLUMN team_id   BIGINT       NULL COMMENT '班组ID(快照)' AFTER user_id,
  ADD COLUMN team_code VARCHAR(64)  NULL COMMENT '班组编码(快照)' AFTER team_id,
  ADD COLUMN team_name VARCHAR(128) NULL COMMENT '班组名称(快照)' AFTER team_code;

-- 2. 上下工：班组快照（已有 user_id）
ALTER TABLE qxx_pro_workrecord
  ADD COLUMN team_id   BIGINT       NULL COMMENT '班组ID(快照)' AFTER task_code,
  ADD COLUMN team_code VARCHAR(64)  NULL COMMENT '班组编码(快照)' AFTER team_id,
  ADD COLUMN team_name VARCHAR(128) NULL COMMENT '班组名称(快照)' AFTER team_code;

-- 3. 历史回填：feedback.user_id 按 user_name 解析
UPDATE qxx_pro_feedback f
  JOIN sys_user u ON u.user_name = f.user_name AND u.del_flag = '0'
  SET f.user_id = u.user_id
  WHERE f.user_id IS NULL;

-- 4. 历史回填：feedback 班组（按 user_id + 同厂班组成员，取最早一条）
UPDATE qxx_pro_feedback f
  JOIN (
      SELECT m.user_id, m.factory_id, MIN(m.member_id) AS mid
      FROM qxx_cal_team_member m GROUP BY m.user_id, m.factory_id
  ) pick ON pick.user_id = f.user_id AND pick.factory_id = f.factory_id
  JOIN qxx_cal_team_member m ON m.member_id = pick.mid
  SET f.team_id = m.team_id, f.team_code = m.team_code, f.team_name = m.team_name
  WHERE f.team_id IS NULL;

-- 5. 历史回填：workrecord 班组
UPDATE qxx_pro_workrecord r
  JOIN (
      SELECT m.user_id, m.factory_id, MIN(m.member_id) AS mid
      FROM qxx_cal_team_member m GROUP BY m.user_id, m.factory_id
  ) pick ON pick.user_id = r.user_id AND pick.factory_id = r.factory_id
  JOIN qxx_cal_team_member m ON m.member_id = pick.mid
  SET r.team_id = m.team_id, r.team_code = m.team_code, r.team_name = m.team_name
  WHERE r.team_id IS NULL;

-- 6. 索引
CREATE INDEX idx_card_process_task_io ON qxx_pro_card_process(task_id, input_time, output_time);
CREATE INDEX idx_card_process_card_seq ON qxx_pro_card_process(card_id, seq_num);
CREATE INDEX idx_feedback_time_team     ON qxx_pro_feedback(feedback_time, team_id);
CREATE INDEX idx_workrecord_clock_team  ON qxx_pro_workrecord(clock_in_time, team_id);
CREATE INDEX idx_task_end_status        ON qxx_pro_task(end_time, status);

-- 7. 菜单：生产统计（挂在生产管理 parent_id=2003 下，排在甘特 2311/换型 2312 之后）
INSERT IGNORE INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
(2320, '生产统计', 2003, 8, 'report', 'mes/pro/report/index', 1, 0, 'C', '0', '0', 'mes:pro:report:query', 'chart', 'admin', sysdate());

-- 8. 系统参数
INSERT IGNORE INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark) VALUES
('进度临期阈值(小时)', 'mes.progress.warnHours', '24', 'Y', 'admin', sysdate(), '任务计划结束前N小时未完成判临期'),
('工单临期阈值(天)',   'mes.progress.warnDays',  '2',  'Y', 'admin', sysdate(), '工单交期前N天未完工判临期'),
('进度滞后容差(百分点)','mes.progress.behindTolerance','10','Y', 'admin', sysdate(), '产出进度比时间进度低超N个点判滞后'),
('班组快照开关',       'mes.progress.snapshotTeam', 'true', 'Y', 'admin', sysdate(), '报工/上下工是否快照班组');

-- 9. 授权 admin 角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id = 2320;
