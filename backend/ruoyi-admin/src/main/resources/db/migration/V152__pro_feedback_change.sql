-- V152__pro_feedback_change.sql
-- 报工上机数量修改痕迹（系统默认带出 vs 人工修改）；factory_id 由 FactoryIdInterceptor 注入，建表仍按多工厂规范保留列
CREATE TABLE IF NOT EXISTS qxx_pro_feedback_change (
    change_id      bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    factory_id     bigint       NOT NULL COMMENT '工厂ID',
    feedback_id    bigint       NOT NULL COMMENT '报工记录ID',
    task_id        bigint       DEFAULT NULL COMMENT '任务ID',
    workorder_id   bigint       DEFAULT NULL COMMENT '工单ID',
    field_name     varchar(64)  NOT NULL DEFAULT 'quantity_input' COMMENT '变更字段',
    old_value      varchar(128) DEFAULT NULL COMMENT '原值(系统默认值)',
    new_value      varchar(128) NOT NULL COMMENT '新值(人工填写值)',
    change_source  varchar(16)  NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL-人工修改/SYSTEM-系统填充',
    change_reason  varchar(255) DEFAULT NULL COMMENT '变更说明',
    create_by      varchar(64)  DEFAULT NULL COMMENT '创建者',
    create_time    datetime     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (change_id),
    KEY idx_fb (feedback_id),
    KEY idx_task (task_id),
    KEY idx_wo (workorder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报工字段变更痕迹';
