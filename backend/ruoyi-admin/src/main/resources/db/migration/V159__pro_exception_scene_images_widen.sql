-- ============================================================
-- V159：生产异常现场照片字段加宽
-- 9 张 MinIO 完整 URL 逗号串，原始文件名较长时 varchar(1000) 可能不足导致提交失败
-- ============================================================
SET NAMES utf8mb4;

ALTER TABLE qxx_pro_exception
    MODIFY COLUMN scene_images varchar(2000) DEFAULT NULL COMMENT '现场照片(MinIO URL逗号串,最多9张)';
