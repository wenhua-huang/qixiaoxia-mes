-- 为 qxx_md_workstation 表新增 process_id 列（关联 qxx_pro_process 工序表）
-- 修复：Mapper XML 已通过 LEFT JOIN 查询 process_name，但数据库缺少对应的列

ALTER TABLE `qxx_md_workstation`
ADD COLUMN `process_id` bigint(20) DEFAULT NULL COMMENT '工序ID(关联qxx_pro_process)',
ADD INDEX `idx_process_id` (`process_id`);
