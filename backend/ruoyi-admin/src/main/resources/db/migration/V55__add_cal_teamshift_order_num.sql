-- ============================================================
-- 补全 cal 模块旧表缺失列（幂等）
-- 背景：CalTeamshift 实体 + CalTeamshiftMapper 新增了 orderNum/order_num
--       字段，但从未写过迁移给表加列；V07__qxx_cal_core.sql 建表本身也无此列。
--       V07 低于 baseline(32)，生产表为早期别的方式建，故缺列。
--       影响：甘特图自动排产时，scheduleWorkOrder 调工作日历查 qxx_cal_teamshift，
--       selectCalTeamshiftList 引用 order_num -> BadSqlGrammarException:
--       Unknown column 'order_num' -> @Transactional 回滚已插入的 pro_task，
--       导致甘特图 children 恒为空。
-- 幂等：列存在则跳过
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. qxx_cal_teamshift.order_num
--    （实体 CalTeamshift + mapper 新增字段，从未写迁移）
-- ============================================================
DROP PROCEDURE IF EXISTS proc_add_teamshift_order_num;
DELIMITER $$
CREATE PROCEDURE proc_add_teamshift_order_num()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_cal_teamshift' AND COLUMN_NAME='order_num') THEN
        ALTER TABLE qxx_cal_teamshift ADD COLUMN order_num bigint(20) DEFAULT 0 COMMENT '排序号' AFTER plan_name;
    END IF;
END$$
DELIMITER ;
CALL proc_add_teamshift_order_num();
DROP PROCEDURE IF EXISTS proc_add_teamshift_order_num;
