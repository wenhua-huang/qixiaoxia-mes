-- V153__qc_block_release.sql
-- 跟单检验不合格硬拦：放行留痕表 + 门控高频反查索引
-- 一张 FAIL 的 IPQC 对每个被拦任务至多一条放行记录（uk_ipqc_target 兜底重复放行）
CREATE TABLE IF NOT EXISTS qxx_qc_block_release (
    release_id          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    factory_id          bigint       NOT NULL COMMENT '工厂ID',
    ipqc_id             bigint       NOT NULL COMMENT '被放行的IPQC检验单ID',
    ipqc_code           varchar(64)  DEFAULT NULL COMMENT '检验单号',
    workorder_id        bigint       DEFAULT NULL COMMENT '工单ID',
    workorder_code      varchar(64)  DEFAULT NULL COMMENT '工单编码',
    card_id             bigint       DEFAULT NULL COMMENT '流转卡ID',
    card_code           varchar(64)  DEFAULT NULL COMMENT '流转卡编码',
    check_process_id    bigint       DEFAULT NULL COMMENT '检验工序ID',
    check_process_name  varchar(128) DEFAULT NULL COMMENT '检验工序名',
    target_task_id      bigint       NOT NULL COMMENT '被拦截(放行)任务ID',
    target_process_id   bigint       DEFAULT NULL COMMENT '被拦截工序ID',
    target_process_name varchar(128) DEFAULT NULL COMMENT '被拦截工序名',
    release_reason      varchar(500) NOT NULL COMMENT '放行理由',
    approver_id         bigint       DEFAULT NULL COMMENT '放行人用户ID',
    approver_name       varchar(64)  DEFAULT NULL COMMENT '放行人姓名(账号)快照',
    approve_time        datetime     DEFAULT NULL COMMENT '放行时间',
    create_by           varchar(64)  DEFAULT NULL COMMENT '创建者',
    create_time         datetime     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (release_id),
    UNIQUE KEY uk_ipqc_target (ipqc_id, target_task_id),
    KEY idx_wo (workorder_id),
    KEY idx_task (target_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质检不合格拦截-放行记录';

-- 门控高频反查：按工单+检验工序取最新判定单
CREATE INDEX idx_ipqc_wo_process ON qxx_qc_ipqc(workorder_id, process_id, status);
