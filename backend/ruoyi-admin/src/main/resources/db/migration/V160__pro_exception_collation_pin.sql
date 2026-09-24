-- ============================================================
-- V160: qxx_pro_exception 排序规则钉死 utf8mb4_unicode_ci
--   V157 建表只写 CHARSET 未写 COLLATE，继承了库级默认值；本地 MySQL8 默认
--   落到 utf8mb4_0900_ai_ci，与 qxx_pro_task/qxx_pro_feedback 等兄弟表
--   （utf8mb4_unicode_ci）不一致，后续跨表字符串 JOIN 会触发
--   "Illegal mix of collations"。本表为本月新建、数据量极小，直接 CONVERT。
-- 幂等：CONVERT TO 对已经是目标排序规则的库重复执行无副作用。
-- ============================================================

SET NAMES utf8mb4;

ALTER TABLE qxx_pro_exception
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
