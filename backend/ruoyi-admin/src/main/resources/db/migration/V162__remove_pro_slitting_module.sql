-- ============================================================
-- V162: 下线老【生产管理-分切作业】模块（qxx_pro_slitting_record 三步式外协流）
--   分切外协统一走【仓库管理-外协管理】通用框架（qxx_wm_outsource_order,
--   source_type='SLITTING' + SlittingResultStrategy）。本迁移：
--   1. 反冲所有仍处 ISSUED 的老外协分切测试单（库存加回、母卷恢复在库、
--      流水/单据删除、流转卡条件恢复），带严格护栏，任何一条不满足即整体失败；
--   2. 删除菜单 2313/23131-23134 及角色授权；
--   3. 删除 mes_pro_slitting_status / mes_pro_slitting_mode 字典；
--   4. 删除 SLITTING_CODE 自动编码规则（ROLL_CODE 保留，新框架建子卷在用）。
--   表 qxx_pro_slitting_record 保留不 DROP（承载 EXECUTED/RECEIVED 历史数据）。
--   幂等：反冲过程在无 ISSUED 单时为 0 行空操作；菜单/字典/规则删除可重复执行。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 反冲 ISSUED 老外协分切单
--    发料动作（ProSlittingServiceImpl.issueOneParent，已删除）的写库点：
--      a. qxx_wm_material_stock 按 SPLIT 负流水扣减 onhand/available
--      b. qxx_wm_roll_detail 母卷 IN_STOCK -> OUTSOURCED、remaining_quantity 清零
--      c. qxx_pro_card ACTIVE -> OUTSOURCING（仅当单据挂卡）
--      d. INSERT qxx_wm_transaction(SPLIT,-) 与 qxx_pro_slitting_record
--    安全护栏（任一 ISSUED 单不满足则 SIGNAL 回滚，禁止部分反冲）：
--      - 恰好存在 1 条对应负 SPLIT 流水，且其库存行仍存在；
--      - 母卷仍 OUTSOURCED、remaining=0、actual_weight 与扣减重量一致；
--      - 母卷无子卷（未发生"录结果"）。
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS proc_v162_reverse_issued_slitting;
DELIMITER $$
CREATE PROCEDURE proc_v162_reverse_issued_slitting()
BEGIN
    DECLARE v_total INT DEFAULT 0;
    DECLARE v_safe INT DEFAULT 0;
    DECLARE v_bad VARCHAR(1000) DEFAULT '';
    DECLARE v_msg VARCHAR(1000) DEFAULT '';

    CREATE TEMPORARY TABLE tmp_v162_safe (
        slit_id      bigint        NOT NULL,
        factory_id   bigint        NOT NULL,
        tx_id        bigint        NOT NULL,
        stock_id     bigint        NOT NULL,
        roll_id      bigint        NOT NULL,
        add_qty      decimal(20,6) NOT NULL,
        card_id      bigint             NULL,
        process_id   bigint             NULL,
        process_name varchar(100)       NULL,
        UNIQUE KEY uk_tx (tx_id),
        KEY idx_slit (slit_id)
    );

    INSERT INTO tmp_v162_safe (slit_id, factory_id, tx_id, stock_id, roll_id, add_qty,
                               card_id, process_id, process_name)
    SELECT s.slit_id, s.factory_id, t.transaction_id, t.material_stock_id,
           s.parent_roll_id, -t.quantity, s.card_id, s.process_id, s.process_name
    FROM qxx_pro_slitting_record s
    JOIN qxx_wm_transaction t
      ON t.source_doc_type = 'SLITTING'
     AND t.source_doc_id = s.slit_id
     AND t.source_line_id = 0
     AND t.transaction_type = 'SPLIT'
     AND t.quantity < 0
     AND t.factory_id = s.factory_id
    JOIN qxx_wm_material_stock ms
      ON ms.material_stock_id = t.material_stock_id
     AND ms.factory_id = t.factory_id
    JOIN qxx_wm_roll_detail r
      ON r.roll_id = s.parent_roll_id
     AND r.factory_id = s.factory_id
     AND r.status = 'OUTSOURCED'
     AND r.remaining_quantity = 0
     AND r.actual_weight = -t.quantity
    WHERE s.status = 'ISSUED'
      AND s.slit_mode = 'OUTSOURCE'
      AND NOT EXISTS (SELECT 1 FROM qxx_wm_roll_detail c
                       WHERE c.parent_roll_id = s.parent_roll_id
                         AND c.factory_id = s.factory_id
                         AND c.roll_id <> s.parent_roll_id)
    GROUP BY s.slit_id, s.factory_id, t.transaction_id, t.material_stock_id,
             s.parent_roll_id, t.quantity, s.card_id, s.process_id, s.process_name
    HAVING COUNT(*) = 1;

    SELECT COUNT(*) INTO v_total
      FROM qxx_pro_slitting_record
     WHERE status = 'ISSUED' AND slit_mode = 'OUTSOURCE';
    SELECT COUNT(*) INTO v_safe FROM tmp_v162_safe;

    IF v_safe <> v_total THEN
        SELECT GROUP_CONCAT(slit_id) INTO v_bad
          FROM qxx_pro_slitting_record
         WHERE status = 'ISSUED' AND slit_mode = 'OUTSOURCE'
           AND slit_id NOT IN (SELECT slit_id FROM tmp_v162_safe);
        SET v_msg = CONCAT('V162: 存在不满足反冲护栏的 ISSUED 外协分切单，slit_id=', IFNULL(v_bad, ''));
        DROP TEMPORARY TABLE tmp_v162_safe;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    START TRANSACTION;

    -- a. 库存加回（多张单反冲到同一库存行时必须先按行汇总，
    --    多表 UPDATE 同一目标行多次匹配会“最后一次写入覆盖”而非累加）
    UPDATE qxx_wm_material_stock ms
    JOIN (SELECT stock_id, factory_id, SUM(add_qty) AS total_qty
            FROM tmp_v162_safe
           GROUP BY stock_id, factory_id) z
      ON z.stock_id = ms.material_stock_id AND z.factory_id = ms.factory_id
       SET ms.quantity_onhand = ms.quantity_onhand + z.total_qty,
           ms.quantity_available = LEAST(ms.quantity_available + z.total_qty,
                                         ms.quantity_onhand + z.total_qty),
           ms.update_time = NOW();

    -- b. 母卷恢复在库（slit_batch_no 保留为原外协收货单号，不动）
    UPDATE qxx_wm_roll_detail r
    JOIN tmp_v162_safe z ON z.roll_id = r.roll_id AND z.factory_id = r.factory_id
       SET r.status = 'IN_STOCK',
           r.remaining_quantity = r.actual_weight,
           r.update_by = 'system',
           r.update_time = NOW()
     WHERE r.status = 'OUTSOURCED';

    -- c. 流转卡恢复 ACTIVE：仅当卡仍 OUTSOURCING 且无在途通用外协单占用
    UPDATE qxx_pro_card c
    JOIN tmp_v162_safe z ON z.card_id = c.card_id AND z.card_id IS NOT NULL
       SET c.status = 'ACTIVE',
           c.current_process_id = z.process_id,
           c.current_process_name = z.process_name,
           c.update_by = 'system',
           c.update_time = NOW()
     WHERE c.status = 'OUTSOURCING'
       AND NOT EXISTS (SELECT 1 FROM qxx_wm_outsource_order o
                        WHERE o.card_id = z.card_id
                          AND o.factory_id = z.factory_id
                          AND o.status IN ('ISSUED','VENDOR_RCVD','PROCESSING','FINISHED','SHIPPED'));

    -- d. 删除 SPLIT 发料流水与分切单
    DELETE FROM qxx_wm_transaction
     WHERE transaction_id IN (SELECT tx_id FROM tmp_v162_safe);
    DELETE FROM qxx_pro_slitting_record
     WHERE slit_id IN (SELECT slit_id FROM tmp_v162_safe);

    COMMIT;
    DROP TEMPORARY TABLE tmp_v162_safe;
