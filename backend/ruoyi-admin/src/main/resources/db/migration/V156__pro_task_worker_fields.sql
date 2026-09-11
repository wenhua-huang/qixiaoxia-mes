-- V150__pro_task_worker_fields.sql
-- 排产任务增加派工报工人、负责人（均为可空快照；自动排产任务默认 NULL，报工页退化为登录人兜底）
ALTER TABLE qxx_pro_task
    ADD COLUMN worker_id   bigint        DEFAULT NULL COMMENT '派工报工人用户ID'   AFTER quantity_changed,
    ADD COLUMN worker_name varchar(64)   DEFAULT NULL COMMENT '派工报工人账号快照' AFTER worker_id,
    ADD COLUMN worker_nick varchar(64)   DEFAULT NULL COMMENT '派工报工人姓名快照' AFTER worker_name,
    ADD COLUMN leader_id   bigint        DEFAULT NULL COMMENT '任务负责人用户ID'   AFTER worker_nick,
    ADD COLUMN leader_name varchar(64)   DEFAULT NULL COMMENT '任务负责人账号快照' AFTER leader_id,
    ADD COLUMN leader_nick varchar(64)   DEFAULT NULL COMMENT '任务负责人姓名快照' AFTER leader_name;
