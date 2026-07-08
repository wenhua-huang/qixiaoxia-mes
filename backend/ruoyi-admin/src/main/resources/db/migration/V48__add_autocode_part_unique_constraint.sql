-- ============================================================
-- V48: 为 sys_auto_code_part 添加唯一约束，防止重复分段
-- 问题: sys_auto_code_part 缺少 (rule_id, part_index) 唯一约束，
--       INSERT IGNORE 在没有唯一约束时不生效，导致重复插入。
--       已出现 6 个规则各有 2 份分段副本 (V40 执行时产生)。
-- 修复: ① 清理残留重复(保留最小 part_id) ② 加唯一约束
-- ============================================================

-- 1. 删除残留重复分段（保留最小的 part_id）
DELETE p1 FROM sys_auto_code_part p1
INNER JOIN sys_auto_code_part p2
    ON p1.rule_id = p2.rule_id
    AND p1.part_index = p2.part_index
    AND p1.part_id > p2.part_id;

-- 2. 添加唯一约束，防止将来再产生重复
ALTER TABLE sys_auto_code_part
    ADD CONSTRAINT uk_rule_part UNIQUE (rule_id, part_index);