END$$
DELIMITER ;

CALL proc_v162_reverse_issued_slitting();
DROP PROCEDURE IF EXISTS proc_v162_reverse_issued_slitting;

-- ------------------------------------------------------------
-- 2. 删除分切作业菜单及角色授权（2313 来自 V93，23134 来自 V98）
--    注意：2310x 是"工序管理"菜单，绝不可动；role 106/vendor 账号被
--    通用外协框架（28800 系列）继续使用，仅回收其 2313x 授权行。
-- ------------------------------------------------------------
DELETE FROM sys_role_menu WHERE menu_id IN (2313, 23131, 23132, 23133, 23134);
DELETE FROM sys_menu      WHERE menu_id IN (2313, 23131, 23132, 23133, 23134);

-- ------------------------------------------------------------
-- 3. 删除分切专用字典（追溯字典 SLIT/ROLL、纸卷状态字典保留）
-- ------------------------------------------------------------
DELETE FROM sys_dict_data WHERE dict_type IN ('mes_pro_slitting_status', 'mes_pro_slitting_mode');
DELETE FROM sys_dict_type WHERE dict_type IN ('mes_pro_slitting_status', 'mes_pro_slitting_mode');

-- ------------------------------------------------------------
-- 4. 删除 SLITTING_CODE 编码规则（先计数器/部件，后规则；ROLL_CODE 保留）
-- ------------------------------------------------------------
DELETE FROM sys_auto_code_result
 WHERE rule_id IN (SELECT rule_id FROM (
        SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'SLITTING_CODE') t);
DELETE FROM sys_auto_code_part
 WHERE rule_id IN (SELECT rule_id FROM (
        SELECT rule_id FROM sys_auto_code_rule WHERE rule_code = 'SLITTING_CODE') t);
DELETE FROM sys_auto_code_rule WHERE rule_code = 'SLITTING_CODE';
