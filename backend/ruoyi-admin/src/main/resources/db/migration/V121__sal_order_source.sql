-- ============================================================
-- V121: 销售订单加「订单来源」字段 + 外部系统 API Key 凭证表
--
-- 背景：销售订单需记录来源（1=直接新增 / 2=CRM系统），并开放 CRM 推单接口。
--      外部接口用 API Key（Header X-API-Key）认证，每个 Key 绑定一个工厂(factory_id)，
--      推单时后端按 Key 解析工厂归属，注入登录上下文，使 FactoryIdInterceptor 正常隔离数据。
--
-- 含：① qxx_sal_order 加 source 列（幂等）
--    ② qxx_sys_api_key 外部凭证表（CREATE IF NOT EXISTS）
--
-- 幂等：列存在跳过（存储过程）/ CREATE IF NOT EXISTS
-- 字符集：utf8mb4
-- 日期：2026-08-05
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. qxx_sal_order 加 source 列（订单来源）
--    1=直接新增(系统内手工建), 2=CRM系统(外部推单)
-- ============================================================
DROP PROCEDURE IF EXISTS proc_add_sal_order_source;
DELIMITER $$
CREATE PROCEDURE proc_add_sal_order_source()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='qxx_sal_order' AND COLUMN_NAME='source') THEN
        ALTER TABLE qxx_sal_order
          ADD COLUMN source tinyint(1) NOT NULL DEFAULT 1 COMMENT '订单来源:1-直接新增,2-CRM系统' AFTER sample_flag;
    END IF;
END$$
DELIMITER ;
CALL proc_add_sal_order_source();
DROP PROCEDURE IF EXISTS proc_add_sal_order_source;

-- ============================================================
-- 2. 外部系统 API Key 凭证表 qxx_sys_api_key
--    用途：CRM 等外部系统调用 /open-api/** 的接入凭证。
--    api_key 明文只在生成时返回一次给调用方；校验时按 api_key_hash 反查。
--    每个 Key 绑定一个 factory_id，推单时的工厂归属由此决定。
-- ============================================================
CREATE TABLE IF NOT EXISTS qxx_sys_api_key (
  id           bigint(20)    NOT NULL AUTO_INCREMENT  COMMENT '主键',
  api_key      varchar(128)  NOT NULL                 COMMENT 'API Key(生成时返回,不可查询)',
  api_key_hash varchar(128)  NOT NULL                 COMMENT 'API Key 的 SHA-256 hex(校验匹配用)',
  name         varchar(100)  NOT NULL                 COMMENT '凭证名称/用途说明',
  factory_id   bigint(20)    NOT NULL                 COMMENT '绑定的工厂ID(推单工厂归属)',
  enabled      char(1)       DEFAULT 'Y'              COMMENT '是否启用:Y-是,N-否',
  expires_at   datetime      DEFAULT NULL             COMMENT '过期时间(NULL=永不过期)',
  remark       varchar(255)  DEFAULT ''               COMMENT '备注',
  create_by    varchar(64)   DEFAULT ''               COMMENT '创建者',
  create_time  datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by    varchar(64)   DEFAULT ''               COMMENT '更新者',
  update_time  datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_api_key_hash (api_key_hash),
  KEY idx_factory_id (factory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部系统 API Key 凭证';
