-- V151__pro_feedback_quantity_input.sql
-- 报工增加「本次上机数量(投入数量)」：可空；不传时由后端按路线流解析默认值（首道=任务排产数，其余=上工序已审产出-本工序累计上机）
ALTER TABLE qxx_pro_feedback
    ADD COLUMN quantity_input decimal(14,2) DEFAULT NULL COMMENT '本次上机数量(投入数量)' AFTER quantity;
